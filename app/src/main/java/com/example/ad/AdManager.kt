package com.example.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

import com.example.BuildConfig

object AdManager {
    private const val TAG = "AdManager"

    // Load ad unit IDs from BuildConfig (debug uses test IDs, release uses real IDs)
    val BANNER_AD_UNIT_ID: String = BuildConfig.BANNER_AD_UNIT_ID
    val INTERSTITIAL_AD_UNIT_ID: String = BuildConfig.INTERSTITIAL_AD_UNIT_ID
    val REWARDED_AD_UNIT_ID: String = BuildConfig.REWARDED_AD_UNIT_ID

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    fun initialize(context: Context) {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "AdMob SDK Initialized successfully. Status: $status")
            loadInterstitial(context)
            loadRewarded(context)
        }
    }

    fun loadInterstitial(context: Context) {
        if (mInterstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed with error: ${adError.message}")
                    mInterstitialAd = null
                    isInterstitialLoading = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    mInterstitialAd = interstitialAd
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        val ad = mInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    mInterstitialAd = null
                    loadInterstitial(activity)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                    mInterstitialAd = null
                    loadInterstitial(activity)
                    onAdClosed()
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial ad not loaded yet, calling fallback")
            loadInterstitial(activity)
            onAdClosed()
        }
    }

    fun loadRewarded(context: Context) {
        if (mRewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${adError.message}")
                    mRewardedAd = null
                    isRewardedLoading = false
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    mRewardedAd = rewardedAd
                    isRewardedLoading = false
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onRewardEarned: (amount: Int) -> Unit, onAdClosed: () -> Unit) {
        val ad = mRewardedAd
        if (ad != null) {
            var earnedReward = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed")
                    mRewardedAd = null
                    loadRewarded(activity)
                    if (earnedReward) {
                        onRewardEarned(3) // Reward size
                    }
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                    mRewardedAd = null
                    loadRewarded(activity)
                    onAdClosed()
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                earnedReward = true
            }
        } else {
            Log.d(TAG, "Rewarded ad not loaded yet, calling fallback")
            loadRewarded(activity)
            onAdClosed()
        }
    }
}
