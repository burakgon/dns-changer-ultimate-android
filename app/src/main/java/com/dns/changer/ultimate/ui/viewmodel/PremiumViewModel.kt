package com.dns.changer.ultimate.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dns.changer.ultimate.data.model.PremiumState
import com.dns.changer.ultimate.data.model.SubscriptionDetails
import com.dns.changer.ultimate.data.model.SubscriptionStatus
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.Offerings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val preferences: DnsPreferences
) : ViewModel() {

    companion object {
        private const val ENTITLEMENT_ID = "premium"
        // RevenueCat package identifiers (from Offerings)
        const val PACKAGE_ID_MONTHLY = "short_sku"
        const val PACKAGE_ID_YEARLY = "long_sku"
        const val PACKAGE_ID_YEARLY_TRIAL = "trial_sku"
    }

    private val _premiumState = MutableStateFlow(PremiumState())
    val premiumState: StateFlow<PremiumState> = _premiumState.asStateFlow()

    // Store packages for paywall (keyed by package identifier)
    private val _packages = MutableStateFlow<Map<String, Package>>(emptyMap())
    val packages: StateFlow<Map<String, Package>> = _packages.asStateFlow()

    // Store products for paywall (derived from packages, keyed by package identifier)
    private val _products = MutableStateFlow<Map<String, StoreProduct>>(emptyMap())
    val products: StateFlow<Map<String, StoreProduct>> = _products.asStateFlow()

    // isPremium is derived from premiumState - this is the UNIFIED source of truth
    // It considers subscription status (PAUSED, BILLING_ISSUE, EXPIRED = no access)
    val isPremium: StateFlow<Boolean> = _premiumState
        .map { it.isPremium }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val hasWatchedAd: StateFlow<Boolean> = preferences.hasWatchedAd
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        setupCustomerInfoListener()
        checkPremiumStatus()
        fetchProducts()
    }

    /**
     * Set up listener for real-time customer info updates (e.g., after purchase completes)
     * This listener is CRITICAL - it updates state when purchases complete, subscriptions renew, etc.
     */
    private fun setupCustomerInfoListener() {
        try {
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                android.util.Log.d("PremiumViewModel", "CustomerInfo updated via listener")
                updatePremiumStateFromCustomerInfo(customerInfo)
            }
            android.util.Log.d("PremiumViewModel", "CustomerInfo listener set up successfully")
        } catch (e: Exception) {
            android.util.Log.e("PremiumViewModel", "Failed to set up customer info listener", e)
        }
    }

    /**
     * Update premium state from CustomerInfo - used by purchase callbacks and listener
     * RevenueCat's entitlement.isActive is the SOURCE OF TRUTH for premium access.
     * It already handles grace periods, cancellations, billing issues, etc.
     */
    private fun updatePremiumStateFromCustomerInfo(customerInfo: CustomerInfo) {
        val entitlement = customerInfo.entitlements[ENTITLEMENT_ID]

        // RevenueCat's entitlement.isActive is the ONLY source of truth for premium access.
        // It already handles: grace periods, paused, cancelled, billing issues, expired, etc.
        // See: https://www.revenuecat.com/docs/customers/customer-info
        val hasPremiumAccess = customerInfo.entitlements.active.containsKey(ENTITLEMENT_ID)

        val (status, details) = parseSubscriptionInfo(entitlement, customerInfo)

        // Logging for debugging
        android.util.Log.d("PremiumViewModel", "=== RevenueCat CustomerInfo ===")
        android.util.Log.d("PremiumViewModel", "Active entitlements: ${customerInfo.entitlements.active.keys}")
        android.util.Log.d("PremiumViewModel", "hasPremiumAccess: $hasPremiumAccess, status: $status")
        if (entitlement != null) {
            android.util.Log.d("PremiumViewModel", "Entitlement '$ENTITLEMENT_ID': isActive=${entitlement.isActive}, willRenew=${entitlement.willRenew}")
        }
        android.util.Log.d("PremiumViewModel", "===============================")

        _premiumState.value = _premiumState.value.copy(
            isPremium = hasPremiumAccess,
            isLoading = false,
            subscriptionStatus = status,
            subscriptionDetails = details,
            errorMessage = null,
            isPurchasePending = false
        )

        // Always persist to DataStore for offline access and boot receiver
        viewModelScope.launch {
            preferences.setPremium(hasPremiumAccess)
        }
    }

    /**
     * Handle purchase error with user-friendly messages based on error code
     */
    private fun handlePurchaseError(error: PurchasesError, userCancelled: Boolean) {
        android.util.Log.e("PremiumViewModel", "Purchase error: ${error.code} - ${error.message}, cancelled=$userCancelled")

        if (userCancelled) {
            // User cancelled - don't show error, just reset loading
            _premiumState.value = _premiumState.value.copy(
                isLoading = false,
                errorMessage = null
            )
            return
        }

        val (errorMessage, isPending) = when (error.code) {
            PurchasesErrorCode.PaymentPendingError ->
                "Your purchase is pending and may complete later. This can happen when awaiting parental approval or additional payment verification." to true

            PurchasesErrorCode.ProductAlreadyPurchasedError ->
                "You already have an active subscription. Try restoring your purchases." to false

            PurchasesErrorCode.ReceiptAlreadyInUseError ->
                "This purchase is linked to another account. Please contact support if you need help." to false

            PurchasesErrorCode.PurchaseNotAllowedError ->
                "Purchase not allowed. Please check your payment method or try again later." to false

            PurchasesErrorCode.StoreProblemError ->
                "There was a problem with the Play Store. Please try again later." to false

            PurchasesErrorCode.NetworkError ->
                "Network error. Please check your connection and try again." to false

            PurchasesErrorCode.PurchaseCancelledError ->
                null to false // Silently handle cancellation

            else ->
                "Purchase failed. Please try again or contact support if the issue persists." to false
        }

        _premiumState.value = _premiumState.value.copy(
            isLoading = false,
            errorMessage = errorMessage,
            isPurchasePending = isPending
        )
    }

    /**
     * Clear error message (call after user dismisses error)
     */
    fun clearError() {
        _premiumState.value = _premiumState.value.copy(errorMessage = null)
    }

    fun fetchProducts() {
        android.util.Log.d("PremiumViewModel", "fetchProducts() called via Offerings API")
        try {
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    val currentOffering = offerings.current
                    android.util.Log.d("PremiumViewModel", "Offerings received, current: ${currentOffering?.identifier}")

                    if (currentOffering != null) {
                        // Get all available packages
                        val availablePackages = currentOffering.availablePackages
                        android.util.Log.d("PremiumViewModel", "Available packages: ${availablePackages.size}")

                        // Map packages by their identifier
                        val packagesMap = availablePackages.associateBy { it.identifier }
                        _packages.value = packagesMap

                        // Also map products by package identifier for PaywallScreen compatibility
                        val productsMap = availablePackages.associate { pkg ->
                            android.util.Log.d("PremiumViewModel", "Package: ${pkg.identifier} -> ${pkg.product.id} - ${pkg.product.price.formatted}")
                            pkg.identifier to pkg.product
                        }
                        _products.value = productsMap

                        android.util.Log.d("PremiumViewModel", "Products mapped: ${productsMap.keys}")
                    } else {
                        android.util.Log.w("PremiumViewModel", "No current offering available")
                    }
                }

                override fun onError(error: PurchasesError) {
                    android.util.Log.e("PremiumViewModel", "Offerings fetch error: ${error.code} - ${error.message}")
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("PremiumViewModel", "RevenueCat not initialized or error", e)
        }
    }

    fun checkPremiumStatus() {
        _premiumState.value = _premiumState.value.copy(isLoading = true)
        android.util.Log.d("PremiumViewModel", "checkPremiumStatus() called - fetching from RevenueCat")

        try {
            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    android.util.Log.d("PremiumViewModel", "CustomerInfo received from RevenueCat")
                    updatePremiumStateFromCustomerInfo(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    android.util.Log.e("PremiumViewModel", "Error fetching CustomerInfo: ${error.code} - ${error.message}")
                    // On error, fall back to cached DataStore value for offline support
                    viewModelScope.launch {
                        try {
                            val cachedPremium = preferences.isPremium.first()
                            android.util.Log.d("PremiumViewModel", "Using cached premium status: $cachedPremium")
                            _premiumState.value = _premiumState.value.copy(
                                isPremium = cachedPremium,
                                isLoading = false,
                                subscriptionStatus = if (cachedPremium) SubscriptionStatus.ACTIVE else SubscriptionStatus.NONE
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("PremiumViewModel", "Failed to read cached premium status", e)
                            _premiumState.value = _premiumState.value.copy(isLoading = false)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("PremiumViewModel", "Exception in checkPremiumStatus", e)
            _premiumState.value = _premiumState.value.copy(isLoading = false)
        }
    }

    /**
     * Parse EntitlementInfo to determine subscription status for UI display.
     * Note: For ACCESS decisions, use customerInfo.entitlements.active.containsKey() instead.
     */
    private fun parseSubscriptionInfo(
        entitlement: EntitlementInfo?,
        customerInfo: CustomerInfo
    ): Pair<SubscriptionStatus, SubscriptionDetails?> {
        if (entitlement == null) {
            return SubscriptionStatus.NONE to null
        }

        val expirationDate = entitlement.expirationDate
        val billingIssueDetectedAt = entitlement.billingIssueDetectedAt
        val unsubscribeDetectedAt = entitlement.unsubscribeDetectedAt
        val willRenew = entitlement.willRenew
        val isActive = entitlement.isActive

        // Determine subscription status for UI display based on entitlement properties
        // RevenueCat's isActive already handles all the complex logic internally
        val status = when {
            // Grace Period: Has billing issue but still active (RevenueCat keeps isActive=true during grace)
            billingIssueDetectedAt != null && isActive -> SubscriptionStatus.GRACE_PERIOD

            // Account Hold / Billing Issue: Has billing issue and no longer active
            billingIssueDetectedAt != null && !isActive -> SubscriptionStatus.BILLING_ISSUE

            // Cancelled but still active: User cancelled renewal but still has access
            unsubscribeDetectedAt != null && isActive && !willRenew -> SubscriptionStatus.CANCELLED

            // Active: Subscription is active and in good standing
            isActive -> SubscriptionStatus.ACTIVE

            // Paused: Not active, will renew, no billing/cancel issues (Google Play pause feature)
            !isActive && willRenew && billingIssueDetectedAt == null -> SubscriptionStatus.PAUSED

            // Expired: Not active and won't renew
            !isActive && !willRenew -> SubscriptionStatus.EXPIRED

            // Fallback
            else -> SubscriptionStatus.EXPIRED
        }

        // Parse period type from product identifier
        val periodType = when {
            entitlement.productIdentifier.contains("monthly", ignoreCase = true) -> "monthly"
            entitlement.productIdentifier.contains("annual", ignoreCase = true) -> "yearly"
            entitlement.productIdentifier.contains("yearly", ignoreCase = true) -> "yearly"
            entitlement.productIdentifier.contains("weekly", ignoreCase = true) -> "weekly"
            else -> "subscription"
        }

        // Get plan name
        val planName = when (periodType) {
            "monthly" -> "Monthly Plan"
            "yearly" -> "Yearly Plan"
            "weekly" -> "Weekly Plan"
            else -> "Premium"
        }

        val details = SubscriptionDetails(
            productId = entitlement.productIdentifier,
            planName = planName,
            expirationDate = expirationDate,
            gracePeriodExpirationDate = null, // RevenueCat doesn't expose this directly
            willRenew = willRenew,
            billingIssueDetectedAt = billingIssueDetectedAt,
            unsubscribeDetectedAt = unsubscribeDetectedAt,
            periodType = periodType,
            managementUrl = customerInfo.managementURL?.toString()
        )

        return status to details
    }

    fun purchasePremium(activity: Activity) {
        // Default to yearly trial package
        val pkg = _packages.value[PACKAGE_ID_YEARLY_TRIAL]
        if (pkg != null) {
            purchasePackage(activity, pkg)
        } else {
            // Fallback: try to fetch offerings and purchase
            purchasePremiumLegacy(activity)
        }
    }

    fun purchasePackage(activity: Activity, pkg: Package) {
        _premiumState.value = _premiumState.value.copy(isLoading = true, errorMessage = null)

        try {
            Purchases.sharedInstance.purchase(
                purchaseParams = com.revenuecat.purchases.PurchaseParams.Builder(activity, pkg).build(),
                callback = object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        android.util.Log.d("PremiumViewModel", "Purchase completed: ${storeTransaction.orderId}")
                        updatePremiumStateFromCustomerInfo(customerInfo)
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        handlePurchaseError(error, userCancelled)
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("PremiumViewModel", "Purchase exception", e)
            _premiumState.value = _premiumState.value.copy(
                isLoading = false,
                errorMessage = "An unexpected error occurred. Please try again."
            )
        }
    }

    fun purchaseProduct(activity: Activity, product: StoreProduct) {
        _premiumState.value = _premiumState.value.copy(isLoading = true, errorMessage = null)

        try {
            Purchases.sharedInstance.purchase(
                purchaseParams = com.revenuecat.purchases.PurchaseParams.Builder(activity, product).build(),
                callback = object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        android.util.Log.d("PremiumViewModel", "Product purchase completed: ${storeTransaction.orderId}")
                        updatePremiumStateFromCustomerInfo(customerInfo)
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        handlePurchaseError(error, userCancelled)
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("PremiumViewModel", "Product purchase exception", e)
            _premiumState.value = _premiumState.value.copy(
                isLoading = false,
                errorMessage = "An unexpected error occurred. Please try again."
            )
        }
    }

    private fun purchasePremiumLegacy(activity: Activity) {
        _premiumState.value = _premiumState.value.copy(isLoading = true)

        try {
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    val pkg = offerings.current?.getPackage(PACKAGE_ID_YEARLY_TRIAL)
                    if (pkg != null) {
                        purchasePackage(activity, pkg)
                    } else {
                        _premiumState.value = _premiumState.value.copy(isLoading = false)
                    }
                }

                override fun onError(error: PurchasesError) {
                    _premiumState.value = _premiumState.value.copy(isLoading = false)
                }
            })
        } catch (e: Exception) {
            _premiumState.value = _premiumState.value.copy(isLoading = false)
        }
    }

    fun restorePurchases() {
        _premiumState.value = _premiumState.value.copy(isLoading = true, errorMessage = null)

        try {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    android.util.Log.d("PremiumViewModel", "Restore purchases completed")
                    val entitlement = customerInfo.entitlements[ENTITLEMENT_ID]
                    val isPremium = entitlement?.isActive == true
                    updatePremiumStateFromCustomerInfo(customerInfo)

                    // Show message if no purchases found
                    if (!isPremium) {
                        _premiumState.value = _premiumState.value.copy(
                            errorMessage = "No previous purchases found for this account."
                        )
                    }
                }

                override fun onError(error: PurchasesError) {
                    android.util.Log.e("PremiumViewModel", "Restore purchases error: ${error.code} - ${error.message}")
                    _premiumState.value = _premiumState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to restore purchases. Please check your connection and try again."
                    )
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("PremiumViewModel", "Restore purchases exception", e)
            _premiumState.value = _premiumState.value.copy(
                isLoading = false,
                errorMessage = "An unexpected error occurred while restoring purchases."
            )
        }
    }

    fun onAdWatched() {
        viewModelScope.launch {
            preferences.setHasWatchedAd(true)
        }
    }

    fun clearAdWatch() {
        viewModelScope.launch {
            preferences.clearAdWatchStatus()
        }
    }
}
