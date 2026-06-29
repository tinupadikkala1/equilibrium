package com.example.platform

import android.app.Activity
import com.example.ad.AdManager

class DefaultAdController : AdController {
    override fun initialize(activity: Activity) {
        AdManager.initialize(activity)
    }

    override fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) { onAdClosed(); return }
        AdManager.showInterstitial(activity, onAdClosed)
    }

    override fun showRewarded(activity: Activity, onRewardEarned: (Int) -> Unit, onAdClosed: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) { onAdClosed(); return }
        AdManager.showRewarded(activity, onRewardEarned, onAdClosed)
    }
}
