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
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.Offerings
import com.dns.changer.ultimate.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
        checkPremiumStatus()
        fetchProducts()
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

        // In debug builds, respect the local DataStore value for testing
        // Don't overwrite it with RevenueCat status
        if (BuildConfig.DEBUG) {
            viewModelScope.launch {
                // Combine premium status and debug subscription status
                kotlinx.coroutines.flow.combine(
                    preferences.isPremium,
                    preferences.debugSubscriptionStatus
                ) { localPremium, debugStatus ->
                    Pair(localPremium, debugStatus)
                }.collect { (localPremium, debugStatus) ->
                    val status = if (localPremium) {
                        try {
                            SubscriptionStatus.valueOf(debugStatus)
                        } catch (e: Exception) {
                            SubscriptionStatus.ACTIVE
                        }
                    } else {
                        SubscriptionStatus.NONE
                    }

                    // Determine if user has premium ACCESS based on status
                    // According to Google Play Billing:
                    // - ACTIVE: has access
                    // - GRACE_PERIOD: has access (billing failing but grace period active)
                    // - CANCELLED: has access (until period ends)
                    // - PAUSED: NO access (user paused)
                    // - BILLING_ISSUE: NO access (account on hold)
                    // - EXPIRED: NO access
                    // - NONE: NO access
                    val hasAccess = when (status) {
                        SubscriptionStatus.ACTIVE,
                        SubscriptionStatus.GRACE_PERIOD,
                        SubscriptionStatus.CANCELLED -> true
                        SubscriptionStatus.PAUSED,
                        SubscriptionStatus.BILLING_ISSUE,
                        SubscriptionStatus.EXPIRED,
                        SubscriptionStatus.NONE -> false
                    }

                    // Create appropriate subscription details based on status
                    val details = if (localPremium) {
                        val now = Date()
                        val futureDate = Date(now.time + 30L * 24 * 60 * 60 * 1000) // 30 days from now
                        val pastDate = Date(now.time - 7L * 24 * 60 * 60 * 1000) // 7 days ago

                        // willRenew logic per Google Play:
                        // - ACTIVE: true (will auto-renew)
                        // - GRACE_PERIOD: true (still attempting to renew)
                        // - PAUSED: true (will auto-resume)
                        // - BILLING_ISSUE: true (still trying to collect payment)
                        // - CANCELLED: false (user cancelled auto-renewal)
                        // - EXPIRED: false (subscription ended)
                        val willRenew = when (status) {
                            SubscriptionStatus.ACTIVE,
                            SubscriptionStatus.GRACE_PERIOD,
                            SubscriptionStatus.PAUSED,
                            SubscriptionStatus.BILLING_ISSUE -> true
                            SubscriptionStatus.CANCELLED,
                            SubscriptionStatus.EXPIRED,
                            SubscriptionStatus.NONE -> false
                        }

                        SubscriptionDetails(
                            productId = "debug_premium",
                            planName = "Debug Premium (${status.name})",
                            expirationDate = when (status) {
                                SubscriptionStatus.EXPIRED -> pastDate
                                SubscriptionStatus.PAUSED -> pastDate // Paused has past expiry
                                SubscriptionStatus.BILLING_ISSUE -> pastDate // Account hold has past expiry
                                SubscriptionStatus.CANCELLED -> futureDate // Access until this date
                                else -> futureDate
                            },
                            gracePeriodExpirationDate = if (status == SubscriptionStatus.GRACE_PERIOD) futureDate else null,
                            willRenew = willRenew,
                            billingIssueDetectedAt = if (status == SubscriptionStatus.GRACE_PERIOD || status == SubscriptionStatus.BILLING_ISSUE) pastDate else null,
                            unsubscribeDetectedAt = if (status == SubscriptionStatus.CANCELLED) pastDate else null,
                            periodType = "yearly",
                            managementUrl = "https://play.google.com/store/account/subscriptions"
                        )
                    } else null

                    _premiumState.value = _premiumState.value.copy(
                        isPremium = hasAccess,
                        isLoading = false,
                        subscriptionStatus = status,
                        subscriptionDetails = details
                    )
                }
            }
            return
        }

        try {
            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val entitlement = customerInfo.entitlements[ENTITLEMENT_ID]
                    val isPremium = entitlement?.isActive == true
                    val (status, details) = parseSubscriptionInfo(entitlement, customerInfo)

                    _premiumState.value = _premiumState.value.copy(
                        isPremium = isPremium,
                        isLoading = false,
                        subscriptionStatus = status,
                        subscriptionDetails = details
                    )
                    viewModelScope.launch {
                        preferences.setPremium(isPremium)
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

    /**
     * Parse EntitlementInfo to determine subscription status and details
     */
    private fun parseSubscriptionInfo(
        entitlement: EntitlementInfo?,
        customerInfo: CustomerInfo
    ): Pair<SubscriptionStatus, SubscriptionDetails?> {
        if (entitlement == null) {
            return SubscriptionStatus.NONE to null
        }

        val now = Date()
        val expirationDate = entitlement.expirationDate
        val billingIssueDetectedAt = entitlement.billingIssueDetectedAt
        val unsubscribeDetectedAt = entitlement.unsubscribeDetectedAt
        val willRenew = entitlement.willRenew

        // Determine subscription status based on Google Play Billing states:
        // - ACTIVE: isActive=true, normal subscription
        // - IN_GRACE_PERIOD: isActive=true, billingIssue detected (billing failed but still has access)
        // - ON_HOLD (Account Hold): isActive=false, billingIssue detected (after grace period, no access)
        // - PAUSED: isActive=false, willRenew=true, no billingIssue (user paused, will auto-resume)
        // - CANCELED: isActive=true, willRenew=false, unsubscribe detected (access until period ends)
        // - EXPIRED: isActive=false, expirationDate in past
        val status = when {
            // Grace Period: Billing issue detected but subscription is still active
            // User has access during this period while Google retries payment
            billingIssueDetectedAt != null && entitlement.isActive -> SubscriptionStatus.GRACE_PERIOD

            // Account Hold (BILLING_ISSUE): Billing issue + no longer active
            // Grace period expired, user lost access but can still recover by fixing payment
            billingIssueDetectedAt != null && !entitlement.isActive -> SubscriptionStatus.BILLING_ISSUE

            // Cancelled but still active: User cancelled but has access until period ends
            // unsubscribeDetectedAt is set when user cancels auto-renewal
            unsubscribeDetectedAt != null && entitlement.isActive && !willRenew -> SubscriptionStatus.CANCELLED

            // Active subscription in good standing
            entitlement.isActive -> SubscriptionStatus.ACTIVE

            // Paused (Google Play feature): Not active, but willRenew is true (will auto-resume)
            // No billing issue, no unsubscribe - user explicitly paused
            // Expiration date is in the past (access ended when pause started)
            !entitlement.isActive && willRenew &&
                billingIssueDetectedAt == null && unsubscribeDetectedAt == null -> SubscriptionStatus.PAUSED

            // Expired: Not active, expiration date in past, won't renew
            expirationDate != null && expirationDate.before(now) && !entitlement.isActive -> SubscriptionStatus.EXPIRED

            else -> SubscriptionStatus.NONE
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
        _premiumState.value = _premiumState.value.copy(isLoading = true)

        try {
            Purchases.sharedInstance.purchase(
                purchaseParams = com.revenuecat.purchases.PurchaseParams.Builder(activity, pkg).build(),
                callback = object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        val isPremium = customerInfo.entitlements.active.containsKey(ENTITLEMENT_ID)
                        _premiumState.value = _premiumState.value.copy(
                            isPremium = isPremium,
                            isLoading = false
                        )
                        viewModelScope.launch {
                            preferences.setPremium(isPremium)
                        }
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        _premiumState.value = _premiumState.value.copy(isLoading = false)
                    }
                }
            )
        } catch (e: Exception) {
            _premiumState.value = _premiumState.value.copy(isLoading = false)
        }
    }

    fun purchaseProduct(activity: Activity, product: StoreProduct) {
        _premiumState.value = _premiumState.value.copy(isLoading = true)

        try {
            Purchases.sharedInstance.purchase(
                purchaseParams = com.revenuecat.purchases.PurchaseParams.Builder(activity, product).build(),
                callback = object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        val isPremium = customerInfo.entitlements.active.containsKey(ENTITLEMENT_ID)
                        _premiumState.value = _premiumState.value.copy(
                            isPremium = isPremium,
                            isLoading = false
                        )
                        viewModelScope.launch {
                            preferences.setPremium(isPremium)
                        }
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        _premiumState.value = _premiumState.value.copy(isLoading = false)
                    }
                }
            )
        } catch (e: Exception) {
            _premiumState.value = _premiumState.value.copy(isLoading = false)
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
        _premiumState.value = _premiumState.value.copy(isLoading = true)

        try {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val isPremium = customerInfo.entitlements.active.containsKey(ENTITLEMENT_ID)
                    _premiumState.value = _premiumState.value.copy(
                        isPremium = isPremium,
                        isLoading = false
                    )
                    viewModelScope.launch {
                        preferences.setPremium(isPremium)
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
