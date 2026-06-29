package com.example.platform

import android.app.Activity

interface AdController {
    fun initialize(activity: Activity)
    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit)
    fun showRewarded(activity: Activity, onRewardEarned: (Int) -> Unit, onAdClosed: () -> Unit)
}
