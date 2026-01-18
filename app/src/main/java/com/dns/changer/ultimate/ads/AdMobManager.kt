package com.dns.changer.ultimate.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AdMobManager"
        // Test ad unit IDs - Replace with your actual ad unit IDs in production
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val INTERSTITIAL_LOAD_TIMEOUT_MS = 6000L // 6 second max load time
        private const val INTERSTITIAL_SHOW_TIMEOUT_MS = 45000L // 45 second max show time (failsafe)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var retryAttempt = 0
    private var interstitialRetryAttempt = 0
    private var isInitialized = false

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    private val _isInterstitialLoaded = MutableStateFlow(false)
    val isInterstitialLoaded: StateFlow<Boolean> = _isInterstitialLoaded.asStateFlow()

    private val _isInterstitialLoading = MutableStateFlow(false)
    val isInterstitialLoading: StateFlow<Boolean> = _isInterstitialLoading.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "AdMob already initialized")
            return
        }

        Log.d(TAG, "Initializing AdMob...")
        MobileAds.initialize(context) { initializationStatus ->
            isInitialized = true
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapter, status) in statusMap) {
                Log.d(TAG, "Adapter: $adapter, Status: ${status.initializationState}, Latency: ${status.latency}ms")
            }
            Log.d(TAG, "AdMob initialized successfully")
            // Start loading the first ad
            loadRewardedAd()
        }
    }

    fun loadRewardedAd() {
        if (_isAdLoading.value) {
            Log.d(TAG, "Ad is already loading, skipping")
            return
        }

        if (_isAdLoaded.value && rewardedAd != null) {
            Log.d(TAG, "Ad already loaded, skipping")
            return
        }

        if (!isInitialized) {
            Log.w(TAG, "AdMob not initialized yet, will retry after initialization")
            return
        }

        _isAdLoading.value = true
        _lastError.value = null

        Log.d(TAG, "Loading rewarded ad (attempt ${retryAttempt + 1}/$MAX_RETRY_ATTEMPTS)")

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully!")
                    rewardedAd = ad
                    _isAdLoaded.value = true
                    _isAdLoading.value = false
                    _lastError.value = null
                    retryAttempt = 0
                    setupFullScreenCallback()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message} (code: ${error.code})")
                    rewardedAd = null
                    _isAdLoaded.value = false
                    _isAdLoading.value = false
                    _lastError.value = error.message

                    // Retry with exponential backoff
                    if (retryAttempt < MAX_RETRY_ATTEMPTS) {
                        retryAttempt++
                        val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl (retryAttempt - 1))
                        Log.d(TAG, "Retrying ad load in ${delayMs}ms (attempt $retryAttempt/$MAX_RETRY_ATTEMPTS)")
                        scope.launch {
                            delay(delayMs)
                            loadRewardedAd()
                        }
                    } else {
                        Log.e(TAG, "Max retry attempts reached. Ad loading failed.")
                        retryAttempt = 0
                    }
                }
            }
        )
    }

    private fun setupFullScreenCallback() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                rewardedAd = null
                _isAdLoaded.value = false
                // Preload the next ad immediately
                scope.launch {
                    delay(500) // Small delay before loading next ad
                    loadRewardedAd()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                _isAdLoaded.value = false
                _lastError.value = adError.message
                // Try to load a new ad
                loadRewardedAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed fullscreen content")
            }

            override fun onAdImpression() {
                Log.d(TAG, "Rewarded ad recorded an impression")
            }

            override fun onAdClicked() {
                Log.d(TAG, "Rewarded ad was clicked")
            }
        }
    }

    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onError: (String) -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null) {
            val errorMsg = _lastError.value ?: "Ad not ready. Please try again in a moment."
            Log.e(TAG, "Cannot show ad: $errorMsg")
            onError(errorMsg)
            // Start loading a new ad
            loadRewardedAd()
            return
        }

        Log.d(TAG, "Showing rewarded ad")
        ad.show(activity, OnUserEarnedRewardListener { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onRewarded()
        })
    }

    fun isRewardedAdReady(): Boolean {
        val isReady = rewardedAd != null
        Log.d(TAG, "isRewardedAdReady: $isReady")
        return isReady
    }

    fun forceReload() {
        Log.d(TAG, "Force reloading ad")
        retryAttempt = 0
        rewardedAd = null
        _isAdLoaded.value = false
        _isAdLoading.value = false
        loadRewardedAd()
    }

    // ============ Interstitial Ad Methods ============

    private var interstitialTimeoutJob: Job? = null
    private var interstitialCallbackInvoked = AtomicBoolean(false)
    private var showTimeoutJob: Job? = null
    private var showCallbackInvoked = AtomicBoolean(false)

    /**
     * Load interstitial ad with a 6-second timeout.
     * If ad doesn't load within timeout, proceeds without showing ad.
     * This prevents users from getting stuck on "Connecting..." or "Disconnecting..." states.
     */
    fun loadInterstitialAd(onLoaded: (() -> Unit)? = null) {
        // Reset callback flag for this load attempt
        interstitialCallbackInvoked.set(false)

        // If already loaded, invoke callback immediately
        if (_isInterstitialLoaded.value && interstitialAd != null) {
            Log.d(TAG, "Interstitial ad already loaded, proceeding immediately")
            invokeInterstitialCallback(onLoaded)
            return
        }

        if (!isInitialized) {
            Log.w(TAG, "AdMob not initialized yet for interstitial, skipping ad")
            invokeInterstitialCallback(onLoaded)
            return
        }

        // Cancel any existing timeout
        interstitialTimeoutJob?.cancel()

        _isInterstitialLoading.value = true
        Log.d(TAG, "Loading interstitial ad with ${INTERSTITIAL_LOAD_TIMEOUT_MS}ms timeout")

        // Start timeout timer
        interstitialTimeoutJob = scope.launch {
            delay(INTERSTITIAL_LOAD_TIMEOUT_MS)
            if (!interstitialCallbackInvoked.get()) {
                Log.w(TAG, "Interstitial ad load timeout (${INTERSTITIAL_LOAD_TIMEOUT_MS}ms). Skipping ad.")
                _isInterstitialLoading.value = false
                invokeInterstitialCallback(onLoaded)
            }
        }

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully!")
                    interstitialTimeoutJob?.cancel()
                    interstitialAd = ad
                    _isInterstitialLoaded.value = true
                    _isInterstitialLoading.value = false
                    interstitialRetryAttempt = 0
                    invokeInterstitialCallback(onLoaded)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message} (code: ${error.code})")
                    interstitialTimeoutJob?.cancel()
                    interstitialAd = null
                    _isInterstitialLoaded.value = false
                    _isInterstitialLoading.value = false
                    // Don't retry - just proceed without ad to not block user
                    Log.d(TAG, "Proceeding without interstitial ad due to load failure")
                    invokeInterstitialCallback(onLoaded)
                }
            }
        )
    }

    /**
     * Thread-safe callback invocation - ensures callback is only called once
     */
    private fun invokeInterstitialCallback(callback: (() -> Unit)?) {
        if (interstitialCallbackInvoked.compareAndSet(false, true)) {
            callback?.invoke()
        }
    }

    fun showInterstitialAd(
        activity: Activity,
        onDismissed: () -> Unit
    ) {
        // Reset show callback flag
        showCallbackInvoked.set(false)

        val ad = interstitialAd
        if (ad == null) {
            Log.e(TAG, "Interstitial ad not ready, proceeding without ad")
            invokeShowCallback(onDismissed)
            return
        }

        // Cancel any existing show timeout
        showTimeoutJob?.cancel()

        // Start show timeout as failsafe (in case ad gets stuck)
        showTimeoutJob = scope.launch {
            delay(INTERSTITIAL_SHOW_TIMEOUT_MS)
            if (!showCallbackInvoked.get()) {
                Log.w(TAG, "Interstitial ad show timeout (${INTERSTITIAL_SHOW_TIMEOUT_MS}ms). Force proceeding.")
                interstitialAd = null
                _isInterstitialLoaded.value = false
                invokeShowCallback(onDismissed)
            }
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                showTimeoutJob?.cancel()
                interstitialAd = null
                _isInterstitialLoaded.value = false
                invokeShowCallback(onDismissed)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                showTimeoutJob?.cancel()
                interstitialAd = null
                _isInterstitialLoaded.value = false
                invokeShowCallback(onDismissed)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed fullscreen content")
            }
        }

        Log.d(TAG, "Showing interstitial ad")
        ad.show(activity)
    }

    /**
     * Thread-safe show callback invocation - ensures callback is only called once
     */
    private fun invokeShowCallback(callback: () -> Unit) {
        if (showCallbackInvoked.compareAndSet(false, true)) {
            callback.invoke()
        }
    }

    fun isInterstitialReady(): Boolean {
        return interstitialAd != null
    }
}
