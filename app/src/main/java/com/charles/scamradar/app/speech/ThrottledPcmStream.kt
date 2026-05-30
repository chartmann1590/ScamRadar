package com.charles.scamradar.app.speech

import java.io.OutputStream

/**
 * Pacing wrapper around an OutputStream that emits PCM at roughly real-time
 * playback rate.
 *
 * Google's Speech Services rejects bulk-dumped audio: if a 30-second file
 * arrives in one write, the VAD treats the whole burst as noise and returns
 * zero matches. By gating writes to the rate the audio would naturally play
 * back (with a small lookahead buffer), the recognizer's incremental decoder
 * keeps up and produces a transcript.
 *
 * We don't need to be metronome-precise — a 1.2x speed-up is fine and keeps
 * UX snappy. The recognizer cares that audio doesn't arrive *too* fast.
 */
internal class ThrottledPcmStream(
    private val out: OutputStream,
    sampleRate: Int,
    channelCount: Int,
    bytesPerSample: Int,
    private val playbackSpeed: Double = 1.2,
) : OutputStream() {

    private val bytesPerSecond: Double =
        sampleRate.toDouble() * channelCount.toDouble() * bytesPerSample.toDouble()

    private val startNanos: Long = System.nanoTime()
    private var bytesWritten: Long = 0L

    override fun write(b: Int) {
        out.write(b)
        bytesWritten += 1L
        pace()
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        out.write(buf, off, len)
        bytesWritten += len.toLong()
        pace()
    }

    override fun flush() = out.flush()
    override fun close() = out.close()

    private fun pace() {
        // Time the audio has consumed in real wall-clock terms
        val elapsedSec = (System.nanoTime() - startNanos) / 1_000_000_000.0
        // Time the audio data would represent at playbackSpeed
        val targetSec = (bytesWritten.toDouble() / bytesPerSecond) / playbackSpeed
        val sleepSec = targetSec - elapsedSec
        if (sleepSec > 0.003) { // tolerate up to 3ms drift before sleeping
            val ms = (sleepSec * 1000.0).toLong()
            if (ms > 0) {
                runCatching { Thread.sleep(ms) }
            }
        }
    }
}
