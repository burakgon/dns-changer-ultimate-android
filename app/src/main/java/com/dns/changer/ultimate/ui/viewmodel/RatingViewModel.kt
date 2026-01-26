package com.dns.changer.ultimate.ui.viewmodel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dns.changer.ultimate.ads.AnalyticsEvents
import com.dns.changer.ultimate.ads.AnalyticsManager
import com.dns.changer.ultimate.BuildConfig
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.preferences.RatingPreferences
import com.dns.changer.ultimate.service.DnsConnectionManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RatingViewModel @Inject constructor(
    private val ratingPreferences: RatingPreferences,
    private val connectionManager: DnsConnectionManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _showRatingDialog = MutableStateFlow(false)
    val showRatingDialog: StateFlow<Boolean> = _showRatingDialog.asStateFlow()

    private val _shouldRequestReview = MutableStateFlow(false)
    val shouldRequestReview: StateFlow<Boolean> = _shouldRequestReview.asStateFlow()

    private var lastConnectionState: ConnectionState? = null
    private var hasCheckedOnLaunch = false

    init {
        observeConnectionForRating()
        observeLaunchCountForRating()
    }

    private fun observeLaunchCountForRating() {
        viewModelScope.launch {
            // Observe launch count changes and check when it updates
            ratingPreferences.launchCount.collect { count ->
                android.util.Log.d("RatingViewModel", "Launch count changed: $count, hasChecked: $hasCheckedOnLaunch")
                if (count >= 2 && !hasCheckedOnLaunch) {
                    hasCheckedOnLaunch = true
                    checkAndShowPromptIfNeeded()
                }
            }
        }
    }

    private fun observeConnectionForRating() {
        viewModelScope.launch {
            connectionManager.connectionState.collect { state ->
                val wasConnecting = lastConnectionState is ConnectionState.Connecting
                val isNowConnected = state is ConnectionState.Connected

                // Count successful connections
                if (wasConnecting && isNowConnected) {
                    ratingPreferences.incrementSuccessfulConnections()
                    checkAndShowPromptIfNeeded()
                }

                lastConnectionState = state
            }
        }
    }

    private suspend fun checkAndShowPromptIfNeeded() {
        if (ratingPreferences.shouldShowPrompt()) {
            _showRatingDialog.value = true
            analyticsManager.logEvent(AnalyticsEvents.RATING_DIALOG_SHOWN)
        }
    }

    fun onPositiveResponse() {
        analyticsManager.logEvent(AnalyticsEvents.RATING_POSITIVE)
        viewModelScope.launch {
            ratingPreferences.setLikedApp(true)
            ratingPreferences.setHasResponded(true)
            // Trigger native review after a short delay
            delay(300)
            _shouldRequestReview.value = true
        }
    }

    fun onNegativeResponse() {
        analyticsManager.logEvent(AnalyticsEvents.RATING_NEGATIVE)
        viewModelScope.launch {
            ratingPreferences.setLikedApp(false)
            // Don't mark as responded yet - wait for feedback
        }
    }

    fun onFeedbackSubmitted(feedback: String, activity: Activity) {
        analyticsManager.logEvent(AnalyticsEvents.RATING_FEEDBACK_SUBMITTED)
        viewModelScope.launch {
            ratingPreferences.setHasResponded(true)
            _showRatingDialog.value = false

            // Open email intent with feedback
            val deviceInfo = buildString {
                appendLine("---")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("---")
            }

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("b@appcastle.co"))
                putExtra(Intent.EXTRA_SUBJECT, "DNS Changer Feedback")
                putExtra(Intent.EXTRA_TEXT, "$feedback\n\n$deviceInfo")
            }

            try {
                activity.startActivity(Intent.createChooser(emailIntent, "Send Feedback"))
            } catch (e: Exception) {
                // Handle no email app available
            }
        }
    }

    fun onFeedbackSkipped() {
        analyticsManager.logEvent(AnalyticsEvents.RATING_FEEDBACK_SKIPPED)
        viewModelScope.launch {
            ratingPreferences.setHasResponded(true)
            _showRatingDialog.value = false
        }
    }

    fun dismissDialog() {
        _showRatingDialog.value = false
        // Don't mark as responded - show again next launch
    }

    fun onReviewShown() {
        viewModelScope.launch {
            ratingPreferences.setHasShownNativePrompt(true)
            _shouldRequestReview.value = false
        }
    }

    fun requestInAppReview(activity: Activity) {
        viewModelScope.launch {
            val hasShown = ratingPreferences.hasShownNativePrompt.first()
            if (hasShown) {
                _shouldRequestReview.value = false
                return@launch
            }

            try {
                val manager = ReviewManagerFactory.create(activity)
                val request = manager.requestReviewFlow()

                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(activity, reviewInfo)
                        flow.addOnCompleteListener {
                            onReviewShown()
                        }
                    } else {
                        _shouldRequestReview.value = false
                    }
                }
            } catch (e: Exception) {
                _shouldRequestReview.value = false
            }
        }
    }
}
