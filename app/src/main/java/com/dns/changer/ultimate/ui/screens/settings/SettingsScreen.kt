package com.dns.changer.ultimate.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ArrowDropDown
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dns.changer.ultimate.BuildConfig
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.SubscriptionDetails
import com.dns.changer.ultimate.data.model.SubscriptionStatus
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.ui.components.BillingIssueDialog
import com.dns.changer.ultimate.ui.components.SubscriptionStatusCard
import com.dns.changer.ultimate.ui.components.openSubscriptionManagement
import com.dns.changer.ultimate.ui.screens.paywall.PaywallScreen
import com.dns.changer.ultimate.ui.theme.AdaptiveLayoutConfig
import com.revenuecat.purchases.models.StoreProduct
import com.dns.changer.ultimate.ui.theme.DnsShapes
import com.dns.changer.ultimate.ui.theme.isAndroidTv
import kotlinx.coroutines.launch

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Composable
fun SettingsScreen(
    preferences: DnsPreferences,
    onThemeChanged: (ThemeMode) -> Unit = {},
    products: Map<String, StoreProduct> = emptyMap(),
    isLoadingPurchase: Boolean = false,
    onPurchase: (StoreProduct) -> Unit = {},
    onRestorePurchases: () -> Unit = {},
    adaptiveConfig: AdaptiveLayoutConfig,
    subscriptionStatus: SubscriptionStatus = SubscriptionStatus.NONE,
    subscriptionDetails: SubscriptionDetails? = null,
    isPremium: Boolean = false // Calculated premium access (considers subscription status)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedTheme by preferences.themeMode.collectAsState(initial = "SYSTEM")
    val selectedTheme = ThemeMode.valueOf(savedTheme)
    // isPremium is now passed as parameter (calculated hasAccess value)
    // debugToggleValue is the raw toggle state for the debug switch
    val debugToggleValue by preferences.isPremium.collectAsState(initial = false)
    val startOnBoot by preferences.startOnBoot.collectAsState(initial = false)
    var showPaywall by remember { mutableStateOf(false) }
    var showBillingIssueDialog by remember { mutableStateOf(false) }

    // TV Focus handling
    val isTv = isAndroidTv()

    // Show billing issue dialog automatically for grace period or billing issues
    LaunchedEffect(subscriptionStatus) {
        if (subscriptionStatus == SubscriptionStatus.GRACE_PERIOD ||
            subscriptionStatus == SubscriptionStatus.BILLING_ISSUE) {
            showBillingIssueDialog = true
        }
    }

    // Center content on larger screens
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = adaptiveConfig.contentMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = adaptiveConfig.horizontalPadding)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = stringResource(R.string.settings_tab),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Section
            SectionHeader(title = stringResource(R.string.appearance))

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DnsShapes.Card,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.theme),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ThemeOption(
                            icon = Icons.Default.LightMode,
                            label = stringResource(R.string.light),
                            isSelected = selectedTheme == ThemeMode.LIGHT,
                            onClick = {
                                scope.launch { preferences.setThemeMode("LIGHT") }
                                onThemeChanged(ThemeMode.LIGHT)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ThemeOption(
                            icon = Icons.Default.DarkMode,
                            label = stringResource(R.string.dark),
                            isSelected = selectedTheme == ThemeMode.DARK,
                            onClick = {
                                scope.launch { preferences.setThemeMode("DARK") }
                                onThemeChanged(ThemeMode.DARK)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ThemeOption(
                            icon = Icons.Default.SettingsSuggest,
                            label = stringResource(R.string.system),
                            isSelected = selectedTheme == ThemeMode.SYSTEM,
                            onClick = {
                                scope.launch { preferences.setThemeMode("SYSTEM") }
                                onThemeChanged(ThemeMode.SYSTEM)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Subscription Status Section
            SectionHeader(title = stringResource(R.string.subscription_status))

            Spacer(modifier = Modifier.height(12.dp))

            SubscriptionStatusCard(
                subscriptionStatus = subscriptionStatus,
                subscriptionDetails = subscriptionDetails,
                isPremium = isPremium,
                onManageSubscription = {
                    openSubscriptionManagement(context, subscriptionDetails?.managementUrl)
                },
                onShowPaywall = { showPaywall = true }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Features Section (Premium)
            SectionHeader(title = stringResource(R.string.features))

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DnsShapes.Card,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    // Start on Boot - with TV focus support
                    val startOnBootInteraction = remember { MutableInteractionSource() }
                    val startOnBootFocused by startOnBootInteraction.collectIsFocusedAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable(interactionSource = startOnBootInteraction)
                            .then(
                                if (isTv && startOnBootFocused) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                if (isPremium) {
                                    scope.launch { preferences.setStartOnBoot(!startOnBoot) }
                                } else {
                                    showPaywall = true
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.start_on_boot),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!isPremium) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        PremiumBadge()
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.start_on_boot_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = startOnBoot && isPremium,
                            onCheckedChange = { enabled ->
                                if (isPremium) {
                                    scope.launch { preferences.setStartOnBoot(enabled) }
                                } else {
                                    showPaywall = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    // Request Feature (Premium only can use, but visible to all)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Request Feature - with TV focus support
                    val requestFeatureInteraction = remember { MutableInteractionSource() }
                    val requestFeatureFocused by requestFeatureInteraction.collectIsFocusedAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable(interactionSource = requestFeatureInteraction)
                            .then(
                                if (isTv && requestFeatureFocused) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                if (isPremium) {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("burakgon1@gmail.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, "DNS Changer Feature Request")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Hi,\n\nI'd like to request the following feature:\n\n[Describe your feature idea here]\n\n---\nApp Version: ${BuildConfig.VERSION_NAME}\nPremium User: Yes"
                                        )
                                    }
                                    context.startActivity(
                                        Intent.createChooser(emailIntent, "Send Feature Request")
                                    )
                                } else {
                                    showPaywall = true
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.request_feature),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!isPremium) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    PremiumBadge()
                                }
                            }
                            Text(
                                text = stringResource(R.string.request_feature_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // About Section
            SectionHeader(title = stringResource(R.string.about))

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DnsShapes.Card,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Build Type",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (BuildConfig.DEBUG) "Debug" else "Release",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (BuildConfig.DEBUG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Debug Section (only visible in debug builds)
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(32.dp))

                SectionHeader(title = "Debug")

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DnsShapes.Card,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Premium Status",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Toggle premium for testing",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = debugToggleValue,
                                onCheckedChange = { enabled ->
                                    scope.launch { preferences.setPremium(enabled) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.error,
                                    checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )

                        // Subscription Status Selector
                        val debugSubStatus by preferences.debugSubscriptionStatus.collectAsState(initial = "ACTIVE")
                        var statusDropdownExpanded by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Subscription Status",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Test different subscription states",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box {
                                Surface(
                                    modifier = Modifier
                                        .clickable { statusDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = debugSubStatus,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = statusDropdownExpanded,
                                    onDismissRequest = { statusDropdownExpanded = false }
                                ) {
                                    SubscriptionStatus.entries.forEach { status ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = status.name,
                                                    fontWeight = if (status.name == debugSubStatus) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                scope.launch {
                                                    preferences.setDebugSubscriptionStatus(status.name)
                                                }
                                                statusDropdownExpanded = false
                                            },
                                            trailingIcon = if (status.name == debugSubStatus) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Full-screen Paywall
        if (showPaywall) {
            Dialog(
                onDismissRequest = { showPaywall = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                PaywallScreen(
                    products = products,
                    isLoading = isLoadingPurchase,
                    onPurchase = { product ->
                        onPurchase(product)
                    },
                    onRestore = onRestorePurchases,
                    onDismiss = { showPaywall = false }
                )
            }
        }

        // Billing Issue Dialog (for grace period and billing issues)
        if (showBillingIssueDialog &&
            (subscriptionStatus == SubscriptionStatus.GRACE_PERIOD ||
             subscriptionStatus == SubscriptionStatus.BILLING_ISSUE)) {
            BillingIssueDialog(
                status = subscriptionStatus,
                subscriptionDetails = subscriptionDetails,
                onDismiss = { showBillingIssueDialog = false },
                onManageSubscription = {
                    showBillingIssueDialog = false
                    openSubscriptionManagement(context, subscriptionDetails?.managementUrl)
                }
            )
        }
    }
}

@Composable
private fun PremiumBadge() {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "PRO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTv = isAndroidTv()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        isTv && isFocused -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .focusable(interactionSource = interactionSource)
            .border(
                width = if (isSelected || (isTv && isFocused)) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
