package com.dns.changer.ultimate.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.dns.changer.ultimate.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages GDPR/CCPA consent using Google's User Messaging Platform (UMP) SDK.
 *
 * This is required for serving personalized ads to users in the EEA, UK, and Switzerland.
 * Without consent, only limited ads will be served which impacts monetization.
 *
 * Flow:
 * 1. Call gatherConsent() at app launch
 * 2. UMP SDK checks if user is in a region requiring consent
 * 3. If required, shows consent form automatically
 * 4. After consent is gathered (or not required), canRequestAds() returns true
 * 5. Users can change consent via showPrivacyOptionsForm() in Settings
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ConsentManager"
    }

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    private val _consentStatus = MutableStateFlow(ConsentInformation.ConsentStatus.UNKNOWN)
    val consentStatus: StateFlow<Int> = _consentStatus.asStateFlow()

    private val _isConsentGathered = MutableStateFlow(false)
    val isConsentGathered: StateFlow<Boolean> = _isConsentGathered.asStateFlow()

    /**
     * Check if we can request ads based on current consent status.
     * This can be called synchronously to check the current state.
     */
    fun canRequestAdsSync(): Boolean = consentInformation.canRequestAds()

    /**
     * Gathers consent from the user if required.
     *
     * This should be called at app launch. The UMP SDK will:
     * - Check if the user is in a region requiring consent (EEA, UK, Switzerland)
     * - If consent is required and not yet obtained, show the consent form
     * - If consent is not required (e.g., US user), proceed without showing form
     *
     * @param activity The activity to use for showing the consent form
     * @param onConsentGatheringComplete Called when consent gathering is complete (success or failure)
     */
    fun gatherConsent(
        activity: Activity,
        onConsentGatheringComplete: (FormError?) -> Unit
    ) {
        Log.e(TAG, "🟢 Starting consent gathering process...")
        Log.e(TAG, "🟢 Is DEBUG build: ${BuildConfig.DEBUG}")

        // Build consent request parameters
        val params = buildConsentRequestParameters()

        // Request consent info update
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            // Success - consent info updated
            {
                Log.d(TAG, "Consent info updated successfully")
                Log.d(TAG, "Consent status: ${getConsentStatusString()}")
                Log.d(TAG, "Privacy options required: ${consentInformation.privacyOptionsRequirementStatus}")

                // Load and show consent form if required
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode} - ${formError.message}")
                    } else {
                        Log.d(TAG, "Consent form shown/dismissed or not required")
                    }

                    // Update state
                    updateConsentState()
                    _isConsentGathered.value = true

                    onConsentGatheringComplete(formError)
                }
            },
            // Failure - couldn't get consent info
            { requestError ->
                Log.e(TAG, "Consent info update failed: ${requestError.errorCode} - ${requestError.message}")

                // Still update state and allow ads to be requested
                // (will serve limited ads if consent not obtained)
                updateConsentState()
                _isConsentGathered.value = true

                onConsentGatheringComplete(requestError)
            }
        )
    }

    /**
     * Shows the privacy options form to allow users to change their consent.
     *
     * This should be called from Settings when the user wants to manage their privacy settings.
     * Only show this option if isPrivacyOptionsRequired is true.
     *
     * @param activity The activity to use for showing the form
     * @param onDismissed Called when the form is dismissed
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onDismissed: (FormError?) -> Unit
    ) {
        Log.d(TAG, "Showing privacy options form...")

        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error: ${formError.errorCode} - ${formError.message}")
            } else {
                Log.d(TAG, "Privacy options form dismissed")
            }

            // Update state after user potentially changed consent
            updateConsentState()

            onDismissed(formError)
        }
    }

    /**
     * Resets consent state. Only use for testing purposes.
     */
    fun resetConsent() {
        Log.d(TAG, "Resetting consent state (testing only)")
        consentInformation.reset()
        updateConsentState()
        _isConsentGathered.value = false
    }

    private fun buildConsentRequestParameters(): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()

        // Add debug settings only in debug builds
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "🟢 Adding debug settings for UMP - EEA geography")
            val debugSettings = ConsentDebugSettings.Builder(context)
                // Add test devices to enable debug geography
                .addTestDeviceHashedId("0CB4D26B8A5068966213D6599648CD1F")
                .addTestDeviceHashedId("36693F0AA9928FF6188E92A5231C765D") // SM-F966B
                // Test as if user is in EEA (enables consent form)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
            builder.setConsentDebugSettings(debugSettings)
            Log.e(TAG, "🟢 Debug settings applied with test devices")
        } else {
            Log.e(TAG, "🔴 NOT a debug build - no debug settings applied")
        }

        return builder.build()
    }

    private fun updateConsentState() {
        _canRequestAds.value = consentInformation.canRequestAds()
        _consentStatus.value = consentInformation.consentStatus
        _isPrivacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        Log.d(TAG, "Consent state updated:")
        Log.d(TAG, "  - Can request ads: ${_canRequestAds.value}")
        Log.d(TAG, "  - Consent status: ${getConsentStatusString()}")
        Log.d(TAG, "  - Privacy options required: ${_isPrivacyOptionsRequired.value}")
    }

    private fun getConsentStatusString(): String {
        return when (consentInformation.consentStatus) {
            ConsentInformation.ConsentStatus.UNKNOWN -> "UNKNOWN"
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
            ConsentInformation.ConsentStatus.REQUIRED -> "REQUIRED"
            ConsentInformation.ConsentStatus.OBTAINED -> "OBTAINED"
            else -> "UNKNOWN (${consentInformation.consentStatus})"
        }
    }
}
