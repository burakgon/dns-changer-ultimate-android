package com.dns.changer.ultimate.ads

import android.content.Context
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase Analytics with proper consent handling.
 *
 * Analytics collection is disabled by default in AndroidManifest.xml.
 * This manager enables analytics ONLY after the user has accepted the data disclosure.
 *
 * Flow:
 * 1. App starts with analytics disabled (manifest setting)
 * 2. User sees DataDisclosureDialog at first launch
 * 3. User accepts disclosure -> enableAnalytics() is called
 * 4. Firebase Analytics starts collecting data with proper consent mode
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AnalyticsManager"
    }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    private var isEnabled = false

    /**
     * Enables Firebase Analytics after user consent.
     * This should be called ONLY after the user accepts the data disclosure dialog.
     *
     * @param adConsentGranted Whether the user also granted consent for ad personalization
     *                         (from UMP SDK for EEA users, or true for non-EEA users)
     */
    fun enableAnalytics(adConsentGranted: Boolean = true) {
        Log.d(TAG, "Enabling Firebase Analytics (ad consent: $adConsentGranted)")

        // Set consent mode based on user's choices using the Map-based API
        val consentMap = mutableMapOf<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus>()
        consentMap[FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE] = FirebaseAnalytics.ConsentStatus.GRANTED
        consentMap[FirebaseAnalytics.ConsentType.AD_STORAGE] =
            if (adConsentGranted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
        consentMap[FirebaseAnalytics.ConsentType.AD_USER_DATA] =
            if (adConsentGranted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
        consentMap[FirebaseAnalytics.ConsentType.AD_PERSONALIZATION] =
            if (adConsentGranted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED

        firebaseAnalytics.setConsent(consentMap)

        // Enable analytics collection
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)
        isEnabled = true

        Log.d(TAG, "Firebase Analytics enabled successfully")
    }

    /**
     * Disables Firebase Analytics.
     * Called if user somehow revokes consent (not typical flow, but good to have).
     */
    fun disableAnalytics() {
        Log.d(TAG, "Disabling Firebase Analytics")

        val consentMap = mutableMapOf<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus>()
        consentMap[FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE] = FirebaseAnalytics.ConsentStatus.DENIED
        consentMap[FirebaseAnalytics.ConsentType.AD_STORAGE] = FirebaseAnalytics.ConsentStatus.DENIED
        consentMap[FirebaseAnalytics.ConsentType.AD_USER_DATA] = FirebaseAnalytics.ConsentStatus.DENIED
        consentMap[FirebaseAnalytics.ConsentType.AD_PERSONALIZATION] = FirebaseAnalytics.ConsentStatus.DENIED

        firebaseAnalytics.setConsent(consentMap)

        firebaseAnalytics.setAnalyticsCollectionEnabled(false)
        isEnabled = false

        Log.d(TAG, "Firebase Analytics disabled")
    }

    /**
     * Updates ad-related consent settings.
     * Called when UMP consent changes (e.g., user changes privacy settings).
     */
    fun updateAdConsent(granted: Boolean) {
        if (!isEnabled) {
            Log.d(TAG, "Analytics not enabled, skipping ad consent update")
            return
        }

        Log.d(TAG, "Updating ad consent: $granted")

        val consentMap = mutableMapOf<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus>()
        // Keep analytics storage granted (user accepted data disclosure)
        consentMap[FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE] = FirebaseAnalytics.ConsentStatus.GRANTED
        // Update ad-related consent based on UMP
        consentMap[FirebaseAnalytics.ConsentType.AD_STORAGE] =
            if (granted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
        consentMap[FirebaseAnalytics.ConsentType.AD_USER_DATA] =
            if (granted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
        consentMap[FirebaseAnalytics.ConsentType.AD_PERSONALIZATION] =
            if (granted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED

        firebaseAnalytics.setConsent(consentMap)
    }

    /**
     * Log a custom event.
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        if (!isEnabled) {
            Log.d(TAG, "Analytics not enabled, skipping event: $eventName")
            return
        }

        val bundle = params?.let { map ->
            android.os.Bundle().apply {
                map.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                    }
                }
            }
        }

        firebaseAnalytics.logEvent(eventName, bundle)
    }

    /**
     * Set user property for analytics segmentation.
     */
    fun setUserProperty(name: String, value: String?) {
        if (!isEnabled) return
        firebaseAnalytics.setUserProperty(name, value)
    }

    /**
     * Set user ID for cross-device analytics (use with caution for privacy).
     */
    fun setUserId(userId: String?) {
        if (!isEnabled) return
        firebaseAnalytics.setUserId(userId)
    }

    fun isAnalyticsEnabled(): Boolean = isEnabled
}
