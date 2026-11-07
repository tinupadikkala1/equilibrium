package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

/** Each playback worker owns its AudioTrack, including releasing it on every exit path. */
object AmbientMusicPlayer {
    @Volatile private var generation = 0
    private var worker: Thread? = null

    @Synchronized fun start() {
        if (worker?.isAlive == true) return
        val token = ++generation
        worker = Thread({ play(token) }, "EquilibriumAmbient").apply {
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    @Synchronized fun stop() {
        generation += 1
        worker?.interrupt()
        worker = null
    }

    private fun play(token: Int) {
        var track: AudioTrack? = null
        try {
            val rate = 22050
            track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(4096, AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)))
                .setTransferMode(AudioTrack.MODE_STREAM).build()
            if (generation != token) return
            track.play()
            val chords = listOf(doubleArrayOf(110.0, 165.0, 220.0, 261.63), doubleArrayOf(87.31, 130.81, 174.61, 220.0), doubleArrayOf(65.41, 98.0, 130.81, 164.81), doubleArrayOf(98.0, 146.83, 196.0, 246.94))
            val buffer = ShortArray(1024)
            val chordSamples = (rate * 4.5).toLong()
            var samples = 0L
            while (generation == token && !Thread.currentThread().isInterrupted) {
                val frequencies = chords[((samples / chordSamples) % chords.size).toInt()]
                for (i in buffer.indices) {
                    val position = samples + i
                    val envelope = sin((position % chordSamples).toDouble() / chordSamples * Math.PI)
                    val t = position.toDouble() / rate
                    val signal = frequencies.sumOf { sin(2 * Math.PI * it * t) } / frequencies.size
                    buffer[i] = (signal * .035 * envelope * Short.MAX_VALUE).toInt().toShort()
                }
                if (track.write(buffer, 0, buffer.size) < 0) break
                samples += buffer.size
            }
        } catch (_: Exception) {
            // Audio is optional; unavailable audio services must not interrupt a puzzle.
        } finally {
            try { track?.stop() } catch (_: Exception) { }
            track?.release()
        }
    }
}
