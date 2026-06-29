package com.example.platform

import android.content.Context

interface HapticController {
    fun vibrate(context: Context, durationMs: Long = 30)
}
