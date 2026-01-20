package com.dns.changer.ultimate.ui.viewmodel

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dns.changer.ultimate.data.preferences.AppLockPreferences
import com.dns.changer.ultimate.data.preferences.LockType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppLockUiState(
    val isLocked: Boolean = true,
    val isAppLockEnabled: Boolean = false,
    val lockType: LockType = LockType.BOTH,
    val isBiometricEnabled: Boolean = true,
    val isPinError: Boolean = false,
    val isLockout: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
    val failedAttempts: Int = 0,
    val errorMessage: String? = null,
    val isInitialized: Boolean = false
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLockPreferences: AppLockPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    val isAppLockEnabled: StateFlow<Boolean> = appLockPreferences.isAppLockEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val lockType: StateFlow<LockType> = appLockPreferences.lockType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LockType.BOTH
        )

    val isBiometricEnabled: StateFlow<Boolean> = appLockPreferences.isBiometricEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val lockTimeout: StateFlow<Long> = appLockPreferences.lockTimeout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLockPreferences.TIMEOUT_ALWAYS
        )

    init {
        viewModelScope.launch {
            // Combine all relevant flows to update UI state
            combine(
                appLockPreferences.isAppLockEnabled,
                appLockPreferences.lockType,
                appLockPreferences.isBiometricEnabled,
                appLockPreferences.failedAttempts,
                appLockPreferences.lockoutUntil
            ) { enabled, type, biometric, attempts, lockoutUntil ->
                val isLockedOut = lockoutUntil > System.currentTimeMillis()
                val remainingSeconds = if (isLockedOut) {
                    ((lockoutUntil - System.currentTimeMillis()) / 1000).toInt()
                } else 0

                _uiState.value.copy(
                    isAppLockEnabled = enabled,
                    lockType = type,
                    isBiometricEnabled = biometric,
                    failedAttempts = attempts.toInt(),
                    isLockout = isLockedOut,
                    lockoutRemainingSeconds = remainingSeconds,
                    isInitialized = true
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        // Start lockout timer if needed
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.isLockout) {
                    val remaining = appLockPreferences.getRemainingLockoutTime()
                    if (remaining > 0) {
                        _uiState.value = _uiState.value.copy(
                            lockoutRemainingSeconds = (remaining / 1000).toInt()
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLockout = false,
                            lockoutRemainingSeconds = 0
                        )
                        appLockPreferences.resetFailedAttempts()
                    }
                }
                delay(1000)
            }
        }
    }

    suspend fun checkShouldLock(): Boolean {
        return appLockPreferences.shouldLock()
    }

    suspend fun hasPin(): Boolean {
        return appLockPreferences.hasPin()
    }

    fun setLocked(locked: Boolean) {
        _uiState.value = _uiState.value.copy(isLocked = locked)
    }

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            if (appLockPreferences.isLockedOut()) {
                _uiState.value = _uiState.value.copy(
                    isLockout = true,
                    isPinError = false
                )
                return@launch
            }

            val isValid = appLockPreferences.verifyPin(pin)
            if (isValid) {
                appLockPreferences.resetFailedAttempts()
                appLockPreferences.setLastUnlockTime(System.currentTimeMillis())
                _uiState.value = _uiState.value.copy(
                    isLocked = false,
                    isPinError = false,
                    errorMessage = null
                )
            } else {
                val attempts = appLockPreferences.incrementFailedAttempts()
                val isLockedOut = appLockPreferences.isLockedOut()
                val remaining = if (isLockedOut) {
                    (appLockPreferences.getRemainingLockoutTime() / 1000).toInt()
                } else 0

                _uiState.value = _uiState.value.copy(
                    isPinError = true,
                    failedAttempts = attempts.toInt(),
                    isLockout = isLockedOut,
                    lockoutRemainingSeconds = remaining,
                    errorMessage = if (isLockedOut) null else "Wrong PIN. ${AppLockPreferences.MAX_FAILED_ATTEMPTS - attempts} attempts remaining"
                )

                // Reset pin error after animation
                delay(300)
                _uiState.value = _uiState.value.copy(isPinError = false)
            }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            appLockPreferences.resetFailedAttempts()
            appLockPreferences.setLastUnlockTime(System.currentTimeMillis())
            _uiState.value = _uiState.value.copy(
                isLocked = false,
                isPinError = false,
                errorMessage = null
            )
        }
    }

    fun onBiometricError(errorMessage: String?) {
        _uiState.value = _uiState.value.copy(
            errorMessage = errorMessage
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            isPinError = false
        )
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            appLockPreferences.setPin(pin)
            appLockPreferences.setAppLockEnabled(true)
            _uiState.value = _uiState.value.copy(
                isAppLockEnabled = true,
                isLocked = false
            )
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            appLockPreferences.disableAppLock()
            _uiState.value = _uiState.value.copy(
                isAppLockEnabled = false,
                isLocked = false
            )
        }
    }

    fun setLockType(type: LockType) {
        viewModelScope.launch {
            appLockPreferences.setLockType(type)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appLockPreferences.setBiometricEnabled(enabled)
        }
    }

    fun setLockTimeout(timeout: Long) {
        viewModelScope.launch {
            appLockPreferences.setLockTimeout(timeout)
        }
    }

    // Biometric helper functions
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun getBiometricStatus(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Don't show error for user cancellation
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Individual attempt failed, but more attempts may be allowed
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(com.dns.changer.ultimate.R.string.app_lock_biometric_title))
            .setSubtitle(activity.getString(com.dns.changer.ultimate.R.string.app_lock_biometric_subtitle))
            .setNegativeButtonText(activity.getString(com.dns.changer.ultimate.R.string.app_lock_use_pin))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NOT_ENROLLED,
    UNAVAILABLE
}
