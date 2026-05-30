package com.charles.scamradar.app.speech

import android.content.Context
import android.net.Uri
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device automatic speech recognition via Sherpa-ONNX + Whisper Tiny EN.
 *
 * Why Sherpa-ONNX (vs Vosk / Android SpeechRecognizer):
 *  - Native libs ship 16 KB page-aligned (Vosk's still 4 KB, blocks Play
 *    Store uploads from Nov 1, 2025)
 *  - Whisper accuracy on synthetic TTS audio is dramatically better than the
 *    Kaldi acoustic models Vosk uses
 *  - Fully offline; no Google Speech Services dependency
 *
 * Model files live in `assets/sherpa-whisper-tiny-en/`. Sherpa-ONNX accepts
 * AssetManager + relative paths directly, so we don't need to extract to
 * filesDir.
 *
 * Audio pipeline:
 *  - WAV files: parsed by [WavReader] (any sample rate / bit depth)
 *  - Everything else: decoded by Android MediaCodec
 *  - Downmix to mono, linear-resample to 16 kHz, convert to FloatArray [-1,1]
 *  - Fed to Sherpa's OfflineRecognizer as a single waveform
 */
class SherpaTranscriber(private val context: Context) {

    suspend fun transcribe(uri: Uri): TranscriptionResult = withContext(Dispatchers.IO) {
        val pcm = decodeToPcm(uri)
            ?: return@withContext TranscriptionResult.Error("Could not read audio file.")
        if (pcm.bytes.isEmpty()) {
            return@withContext TranscriptionResult.Error("Audio file appears to be empty.")
        }
        Log.d(TAG, "decoded bytes=${pcm.bytes.size} sampleRate=${pcm.sampleRate} channels=${pcm.channelCount}")

        // 1) Downmix to mono
        val mono = if (pcm.channelCount == 1) pcm.bytes
            else downmixToMono(pcm.bytes, pcm.channelCount)

        // 2) Resample to 16 kHz (Whisper requirement)
        val resampledBytes = if (pcm.sampleRate == 16000) mono
            else linearResample(mono, pcm.sampleRate, 16000)

        // 3) Convert int16 PCM to float [-1, 1]
        val floats = pcmInt16ToFloat(resampledBytes)
        Log.d(TAG, "float samples=${floats.size} (~${floats.size / 16000.0}s)")

        val recognizer = buildRecognizer()
        try {
            val stream = recognizer.createStream()
            try {
                stream.acceptWaveform(floats, sampleRate = 16000)
                recognizer.decode(stream)
                val result = recognizer.getResult(stream)
                val text = result.text.trim()
                Log.d(TAG, "result text='$text' tokens=${result.tokens.size}")
                TranscriptionResult.Success(text)
            } finally {
                stream.release()
            }
        } finally {
            recognizer.release()
        }
    }

    private fun buildRecognizer(): OfflineRecognizer {
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = "$MODEL_DIR/tiny.en-encoder.int8.onnx",
                    decoder = "$MODEL_DIR/tiny.en-decoder.int8.onnx",
                    language = "en",
                    task = "transcribe",
                ),
                tokens = "$MODEL_DIR/tiny.en-tokens.txt",
                numThreads = 2,
            ),
        )
        return OfflineRecognizer(assetManager = context.assets, config = config)
    }

    private data class PcmAudio(val bytes: ByteArray, val sampleRate: Int, val channelCount: Int)

    private fun decodeToPcm(uri: Uri): PcmAudio? {
        // Try the native WAV reader first
        val wavInfo = peekWavInfo(uri)
        if (wavInfo != null) {
            Log.d(TAG, "wav path: sampleRate=${wavInfo.sampleRate} channels=${wavInfo.channelCount}")
            return readWavToPcm(uri, wavInfo)
        }
        Log.d(TAG, "media-codec decode path")
        return decodeWithMediaCodec(uri)
    }

    private fun peekWavInfo(uri: Uri): WavReader.WavInfo? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val peek = ByteArray(12)
                if (input.read(peek, 0, 12) < 12) return@runCatching null
                if (!WavReader.looksLikeWav(peek)) return@runCatching null
                context.contentResolver.openInputStream(uri)?.use { fresh ->
                    WavReader.parseHeader(fresh)
                }
            }
        }.getOrNull()
    }

    private fun readWavToPcm(uri: Uri, info: WavReader.WavInfo): PcmAudio? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                WavReader.parseHeader(input) ?: return@runCatching null
                val buf = java.io.ByteArrayOutputStream()
                WavReader.streamAs16BitLePcm(input, info, buf)
                PcmAudio(buf.toByteArray(), info.sampleRate, info.channelCount)
            }
        }.getOrNull()
    }

    private fun decodeWithMediaCodec(uri: Uri): PcmAudio? {
        val extractor = android.media.MediaExtractor()
        var codec: android.media.MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var trackIndex = -1
            var format: android.media.MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if (fmt.getString(android.media.MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                    trackIndex = i
                    format = fmt
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null
            extractor.selectTrack(trackIndex)

            val sampleRate = format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME)!!
            codec = android.media.MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = android.media.MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false
            val timeoutUs = 10_000L
            val out = java.io.ByteArrayOutputStream()

            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val inIdx = codec.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)!!
                    if (info.size > 0) {
                        val bytes = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(bytes, 0, info.size)
                        out.write(bytes)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEos = true
                    }
                }
            }
            return PcmAudio(out.toByteArray(), sampleRate, channels)
        } catch (e: Exception) {
            Log.e(TAG, "media-codec decode failed", e)
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun downmixToMono(pcm: ByteArray, channels: Int): ByteArray {
        val frameBytes = 2 * channels
        val frames = pcm.size / frameBytes
        val out = ByteArray(frames * 2)
        var oi = 0
        var i = 0
        for (f in 0 until frames) {
            var sum = 0
            for (c in 0 until channels) {
                val lo = pcm[i].toInt() and 0xFF
                val hi = pcm[i + 1].toInt() // signed
                sum += (hi shl 8) or lo
                i += 2
            }
            val avg = (sum / channels).coerceIn(-32768, 32767)
            out[oi] = (avg and 0xFF).toByte()
            out[oi + 1] = ((avg shr 8) and 0xFF).toByte()
            oi += 2
        }
        return out
    }

    private fun linearResample(pcm: ByteArray, fromRate: Int, toRate: Int): ByteArray {
        if (fromRate == toRate) return pcm
        val srcSamples = pcm.size / 2
        val ratio = toRate.toDouble() / fromRate.toDouble()
        val dstSamples = (srcSamples * ratio).toInt()
        val out = ByteArray(dstSamples * 2)
        for (i in 0 until dstSamples) {
            val srcPos = i / ratio
            val s0 = srcPos.toInt()
            val s1 = (s0 + 1).coerceAtMost(srcSamples - 1)
            val frac = srcPos - s0
            val v0 = sample16(pcm, s0)
            val v1 = sample16(pcm, s1)
            val v = (v0 + (v1 - v0) * frac).toInt().coerceIn(-32768, 32767)
            out[i * 2] = (v and 0xFF).toByte()
            out[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun sample16(pcm: ByteArray, index: Int): Int {
        val i = index * 2
        val lo = pcm[i].toInt() and 0xFF
        val hi = pcm[i + 1].toInt()
        return (hi shl 8) or lo
    }

    private fun pcmInt16ToFloat(pcm: ByteArray): FloatArray {
        val samples = pcm.size / 2
        val out = FloatArray(samples)
        for (i in 0 until samples) {
            val lo = pcm[i * 2].toInt() and 0xFF
            val hi = pcm[i * 2 + 1].toInt()
            val s = (hi shl 8) or lo
            out[i] = s / 32768.0f
        }
        return out
    }

    companion object {
        private const val MODEL_DIR = "sherpa-whisper-tiny-en"
        private const val TAG = "ScamSherpa"
    }
}
