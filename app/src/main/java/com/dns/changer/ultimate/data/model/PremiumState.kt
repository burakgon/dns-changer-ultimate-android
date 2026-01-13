package com.dns.changer.ultimate.data.model

data class PremiumState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    val offerings: List<PremiumOffering> = emptyList()
)

data class PremiumOffering(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val period: String
)

sealed class PremiumFeature {
    data object SpeedTest : PremiumFeature()
    data object CustomDns : PremiumFeature()
    data object NoAds : PremiumFeature()
}
