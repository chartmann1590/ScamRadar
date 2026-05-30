package com.charles.scamradar.app.speech

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.FileOutputStream
import kotlin.coroutines.resume

class SpeechProcessor {

    fun isOnDeviceSpeechAvailable(context: Context): Boolean {
        return SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }

    fun isFileImportSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    suspend fun transcribeAudioFile(audioUri: Uri, context: Context): TranscriptionResult {
        // On-device transcription via Sherpa-ONNX + Whisper Tiny (16 KB
        // page-aligned native libs, accurate on TTS + real voicemails,
        // fully offline, no Google Speech Services dependency).
        return runCatching { SherpaTranscriber(context).transcribe(audioUri) }
            .getOrElse {
                Log.e("ScamSpeech", "sherpa transcribe failed", it)
                TranscriptionResult.Error("Could not transcribe: ${it.message ?: "unknown error"}")
            }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun transcribeFileOnDevice(
        audioUri: Uri,
        context: Context
    ): TranscriptionResult = suspendCancellableCoroutine { cont ->
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]

        val decoderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Step 1: try the native WAV reader. Android's MediaExtractor doesn't
        // reliably handle raw PCM WAV (the format most voicemail apps export),
        // so we read RIFF/WAVE ourselves and stream 16-bit PCM directly.
        val wavInfo = peekWavInfo(audioUri, context)
        Log.d(TAG, "wavInfo=$wavInfo uri=$audioUri")

        val pcmFormat = try {
            if (wavInfo != null) {
                PcmFormat(wavInfo.sampleRate, wavInfo.channelCount)
            } else {
                probeFormat(audioUri, context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "probe failed", e)
            readSide.close()
            writeSide.close()
            cont.resume(TranscriptionResult.Error("Could not read audio file: ${e.message ?: "unknown"}"))
            return@suspendCancellableCoroutine
        }
        Log.d(TAG, "pcmFormat=$pcmFormat")

        // Use the regular recognizer (Google Speech Services on Pixel). The
        // on-device-only path silently returns empty matches when no offline
        // model is downloaded for the user's locale — which is the default on
        // most Pixels until they explicitly install it via Settings.
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readSide)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, pcmFormat.sampleRate)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, pcmFormat.channelCount)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.w(TAG, "recognizer.onError code=$error (${errorName(error)})")
                runCatching { recognizer.destroy() }
                runCatching { readSide.close() }
                runCatching { writeSide.close() }
                if (!cont.isCompleted) {
                    cont.resume(
                        TranscriptionResult.Error(
                            "Speech recognition failed (${errorName(error)}). The file may be silent, too quiet, or in a language the on-device model doesn't support."
                        )
                    )
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                Log.d(TAG, "recognizer.onResults textLen=${text.length} matches=${matches?.size}")
                runCatching { recognizer.destroy() }
                runCatching { readSide.close() }
                if (!cont.isCompleted) {
                    cont.resume(TranscriptionResult.Success(text))
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        decoderScope.launch {
            FileOutputStream(writeSide.fileDescriptor).use { rawOut ->
                // Throttle to ~realtime playback rate. Google Speech Services
                // expects audio arriving roughly as fast as it's spoken; if you
                // bulk-dump the whole file in 200ms the VAD discards it as
                // noise and returns no matches.
                val throttled = ThrottledPcmStream(
                    out = rawOut,
                    sampleRate = pcmFormat.sampleRate,
                    channelCount = pcmFormat.channelCount,
                    bytesPerSample = 2,
                )
                val bytes = runCatching {
                    if (wavInfo != null) {
                        streamWavPcm(audioUri, context, wavInfo, throttled)
                    } else {
                        decodePcmTo(audioUri, context, throttled)
                    }
                }
                Log.d(TAG, "pcm streaming done bytes=${bytes.getOrNull()} err=${bytes.exceptionOrNull()?.message}")
            }
            runCatching { writeSide.close() }
        }

        recognizer.startListening(intent)
        cont.invokeOnCancellation {
            runCatching { recognizer.cancel() }
            runCatching { recognizer.destroy() }
            runCatching { readSide.close() }
            runCatching { writeSide.close() }
        }
    }

    private data class PcmFormat(val sampleRate: Int, val channelCount: Int)

    /** Returns WAV format info if the file is a parseable RIFF/WAVE, else null. */
    private fun peekWavInfo(uri: Uri, context: Context): WavReader.WavInfo? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val peek = ByteArray(12)
                if (input.read(peek, 0, 12) < 12) return@runCatching null
                if (!WavReader.looksLikeWav(peek)) return@runCatching null
                // Re-open and parse from the start
                context.contentResolver.openInputStream(uri)?.use { fresh ->
                    WavReader.parseHeader(fresh)
                }
            }
        }.getOrNull()
    }

    /** Streams 16-bit LE PCM from the WAV file into [out]. */
    private fun streamWavPcm(
        uri: Uri,
        context: Context,
        info: WavReader.WavInfo,
        out: java.io.OutputStream,
    ): Long {
        var total = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            // Re-parse the header to position the stream at the data chunk
            WavReader.parseHeader(input) ?: return 0L
            total = WavReader.streamAs16BitLePcm(input, info, out)
        }
        return total
    }

    private fun probeFormat(uri: Uri, context: Context): PcmFormat {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    val rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    return PcmFormat(rate, channels)
                }
            }
            throw IllegalArgumentException("No audio track found")
        } finally {
            extractor.release()
        }
    }

    private fun decodePcmTo(uri: Uri, context: Context, out: java.io.OutputStream): Long {
        var total = 0L
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if (fmt.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                    trackIndex = i
                    format = fmt
                    break
                }
            }
            if (trackIndex < 0 || format == null) return 0L
            extractor.selectTrack(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME)!!
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false
            val timeoutUs = 10_000L

            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val inIdx = codec.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
                when {
                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)!!
                        if (info.size > 0) {
                            val bytes = ByteArray(info.size)
                            outBuf.position(info.offset)
                            outBuf.get(bytes, 0, info.size)
                            out.write(bytes)
                            total += info.size
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawOutputEos = true
                        }
                    }
                    outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // keep looping
                    }
                }
            }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
        return total
    }

    private fun errorName(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "ERROR_TOO_MANY_REQUESTS"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "ERROR_SERVER_DISCONNECTED"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "ERROR_LANGUAGE_NOT_SUPPORTED"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "ERROR_LANGUAGE_UNAVAILABLE"
        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "ERROR_CANNOT_CHECK_SUPPORT"
        else -> "ERROR_$code"
    }

    companion object {
        private const val TAG = "ScamSpeech"
    }
}

sealed interface TranscriptionResult {
    data class Success(val text: String) : TranscriptionResult
    data class Error(val message: String) : TranscriptionResult
    data class Unsupported(val message: String) : TranscriptionResult
}
