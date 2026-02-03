package com.dns.changer.ultimate.data.model

import java.util.Date

data class PremiumState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = true, // Default to loading until we fetch actual status from RevenueCat
    val offerings: List<PremiumOffering> = emptyList(),
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.NONE,
    val subscriptionDetails: SubscriptionDetails? = null,
    val errorMessage: String? = null,
    val isPurchasePending: Boolean = false
)

/**
 * Represents the current subscription status.
 * Maps to Google Play Billing subscription states.
 *
 * @see <a href="https://developer.android.com/google/play/billing/lifecycle/subscriptions">Google Play Subscription Lifecycle</a>
 */
enum class SubscriptionStatus {
    /** No active subscription - user has never subscribed or subscription fully ended */
    NONE,

    /** Active subscription in good standing (SUBSCRIPTION_STATE_ACTIVE) */
    ACTIVE,

    /**
     * Grace Period (SUBSCRIPTION_STATE_IN_GRACE_PERIOD)
     * Billing failed but user STILL HAS ACCESS while Google retries payment.
     * Typically 3-30 days depending on store configuration.
     * User should be prompted to fix payment method.
     */
    GRACE_PERIOD,

    /**
     * Paused by user (SUBSCRIPTION_STATE_PAUSED) - Google Play feature
     * User explicitly paused their subscription. NO ACCESS during pause.
     * Subscription will auto-resume at the configured date.
     * willRenew=true, but isActive=false
     */
    PAUSED,

    /**
     * Account Hold (SUBSCRIPTION_STATE_ON_HOLD)
     * Grace period expired, billing still failing. NO ACCESS.
     * User can recover by fixing payment method within hold period (typically 30 days).
     * After hold period expires, subscription is cancelled.
     */
    ACCOUNT_HOLD,

    /**
     * Expired (SUBSCRIPTION_STATE_EXPIRED)
     * Subscription ended and is no longer active. NO ACCESS.
     * User needs to resubscribe to regain access.
     */
    EXPIRED,

    /**
     * Cancelled but still active (SUBSCRIPTION_STATE_CANCELED with future expiry)
     * User cancelled auto-renewal but STILL HAS ACCESS until the billing period ends.
     * willRenew=false, isActive=true, expirationDate in future
     */
    CANCELLED
}

/**
 * Detailed subscription information
 */
data class SubscriptionDetails(
    val productId: String,
    val planName: String,
    val expirationDate: Date?,
    val gracePeriodExpirationDate: Date?,
    val willRenew: Boolean,
    val billingIssueDetectedAt: Date?,
    val unsubscribeDetectedAt: Date?,
    val periodType: String, // "monthly", "yearly", etc.
    val managementUrl: String? = null
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
