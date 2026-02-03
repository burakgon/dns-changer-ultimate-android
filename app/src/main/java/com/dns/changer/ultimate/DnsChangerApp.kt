package com.dns.changer.ultimate

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DnsChangerApp : Application() {

    companion object {
        // Replace with your actual RevenueCat API key
        private const val REVENUECAT_API_KEY = "goog_qryzFkHcohaglsKjuSNrNzVkrUc"
    }

    override fun onCreate() {
        super.onCreate()

        // Note: AdMob is initialized in MainActivity AFTER GDPR consent is gathered
        // This ensures compliance with EU privacy regulations

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
