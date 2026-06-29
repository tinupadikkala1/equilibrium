package com.example.sound

import android.media.AudioManager
import android.media.ToneGenerator

object SoundEffectPlayer {
    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        toneGen?.release()
        toneGen = null
    }

    fun playTap() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSuccess() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playWin() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
