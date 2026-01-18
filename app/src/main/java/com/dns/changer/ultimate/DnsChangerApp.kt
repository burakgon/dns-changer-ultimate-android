package com.dns.changer.ultimate

import android.app.Application
import com.dns.changer.ultimate.ads.AdMobManager
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DnsChangerApp : Application() {

    companion object {
        // Replace with your actual RevenueCat API key
        private const val REVENUECAT_API_KEY = "goog_qryzFkHcohaglsKjuSNrNzVkrUc"
    }

    @Inject
    lateinit var adMobManager: AdMobManager

    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob
        adMobManager.initialize()

        // Initialize RevenueCat
        initializeRevenueCat()
    }

    private fun initializeRevenueCat() {
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR

        Purchases.configure(
            PurchasesConfiguration.Builder(this, REVENUECAT_API_KEY)
                .build()
        )
    }
}
