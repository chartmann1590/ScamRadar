package com.scamradar.app.speech

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
        if (!isFileImportSupported()) {
            return TranscriptionResult.Unsupported(
                "Audio file import requires Android 13 or newer. Please play the voicemail near your phone instead."
            )
        }
        return transcribeFileOnDevice(audioUri, context)
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
        val pcmFormat = try {
            probeFormat(audioUri, context)
        } catch (e: Exception) {
            readSide.close()
            writeSide.close()
            cont.resume(TranscriptionResult.Error("Could not read audio file: ${e.message ?: "unknown"}"))
            return@suspendCancellableCoroutine
        }

        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
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
                runCatching { recognizer.destroy() }
                runCatching { readSide.close() }
                runCatching { writeSide.close() }
                if (!cont.isCompleted) {
                    cont.resume(
                        TranscriptionResult.Error("Speech recognition failed (code $error). The file may be silent, too long, or in a language the on-device model doesn't support.")
                    )
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
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
            FileOutputStream(writeSide.fileDescriptor).use { out ->
                runCatching { decodePcmTo(audioUri, context, out) }
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

    private fun decodePcmTo(uri: Uri, context: Context, out: FileOutputStream) {
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
            if (trackIndex < 0 || format == null) return
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
    }
}

sealed interface TranscriptionResult {
    data class Success(val text: String) : TranscriptionResult
    data class Error(val message: String) : TranscriptionResult
    data class Unsupported(val message: String) : TranscriptionResult
}
