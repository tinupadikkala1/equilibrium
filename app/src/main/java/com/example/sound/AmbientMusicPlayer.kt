package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

object AmbientMusicPlayer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var thread: Thread? = null

    @Synchronized
    fun start() {
        if (isPlaying) return
        isPlaying = true
        thread = Thread {
            runPlayback()
        }.apply {
            name = "AmbientMusicPlaybackThread"
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    @Synchronized
    fun stop() {
        isPlaying = false
        thread?.interrupt()
        thread = null
        try {
            audioTrack?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }

    private fun runPlayback() {
        val sampleRate = 22050
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufferSize, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)
        
        // Soothing chord progression for mindfulness focus
        // Chord 1: Am (A2, E3, A3, C4)
        // Chord 2: F (F2, C3, F3, A3)
        // Chord 3: C (C2, G2, C3, E3)
        // Chord 4: G (G2, D3, G3, B3)
        val chords = listOf(
            floatArrayOf(110.0f, 165.0f, 220.0f, 261.63f),
            floatArrayOf(87.31f, 130.81f, 174.61f, 220.0f),
            floatArrayOf(65.41f, 98.0f, 130.81f, 164.81f),
            floatArrayOf(98.0f, 146.83f, 196.0f, 246.94f)
        )
        
        var globalSampleCount = 0L
        val secondsPerChord = 4.5
        val samplesPerChord = (sampleRate * secondsPerChord).toLong()

        while (isPlaying) {
            if (Thread.currentThread().isInterrupted) break
            
            val currentChordIndex = ((globalSampleCount / samplesPerChord) % chords.size).toInt()
            val frequencies = chords[currentChordIndex]
            
            for (i in 0 until bufferSize) {
                val actualIndex = globalSampleCount + i
                val chordProgress = (actualIndex % samplesPerChord).toDouble() / samplesPerChord
                val envelope = sin(chordProgress * Math.PI) // Smooth swell swell
                
                val t = actualIndex.toDouble() / sampleRate
                
                var signal = 0.0
                for (freq in frequencies) {
                    signal += sin(2.0 * Math.PI * freq * t)
                }
                
                // Keep audio extremely quiet/ambient and filter harmonics slightly
                val volumeScalar = 0.045 * envelope
                val sampleValue = (signal / frequencies.size * volumeScalar * Short.MAX_VALUE).toInt()
                
                buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            
            try {
                val written = audioTrack?.write(buffer, 0, bufferSize) ?: -1
                if (written < 0) break
            } catch (e: Exception) {
                break
            }
            
            globalSampleCount += bufferSize
        }
    }
}
