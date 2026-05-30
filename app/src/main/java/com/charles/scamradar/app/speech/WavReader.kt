package com.charles.scamradar.app.speech

import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal RIFF/WAVE parser. Android's MediaExtractor does not reliably handle
 * raw PCM WAV files (most voicemail exports), so we read them directly and
 * stream 16-bit little-endian PCM to the on-device speech recognizer.
 *
 * Supports:
 *  - PCM (format code 1) at 8 / 16 / 24 / 32 bits per sample
 *  - IEEE float (format code 3) at 32 / 64 bits — downscaled to 16-bit
 *  - Any sample rate and channel count
 *  - WAVE_FORMAT_EXTENSIBLE (format code 0xFFFE) with PCM subtype
 *
 * Anything else (G.711, ADPCM, A-law, μ-law, etc.) returns null and the caller
 * falls back to MediaExtractor + MediaCodec.
 */
internal object WavReader {

    data class WavInfo(
        val sampleRate: Int,
        val channelCount: Int,
        val bitsPerSample: Int,
        val isFloat: Boolean,
    )

    /**
     * Returns true if the first 12 bytes look like a RIFF/WAVE file.
     * Caller is expected to pass a buffered stream so this read can be undone
     * by .reset() if false.
     */
    fun looksLikeWav(header: ByteArray): Boolean {
        if (header.size < 12) return false
        return header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
            header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
            header[8] == 'W'.code.toByte() && header[9] == 'A'.code.toByte() &&
            header[10] == 'V'.code.toByte() && header[11] == 'E'.code.toByte()
    }

    /**
     * Parses chunks until it finds the fmt + data chunks. After this call the
     * stream is positioned at the first PCM sample byte. Returns null if
     * unsupported encoding (e.g. ADPCM, A-law).
     */
    fun parseHeader(input: InputStream): WavInfo? {
        val riff = readFully(input, 12)
        if (!looksLikeWav(riff)) return null

        var info: WavInfo? = null
        while (true) {
            val header = readFully(input, 8, allowEarlyEof = true) ?: return null
            val id = String(header, 0, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            when (id) {
                "fmt " -> {
                    val fmt = readFully(input, size)
                    info = parseFmt(fmt) ?: return null
                    // fmt size may be odd, but PCM is always even
                }
                "data" -> {
                    return info // stream now sitting on PCM bytes
                }
                else -> {
                    // Skip unknown chunks (LIST, fact, bext, etc.)
                    skip(input, size.toLong())
                    // RIFF chunks are word-aligned: skip a pad byte if size is odd
                    if (size and 1 == 1) skip(input, 1)
                }
            }
        }
    }

    private fun parseFmt(fmt: ByteArray): WavInfo? {
        if (fmt.size < 16) return null
        val bb = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
        var format = bb.short.toInt() and 0xFFFF
        val channels = bb.short.toInt() and 0xFFFF
        val sampleRate = bb.int
        bb.int // byte rate
        bb.short // block align
        val bitsPerSample = bb.short.toInt() and 0xFFFF

        if (format == 0xFFFE && fmt.size >= 40) {
            // WAVE_FORMAT_EXTENSIBLE: real format is in the GUID at offset 24
            bb.short // cbSize
            bb.short // valid bits per sample
            bb.int // channel mask
            val guidFormat = bb.short.toInt() and 0xFFFF
            format = guidFormat
        }

        if (channels <= 0 || sampleRate <= 0 || bitsPerSample <= 0) return null

        return when (format) {
            1 -> WavInfo(sampleRate, channels, bitsPerSample, isFloat = false)
            3 -> WavInfo(sampleRate, channels, bitsPerSample, isFloat = true)
            else -> null
        }
    }

    /**
     * Streams PCM samples from [input], converting to 16-bit little-endian PCM
     * and writing to [out]. Stops at EOF. Returns total bytes written.
     */
    fun streamAs16BitLePcm(
        input: InputStream,
        info: WavInfo,
        out: java.io.OutputStream,
    ): Long {
        val bps = info.bitsPerSample
        val written: Long
        when {
            info.isFloat && (bps == 32 || bps == 64) -> written = streamFloat(input, bps, out)
            !info.isFloat && bps == 8 -> written = stream8(input, out)
            !info.isFloat && bps == 16 -> written = copyDirect(input, out)
            !info.isFloat && bps == 24 -> written = stream24(input, out)
            !info.isFloat && bps == 32 -> written = stream32(input, out)
            else -> throw IllegalStateException("unsupported PCM bits=$bps float=${info.isFloat}")
        }
        return written
    }

    private fun copyDirect(input: InputStream, out: java.io.OutputStream): Long {
        val buf = ByteArray(8192)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            out.write(buf, 0, n)
            total += n
        }
        return total
    }

    private fun stream8(input: InputStream, out: java.io.OutputStream): Long {
        // 8-bit PCM is unsigned (0..255), center at 128. Convert to signed 16-bit.
        val inBuf = ByteArray(4096)
        val outBuf = ByteArray(8192)
        var total = 0L
        while (true) {
            val n = input.read(inBuf)
            if (n <= 0) break
            var oi = 0
            for (i in 0 until n) {
                val s = ((inBuf[i].toInt() and 0xFF) - 128) shl 8
                outBuf[oi] = (s and 0xFF).toByte()
                outBuf[oi + 1] = ((s shr 8) and 0xFF).toByte()
                oi += 2
            }
            out.write(outBuf, 0, oi)
            total += oi
        }
        return total
    }

    private fun stream24(input: InputStream, out: java.io.OutputStream): Long {
        // 24-bit signed little-endian → take the upper 16 bits.
        val inBuf = ByteArray(4095) // multiple of 3
        val outBuf = ByteArray(2730)
        var total = 0L
        while (true) {
            val n = input.read(inBuf)
            if (n <= 0) break
            val frames = n / 3
            var oi = 0
            var ii = 0
            for (f in 0 until frames) {
                val mid = inBuf[ii + 1].toInt() and 0xFF
                val hi = inBuf[ii + 2].toInt() // signed
                outBuf[oi] = mid.toByte()
                outBuf[oi + 1] = hi.toByte()
                oi += 2
                ii += 3
            }
            out.write(outBuf, 0, oi)
            total += oi
        }
        return total
    }

    private fun stream32(input: InputStream, out: java.io.OutputStream): Long {
        // 32-bit signed little-endian → take the upper 16 bits.
        val inBuf = ByteArray(4096)
        val outBuf = ByteArray(2048)
        var total = 0L
        while (true) {
            val n = input.read(inBuf)
            if (n <= 0) break
            val frames = n / 4
            var oi = 0
            var ii = 0
            for (f in 0 until frames) {
                outBuf[oi] = inBuf[ii + 2]
                outBuf[oi + 1] = inBuf[ii + 3]
                oi += 2
                ii += 4
            }
            out.write(outBuf, 0, oi)
            total += oi
        }
        return total
    }

    private fun streamFloat(input: InputStream, bps: Int, out: java.io.OutputStream): Long {
        val sampleBytes = bps / 8
        val inBuf = ByteArray(4096 - 4096 % sampleBytes)
        val outBuf = ByteArray(inBuf.size / sampleBytes * 2)
        val bb = ByteBuffer.wrap(inBuf).order(ByteOrder.LITTLE_ENDIAN)
        var total = 0L
        while (true) {
            val n = input.read(inBuf)
            if (n <= 0) break
            val frames = n / sampleBytes
            bb.position(0)
            var oi = 0
            for (f in 0 until frames) {
                val v = if (bps == 32) bb.float.toDouble() else bb.double
                val clipped = v.coerceIn(-1.0, 1.0)
                val s = (clipped * 32767).toInt()
                outBuf[oi] = (s and 0xFF).toByte()
                outBuf[oi + 1] = ((s shr 8) and 0xFF).toByte()
                oi += 2
            }
            out.write(outBuf, 0, oi)
            total += oi
        }
        return total
    }

    private fun readFully(input: InputStream, size: Int, allowEarlyEof: Boolean = false): ByteArray? {
        val buf = ByteArray(size)
        var read = 0
        while (read < size) {
            val n = input.read(buf, read, size - read)
            if (n < 0) {
                if (allowEarlyEof && read == 0) return null
                throw java.io.EOFException("unexpected EOF reading $size bytes (got $read)")
            }
            read += n
        }
        return buf
    }

    private fun readFully(input: InputStream, size: Int): ByteArray =
        readFully(input, size, allowEarlyEof = false)!!

    private fun skip(input: InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                if (input.read() < 0) return
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
    }
}

/**
 * Buffered input with a peek-and-rewind for the first 12 bytes (RIFF header).
 */
internal class PeekableInputStream(private val src: InputStream) : InputStream() {
    private val buffered = BufferedInputStream(src, 64 * 1024).also { it.mark(64) }

    fun peek(n: Int): ByteArray {
        buffered.mark(n + 16)
        val buf = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = buffered.read(buf, read, n - read)
            if (r < 0) break
            read += r
        }
        buffered.reset()
        return if (read == n) buf else buf.copyOf(read)
    }

    override fun read(): Int = buffered.read()
    override fun read(b: ByteArray, off: Int, len: Int): Int = buffered.read(b, off, len)
    override fun close() = buffered.close()
}
