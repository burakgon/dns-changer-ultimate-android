package com.dns.changer.ultimate.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCardOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.SubscriptionDetails
import com.dns.changer.ultimate.data.model.SubscriptionStatus
import com.dns.changer.ultimate.ui.theme.DnsShapes
import com.dns.changer.ultimate.ui.theme.isAndroidTv
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Premium colors
private object SubscriptionColors {
    val GoldLight = Color(0xFFFFD54F)
    val GoldPrimary = Color(0xFFFFB300)
    val GoldDeep = Color(0xFFF57C00)
    val GoldLightMode = Color(0xFFFF9800)
    val GoldDeepLightMode = Color(0xFFE65100)
    val SuccessGreen = Color(0xFF4CAF50)
    val WarningOrange = Color(0xFFFF9800)
    val ErrorRed = Color(0xFFE53935)
}

/**
 * Card displaying the current subscription status in Settings
 */
@Composable
fun SubscriptionStatusCard(
    subscriptionStatus: SubscriptionStatus,
    subscriptionDetails: SubscriptionDetails?,
    isPremium: Boolean,
    onManageSubscription: () -> Unit,
    onShowPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isTv = isAndroidTv()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DnsShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIcon(subscriptionStatus, isPremium)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.subscription_status),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = getStatusDisplayText(subscriptionStatus, isPremium),
                            style = MaterialTheme.typography.bodySmall,
                            color = getStatusColor(subscriptionStatus, isPremium, isDark)
                        )
                    }
                }

                // Status badge
                StatusBadge(subscriptionStatus, isPremium, isDark)
            }

            // Determine if user has an existing subscription (even if suspended)
            // This is different from isPremium (which means has ACCESS)
            val hasSubscription = subscriptionStatus != SubscriptionStatus.NONE &&
                                  subscriptionStatus != SubscriptionStatus.EXPIRED

            // Subscription details (if has any subscription - active or suspended)
            if (subscriptionDetails != null && subscriptionStatus != SubscriptionStatus.NONE) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Plan name
                DetailRow(
                    icon = Icons.Default.WorkspacePremium,
                    label = stringResource(R.string.subscription_plan),
                    value = subscriptionDetails.planName
                )

                // Renewal status (not for cancelled/expired)
                if (subscriptionStatus != SubscriptionStatus.CANCELLED &&
                    subscriptionStatus != SubscriptionStatus.EXPIRED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(
                        icon = Icons.Default.Refresh,
                        label = stringResource(R.string.subscription_renews),
                        value = if (subscriptionDetails.willRenew)
                            stringResource(R.string.subscription_auto_renew)
                        else stringResource(R.string.subscription_no_renew)
                    )
                }

                // Expiration date
                subscriptionDetails.expirationDate?.let { date ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val label = when (subscriptionStatus) {
                        SubscriptionStatus.CANCELLED -> stringResource(R.string.subscription_access_until)
                        SubscriptionStatus.EXPIRED -> stringResource(R.string.subscription_expired_on)
                        SubscriptionStatus.PAUSED -> stringResource(R.string.subscription_paused_since)
                        SubscriptionStatus.ACCOUNT_HOLD -> stringResource(R.string.subscription_on_hold_since)
                        else -> stringResource(R.string.subscription_next_billing)
                    }
                    DetailRow(
                        icon = Icons.Default.CalendarMonth,
                        label = label,
                        value = formatDate(date)
                    )
                }

                // Paused status indicator
                if (subscriptionStatus == SubscriptionStatus.PAUSED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusInfoBox(
                        icon = Icons.Default.Pause,
                        text = stringResource(R.string.subscription_paused_info),
                        backgroundColor = SubscriptionColors.WarningOrange.copy(alpha = 0.15f),
                        textColor = SubscriptionColors.WarningOrange
                    )
                }

                // Billing issue indicator
                if (subscriptionStatus == SubscriptionStatus.ACCOUNT_HOLD) {
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusInfoBox(
                        icon = Icons.Default.CreditCardOff,
                        text = stringResource(R.string.subscription_billing_issue_info),
                        backgroundColor = SubscriptionColors.ErrorRed.copy(alpha = 0.15f),
                        textColor = SubscriptionColors.ErrorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            // Show "Manage Subscription" for any existing subscription (even suspended)
            // Show "Go Premium" only for NONE or EXPIRED
            if (hasSubscription) {
                // Manage subscription button (Google Play policy required)
                val manageInteraction = remember { MutableInteractionSource() }
                val manageFocused by manageInteraction.collectIsFocusedAsState()

                OutlinedButton(
                    onClick = onManageSubscription,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable(interactionSource = manageInteraction)
                        .then(
                            if (isTv && manageFocused) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (subscriptionStatus) {
                            SubscriptionStatus.PAUSED -> stringResource(R.string.resume_subscription)
                            SubscriptionStatus.ACCOUNT_HOLD -> stringResource(R.string.fix_payment)
                            else -> stringResource(R.string.manage_subscription)
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Go Premium button (for NONE or EXPIRED)
                val premiumInteraction = remember { MutableInteractionSource() }
                val premiumFocused by premiumInteraction.collectIsFocusedAsState()

                Button(
                    onClick = onShowPaywall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable(interactionSource = premiumInteraction)
                        .then(
                            if (isTv && premiumFocused) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.go_premium),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: SubscriptionStatus, isPremium: Boolean) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Icon should reflect the SUBSCRIPTION STATUS, not access level
    val (icon, color) = when (status) {
        SubscriptionStatus.ACTIVE -> Icons.Default.CheckCircle to SubscriptionColors.SuccessGreen
        SubscriptionStatus.GRACE_PERIOD -> Icons.Default.Warning to SubscriptionColors.WarningOrange
        SubscriptionStatus.PAUSED -> Icons.Default.Pause to SubscriptionColors.WarningOrange
        SubscriptionStatus.ACCOUNT_HOLD -> Icons.Default.CreditCardOff to SubscriptionColors.ErrorRed
        SubscriptionStatus.CANCELLED -> Icons.Default.Cancel to SubscriptionColors.WarningOrange
        SubscriptionStatus.EXPIRED -> Icons.Default.Error to SubscriptionColors.ErrorRed
        SubscriptionStatus.NONE -> Icons.Default.WorkspacePremium to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = color.copy(alpha = if (isDark) 0.15f else 0.1f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun StatusBadge(status: SubscriptionStatus, isPremium: Boolean, isDark: Boolean) {
    // Status badge should reflect the SUBSCRIPTION STATUS, not access level
    // A user with PAUSED subscription should see "Paused", not "Free"
    // isPremium is only used for NONE status to show "Free"
    val (text, backgroundColor, textColor) = when (status) {
        SubscriptionStatus.ACTIVE -> Triple(
            stringResource(R.string.status_active),
            SubscriptionColors.SuccessGreen.copy(alpha = if (isDark) 0.2f else 0.15f),
            SubscriptionColors.SuccessGreen
        )
        SubscriptionStatus.GRACE_PERIOD -> Triple(
            stringResource(R.string.status_grace_period),
            SubscriptionColors.WarningOrange.copy(alpha = if (isDark) 0.2f else 0.15f),
            SubscriptionColors.WarningOrange
        )
        SubscriptionStatus.PAUSED -> Triple(
            stringResource(R.string.status_paused),
            SubscriptionColors.WarningOrange.copy(alpha = if (isDark) 0.2f else 0.15f),
            SubscriptionColors.WarningOrange
        )
        SubscriptionStatus.ACCOUNT_HOLD -> Triple(
            stringResource(R.string.status_billing_issue),
            SubscriptionColors.ErrorRed.copy(alpha = if (isDark) 0.2f else 0.15f),
            SubscriptionColors.ErrorRed
        )
        SubscriptionStatus.CANCELLED -> Triple(
            stringResource(R.string.status_cancelled),
            SubscriptionColors.WarningOrange.copy(alpha = if (isDark) 0.2f else 0.15f),
            SubscriptionColors.WarningOrange
        )
        SubscriptionStatus.EXPIRED -> Triple(
            stringResource(R.string.status_expired),
            SubscriptionColors.ErrorRed.copy(alpha = if (isDark) 0.2f else 0.15f),
            SubscriptionColors.ErrorRed
        )
        SubscriptionStatus.NONE -> Triple(
            stringResource(R.string.status_free),
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusInfoBox(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}

@Composable
private fun getStatusDisplayText(status: SubscriptionStatus, isPremium: Boolean): String {
    // Status text should reflect the SUBSCRIPTION STATUS, not access level
    // A user with PAUSED subscription should see "Subscription paused", not "Upgrade to unlock"
    return when (status) {
        SubscriptionStatus.ACTIVE -> stringResource(R.string.subscription_status_active)
        SubscriptionStatus.GRACE_PERIOD -> stringResource(R.string.subscription_status_grace)
        SubscriptionStatus.PAUSED -> stringResource(R.string.subscription_status_paused)
        SubscriptionStatus.ACCOUNT_HOLD -> stringResource(R.string.subscription_status_billing)
        SubscriptionStatus.CANCELLED -> stringResource(R.string.subscription_status_cancelled)
        SubscriptionStatus.EXPIRED -> stringResource(R.string.subscription_status_expired)
        SubscriptionStatus.NONE -> stringResource(R.string.subscription_status_free)
    }
}

@Composable
private fun getStatusColor(status: SubscriptionStatus, isPremium: Boolean, isDark: Boolean): Color {
    // Status color should reflect the SUBSCRIPTION STATUS, not access level
    return when (status) {
        SubscriptionStatus.ACTIVE -> SubscriptionColors.SuccessGreen
        SubscriptionStatus.GRACE_PERIOD -> SubscriptionColors.WarningOrange
        SubscriptionStatus.PAUSED -> SubscriptionColors.WarningOrange
        SubscriptionStatus.ACCOUNT_HOLD -> SubscriptionColors.ErrorRed
        SubscriptionStatus.CANCELLED -> SubscriptionColors.WarningOrange
        SubscriptionStatus.EXPIRED -> SubscriptionColors.ErrorRed
        SubscriptionStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Adaptive dialog for subscription status notifications
 * Handles: GRACE_PERIOD, ACCOUNT_HOLD, PAUSED, CANCELLED
 * Works on phones, tablets, foldables, and TV
 */
@Composable
fun BillingIssueDialog(
    status: SubscriptionStatus,
    subscriptionDetails: SubscriptionDetails?,
    onDismiss: () -> Unit,
    onManageSubscription: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isLandscape = screenWidth > screenHeight
            val isWideScreen = screenWidth > 600.dp

            // Adaptive dialog width
            val dialogWidth = when {
                isWideScreen -> 480.dp
                isLandscape -> 400.dp
                else -> screenWidth - 32.dp
            }

            Surface(
                modifier = Modifier
                    .widthIn(max = dialogWidth)
                    .heightIn(max = screenHeight * 0.9f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close button
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Animated icon
                    val infiniteTransition = rememberInfiniteTransition(label = "status")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = EaseInOut),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "iconScale"
                    )

                    val (icon, iconColor, iconBg) = when (status) {
                        SubscriptionStatus.GRACE_PERIOD -> Triple(
                            Icons.Default.Warning,
                            SubscriptionColors.WarningOrange,
                            SubscriptionColors.WarningOrange.copy(alpha = 0.15f)
                        )
                        SubscriptionStatus.ACCOUNT_HOLD -> Triple(
                            Icons.Default.CreditCardOff,
                            SubscriptionColors.ErrorRed,
                            SubscriptionColors.ErrorRed.copy(alpha = 0.15f)
                        )
                        SubscriptionStatus.PAUSED -> Triple(
                            Icons.Default.Pause,
                            SubscriptionColors.WarningOrange,
                            SubscriptionColors.WarningOrange.copy(alpha = 0.15f)
                        )
                        SubscriptionStatus.CANCELLED -> Triple(
                            Icons.Default.Cancel,
                            SubscriptionColors.WarningOrange,
                            SubscriptionColors.WarningOrange.copy(alpha = 0.15f)
                        )
                        else -> Triple(
                            Icons.Default.Warning,
                            SubscriptionColors.WarningOrange,
                            SubscriptionColors.WarningOrange.copy(alpha = 0.15f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scale)
                            .background(color = iconBg, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title
                    Text(
                        text = when (status) {
                            SubscriptionStatus.GRACE_PERIOD -> stringResource(R.string.billing_issue_title_grace)
                            SubscriptionStatus.ACCOUNT_HOLD -> stringResource(R.string.billing_issue_title_expired)
                            SubscriptionStatus.PAUSED -> stringResource(R.string.subscription_paused_title)
                            SubscriptionStatus.CANCELLED -> stringResource(R.string.subscription_cancelled_title)
                            else -> stringResource(R.string.billing_issue_title_grace)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    Text(
                        text = when (status) {
                            SubscriptionStatus.GRACE_PERIOD -> stringResource(R.string.billing_issue_desc_grace)
                            SubscriptionStatus.ACCOUNT_HOLD -> stringResource(R.string.billing_issue_desc_expired)
                            SubscriptionStatus.PAUSED -> stringResource(R.string.subscription_paused_desc)
                            SubscriptionStatus.CANCELLED -> stringResource(R.string.subscription_cancelled_desc)
                            else -> stringResource(R.string.billing_issue_desc_grace)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    // Info box for specific statuses
                    when (status) {
                        SubscriptionStatus.GRACE_PERIOD -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            StatusInfoBox(
                                icon = Icons.Default.Warning,
                                text = stringResource(R.string.billing_issue_access_continues),
                                backgroundColor = SubscriptionColors.WarningOrange.copy(alpha = 0.1f),
                                textColor = SubscriptionColors.WarningOrange
                            )
                        }
                        SubscriptionStatus.CANCELLED -> {
                            subscriptionDetails?.expirationDate?.let { expDate ->
                                Spacer(modifier = Modifier.height(16.dp))
                                StatusInfoBox(
                                    icon = Icons.Default.CalendarMonth,
                                    text = stringResource(R.string.subscription_access_until_date, formatDate(expDate)),
                                    backgroundColor = SubscriptionColors.WarningOrange.copy(alpha = 0.1f),
                                    textColor = SubscriptionColors.WarningOrange
                                )
                            }
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary action button
                    Button(
                        onClick = onManageSubscription,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (status) {
                                SubscriptionStatus.ACCOUNT_HOLD -> SubscriptionColors.ErrorRed
                                else -> SubscriptionColors.WarningOrange
                            }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (status) {
                                SubscriptionStatus.PAUSED -> stringResource(R.string.resume_subscription)
                                SubscriptionStatus.CANCELLED -> stringResource(R.string.resubscribe)
                                else -> stringResource(R.string.billing_issue_update_button)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary action - Dismiss
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.billing_issue_dismiss),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Popup shown when user tries to access a premium feature while their subscription is on hold.
 * This is different from the Paywall - it prompts the user to fix their payment method.
 */
@Composable
fun AccountHoldPopup(
    visible: Boolean,
    managementUrl: String?,
    onDismiss: () -> Unit,
    onFixPayment: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight
                val isLandscape = screenWidth > screenHeight
                val isWideScreen = screenWidth > 600.dp

                // Adaptive dialog width
                val dialogWidth = when {
                    isWideScreen -> 420.dp
                    isLandscape -> 380.dp
                    else -> screenWidth - 32.dp
                }

                // Floating animation for the card
                val infiniteTransition = rememberInfiniteTransition(label = "float")
                val floatOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "floatOffset"
                )

                Surface(
                    modifier = Modifier
                        .widthIn(max = dialogWidth)
                        .heightIn(max = screenHeight * 0.85f),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Close button
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Animated icon
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "iconScale"
                        )

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .scale(scale)
                                .background(
                                    color = SubscriptionColors.ErrorRed.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCardOff,
                                contentDescription = null,
                                tint = SubscriptionColors.ErrorRed,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Title
                        Text(
                            text = stringResource(R.string.account_hold_feature_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        Text(
                            text = stringResource(R.string.account_hold_feature_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Fix Payment button
                        Button(
                            onClick = {
                                onDismiss()
                                onFixPayment()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SubscriptionColors.ErrorRed
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.account_hold_feature_button),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dismiss button
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.billing_issue_dismiss),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Open Google Play subscription management
 */
fun openSubscriptionManagement(context: android.content.Context, managementUrl: String?) {
    val url = managementUrl ?: "https://play.google.com/store/account/subscriptions"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to Play Store subscriptions page
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
        context.startActivity(fallbackIntent)
    }
}
