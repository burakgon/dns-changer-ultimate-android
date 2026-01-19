package com.dns.changer.ultimate.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dns.changer.ultimate.data.model.PremiumState
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.revenuecat.purchases.CustomerInfo
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    val isPremium: StateFlow<Boolean> = preferences.isPremium
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
                preferences.isPremium.collect { localPremium ->
                    _premiumState.value = _premiumState.value.copy(
                        isPremium = localPremium,
                        isLoading = false
                    )
                }
            }
            return
        }

        try {
            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
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
