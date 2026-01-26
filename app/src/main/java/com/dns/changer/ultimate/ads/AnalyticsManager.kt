package com.dns.changer.ultimate.ads

import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CompositionLocal for accessing AnalyticsManager in Compose.
 * Provided in DnsChangerApp so all composables can access it.
 */
val LocalAnalyticsManager = staticCompositionLocalOf<AnalyticsManager> {
    error("AnalyticsManager not provided")
}

/**
 * Centralized event name constants for Firebase Analytics.
 * Using consistent naming ensures clean data in the Firebase console.
 *
 * Naming convention: snake_case, max 40 chars, prefix by feature area.
 */
object AnalyticsEvents {
    // -- Connection --
    const val DNS_CONNECT_TAP = "dns_connect_tap"
    const val DNS_DISCONNECT_TAP = "dns_disconnect_tap"
    const val DNS_CONNECTED = "dns_connected"
    const val DNS_DISCONNECTED = "dns_disconnected"
    const val DNS_CONNECTION_ERROR = "dns_connection_error"
    const val DNS_SERVER_SWITCH = "dns_server_switch"

    // -- Server Picker --
    const val SERVER_PICKER_OPENED = "server_picker_opened"
    const val SERVER_PICKER_SEARCH = "server_picker_search"
    const val SERVER_PICKER_FILTER = "server_picker_filter"
    const val SERVER_PICKER_SELECTED = "server_picker_selected"

    // -- Custom DNS --
    const val CUSTOM_DNS_DIALOG_OPENED = "custom_dns_dialog_opened"
    const val CUSTOM_DNS_SAVED = "custom_dns_saved"
    const val CUSTOM_DNS_DELETED = "custom_dns_deleted"

    // -- Speed Test --
    const val SPEED_TEST_STARTED = "speed_test_started"
    const val SPEED_TEST_COMPLETED = "speed_test_completed"
    const val SPEED_TEST_APPLY_SERVER = "speed_test_apply_server"
    const val SPEED_TEST_CONNECT_TAP = "speed_test_connect_tap"

    // -- Leak Test --
    const val LEAK_TEST_STARTED = "leak_test_started"
    const val LEAK_TEST_COMPLETED = "leak_test_completed"

    // -- Settings --
    const val SETTING_CHANGED = "setting_changed"
    const val THEME_CHANGED = "theme_changed"

    // -- Paywall / Premium --
    const val PAYWALL_VIEWED = "paywall_viewed"
    const val PAYWALL_DISMISSED = "paywall_dismissed"
    const val PAYWALL_PLAN_SELECTED = "paywall_plan_selected"
    const val PURCHASE_INITIATED = "purchase_initiated"
    const val PURCHASE_COMPLETED = "purchase_completed"
    const val PURCHASE_FAILED = "purchase_failed"
    const val RESTORE_PURCHASES_TAP = "restore_purchases_tap"
    const val PREMIUM_GATE_SHOWN = "premium_gate_shown"
    const val PREMIUM_GATE_WATCH_AD = "premium_gate_watch_ad"
    const val PREMIUM_GATE_GO_PREMIUM = "premium_gate_go_premium"
    const val PREMIUM_GATE_DISMISSED = "premium_gate_dismissed"

    // -- Rating --
    const val RATING_DIALOG_SHOWN = "rating_dialog_shown"
    const val RATING_POSITIVE = "rating_positive"
    const val RATING_NEGATIVE = "rating_negative"
    const val RATING_FEEDBACK_SUBMITTED = "rating_feedback_submitted"
    const val RATING_FEEDBACK_SKIPPED = "rating_feedback_skipped"

    // -- Ads --
    const val REWARDED_AD_SHOWN = "rewarded_ad_shown"
    const val REWARDED_AD_COMPLETED = "rewarded_ad_completed"
    const val REWARDED_AD_FAILED = "rewarded_ad_failed"
    const val INTERSTITIAL_AD_SHOWN = "interstitial_ad_shown"
    const val INTERSTITIAL_AD_FAILED = "interstitial_ad_failed"

    // -- Navigation --
    const val TAB_SELECTED = "tab_selected"

    // -- Widget & Quick Settings --
    const val WIDGET_CONNECT_TAP = "widget_connect_tap"
    const val WIDGET_DISCONNECT_TAP = "widget_disconnect_tap"
    const val QUICK_SETTINGS_TAP = "quick_settings_tap"

    // -- Boot --
    const val BOOT_AUTO_CONNECT = "boot_auto_connect"

    // -- Disclosure Dialogs --
    const val DATA_DISCLOSURE_SHOWN = "data_disclosure_shown"
    const val DATA_DISCLOSURE_ACCEPTED = "data_disclosure_accepted"
    const val VPN_DISCLOSURE_SHOWN = "vpn_disclosure_shown"
    const val VPN_DISCLOSURE_ACCEPTED = "vpn_disclosure_accepted"

    // -- App Lock --
    const val APP_LOCK_SHOWN = "app_lock_shown"
    const val APP_LOCK_PIN_SUCCESS = "app_lock_pin_success"
    const val APP_LOCK_PIN_FAILED = "app_lock_pin_failed"
    const val APP_LOCK_BIOMETRIC_SUCCESS = "app_lock_biometric_success"
    const val APP_LOCK_BIOMETRIC_FAILED = "app_lock_biometric_failed"

    // -- Overlays --
    const val CONNECTION_OVERLAY_SHOWN = "connection_overlay_shown"
    const val DISCONNECTION_OVERLAY_SHOWN = "disconnection_overlay_shown"

    // -- Subscription Status --
    const val SUBSCRIPTION_STATUS_DIALOG = "subscription_status_dialog"
}

/**
 * Centralized parameter name constants for Firebase Analytics.
 */
object AnalyticsParams {
    const val SERVER_NAME = "server_name"
    const val SERVER_CATEGORY = "server_category"
    const val DNS_PRIMARY = "dns_primary"
    const val DNS_SECONDARY = "dns_secondary"
    const val DOH_ENABLED = "doh_enabled"
    const val IS_PREMIUM = "is_premium"
    const val IS_CUSTOM = "is_custom"
    const val SEARCH_QUERY = "search_query"
    const val FILTER_CATEGORY = "filter_category"
    const val RESULT_COUNT = "result_count"
    const val FASTEST_SERVER = "fastest_server"
    const val FASTEST_LATENCY_MS = "fastest_latency_ms"
    const val SERVER_COUNT = "server_count"
    const val IS_LEAKED = "is_leaked"
    const val RESOLVER_COUNT = "resolver_count"
    const val SETTING_NAME = "setting_name"
    const val SETTING_VALUE = "setting_value"
    const val THEME_MODE = "theme_mode"
    const val PLAN_ID = "plan_id"
    const val PLAN_PRICE = "plan_price"
    const val PRODUCT_ID = "product_id"
    const val ERROR_MESSAGE = "error_message"
    const val FEATURE_NAME = "feature_name"
    const val TAB_NAME = "tab_name"
    const val SOURCE = "source"
    const val SUBSCRIPTION_STATUS = "subscription_status"
    const val CONNECTION_DURATION_SEC = "connection_duration_sec"
    const val LATENCY_MS = "latency_ms"
}

/**
 * User property name constants for segmentation in Firebase.
 */
object AnalyticsUserProps {
    const val IS_PREMIUM = "is_premium"
    const val SELECTED_SERVER = "selected_server"
    const val DOH_ENABLED = "doh_enabled"
    const val THEME_MODE = "theme_mode"
    const val APP_LOCK_ENABLED = "app_lock_enabled"
    const val CUSTOM_DNS_COUNT = "custom_dns_count"
}

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
     * Log a screen view event using Firebase's standard screen_view event.
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        if (!isEnabled) {
            Log.d(TAG, "Analytics not enabled, skipping screen view: $screenName")
            return
        }

        val bundle = android.os.Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
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
