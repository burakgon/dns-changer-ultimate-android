package com.dns.changer.ultimate.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dns.changer.ultimate.data.model.PremiumState
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
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
        // Subscription product IDs
        const val PRODUCT_ID_MONTHLY = "dc_sub_1_month_7.50try"
        const val PRODUCT_ID_YEARLY = "dc_sub_1_year_32.00try"
        const val PRODUCT_ID_YEARLY_TRIAL = "dc_sub_trial_1_year_32.00try"
        // All product IDs
        val ALL_PRODUCT_IDS = listOf(PRODUCT_ID_MONTHLY, PRODUCT_ID_YEARLY, PRODUCT_ID_YEARLY_TRIAL)
    }

    private val _premiumState = MutableStateFlow(PremiumState())
    val premiumState: StateFlow<PremiumState> = _premiumState.asStateFlow()

    // Store products for paywall
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
        try {
            Purchases.sharedInstance.getProducts(
                ALL_PRODUCT_IDS,
                callback = object : GetStoreProductsCallback {
                    override fun onReceived(storeProducts: List<StoreProduct>) {
                        _products.value = storeProducts.associateBy { it.id }
                    }

                    override fun onError(error: PurchasesError) {
                        // Products fetch failed, will use fallback prices
                    }
                }
            )
        } catch (e: Exception) {
            // RevenueCat not initialized
        }
    }

    fun checkPremiumStatus() {
        _premiumState.value = _premiumState.value.copy(isLoading = true)

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
        // Default to yearly trial
        val product = _products.value[PRODUCT_ID_YEARLY_TRIAL]
        if (product != null) {
            purchaseProduct(activity, product)
        } else {
            // Fallback: fetch and purchase
            purchasePremiumLegacy(activity)
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
            Purchases.sharedInstance.getProducts(
                listOf(PRODUCT_ID_YEARLY_TRIAL),
                callback = object : GetStoreProductsCallback {
                    override fun onReceived(storeProducts: List<StoreProduct>) {
                        val product = storeProducts.firstOrNull()
                        if (product != null) {
                            purchaseProduct(activity, product)
                        } else {
                            _premiumState.value = _premiumState.value.copy(isLoading = false)
                        }
                    }

                    override fun onError(error: PurchasesError) {
                        _premiumState.value = _premiumState.value.copy(isLoading = false)
                    }
                }
            )
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
