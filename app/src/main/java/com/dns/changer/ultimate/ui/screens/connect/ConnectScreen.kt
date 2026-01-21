package com.dns.changer.ultimate.ui.screens.connect

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.LocalIndication
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.ui.theme.AdaptiveLayoutConfig
import com.dns.changer.ultimate.ui.theme.WindowSize
import com.dns.changer.ultimate.ui.theme.isAndroidTv
import com.dns.changer.ultimate.ui.theme.tvFocusable
import com.dns.changer.ultimate.ui.viewmodel.MainViewModel

@Composable
fun ConnectScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onRequestVpnPermission: (android.content.Intent) -> Unit,
    adaptiveConfig: AdaptiveLayoutConfig,
    isPremium: Boolean = false,
    onShowPremiumGate: (title: String, description: String, onUnlock: () -> Unit) -> Unit = { _, _, _ -> },
    onShowPaywall: () -> Unit = {},
    onNavigateToSpeedTest: () -> Unit = {},
    onConnectWithAd: () -> Unit = {},
    onDisconnectWithAd: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var showServerPicker by remember { mutableStateOf(false) }
    var showCustomCategory by remember { mutableStateOf(false) }

    // TV Focus Management
    val isTv = isAndroidTv()
    val powerButtonFocusRequester = remember { FocusRequester() }

    // Request focus on power button when on TV
    LaunchedEffect(isTv) {
        if (isTv) {
            delay(300) // Wait for UI to settle
            try {
                powerButtonFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Focus request may fail if not yet attached
            }
        }
    }

    // Check if we're in a transitioning state where we should hide some UI
    val isTransitioning = connectionState is ConnectionState.Connecting ||
        connectionState is ConnectionState.Disconnecting ||
        connectionState is ConnectionState.Switching

    // Determine power button size based on window size and orientation
    val powerButtonSize = when {
        adaptiveConfig.isCompactLandscape -> 140.dp // Smaller for phone landscape
        adaptiveConfig.windowSize == WindowSize.COMPACT -> 200.dp
        adaptiveConfig.windowSize == WindowSize.MEDIUM -> 220.dp
        else -> 240.dp
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Use horizontal layout for tablets/foldables (EXPANDED) or phone landscape
        if (adaptiveConfig.useHorizontalLayout) {
            // Horizontal layout for large tablets and phone landscape
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = adaptiveConfig.horizontalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Power Button and Status
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PowerButton(
                        connectionState = connectionState,
                        isPreparing = false,
                        size = powerButtonSize,
                        focusRequester = powerButtonFocusRequester,
                        onClick = {
                            when (connectionState) {
                                is ConnectionState.Connected -> onDisconnectWithAd()
                                is ConnectionState.Disconnected -> onConnectWithAd()
                                else -> {}
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    StatusText(
                        connectionState = connectionState,
                        isPreparing = false,
                        onConnect = null // Disable click during expanded layout
                    )
                }

                // Right side: Server Selection Card - hide during preparing/transitioning
                if (!isTransitioning) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ServerSelectionCard(
                            server = uiState.selectedServer,
                            isConnected = connectionState is ConnectionState.Connected,
                            onClick = { showServerPicker = true },
                            maxWidth = 400.dp,
                            isTv = isTv
                        )
                    }
                } else {
                    // Empty space to maintain layout
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            // Vertical layout for compact and medium screens (phones and portrait tablets)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = adaptiveConfig.contentMaxWidth)
                        .padding(horizontal = adaptiveConfig.horizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Power Button
                    PowerButton(
                        connectionState = connectionState,
                        isPreparing = false,
                        size = powerButtonSize,
                        focusRequester = powerButtonFocusRequester,
                        onClick = {
                            when (connectionState) {
                                is ConnectionState.Connected -> onDisconnectWithAd()
                                is ConnectionState.Disconnected -> onConnectWithAd()
                                else -> {}
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Status Text
                    StatusText(
                        connectionState = connectionState,
                        isPreparing = false,
                        onConnect = if (!isTransitioning) {
                            { onConnectWithAd() }
                        } else null
                    )

                    // Server Selection Card - hide during preparing/transitioning
                    if (!isTransitioning) {
                        Spacer(modifier = Modifier.height(40.dp))

                        ServerSelectionCard(
                            server = uiState.selectedServer,
                            isConnected = connectionState is ConnectionState.Connected,
                            onClick = { showServerPicker = true },
                            isTv = isTv
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Premium gate strings for custom DNS
    val customDnsTitle = stringResource(R.string.unlock_custom_dns)
    val customDnsDescription = stringResource(R.string.custom_dns_premium_description)

    // Server Picker Dialog
    if (showServerPicker) {
        DnsPickerDialog(
            servers = uiState.servers,
            selectedServer = uiState.selectedServer,
            onServerSelected = { server ->
                viewModel.selectServer(server)
            },
            onAddCustomDns = {
                // Check if free user already has 1 custom DNS
                val customDnsCount = uiState.servers[DnsCategory.CUSTOM]?.size ?: 0
                if (!isPremium && customDnsCount >= 1) {
                    // Show premium gate with ads option
                    onShowPremiumGate(customDnsTitle, customDnsDescription) {
                        viewModel.showAddCustomDns()
                    }
                } else {
                    viewModel.showAddCustomDns()
                }
            },
            onDeleteCustomDns = { serverId ->
                viewModel.removeCustomDns(serverId)
            },
            onDismiss = {
                showServerPicker = false
                showCustomCategory = false
            },
            onFindFastest = onNavigateToSpeedTest,
            initialCategory = if (showCustomCategory) DnsCategory.CUSTOM else null
        )
    }

    // Custom DNS Dialog
    if (uiState.showCustomDnsDialog) {
        AddCustomDnsDialog(
            onDismiss = { viewModel.hideAddCustomDns() },
            onConfirm = { name, primary, secondary, isDoH, dohUrl ->
                viewModel.addCustomDns(name, primary, secondary, isDoH, dohUrl)
                // After adding custom DNS, show picker with Custom category selected
                showCustomCategory = true
            },
            isPremium = isPremium,
            onShowPremiumGate = {
                // DoH is a premium-only feature (no watch ad option)
                // Show paywall directly
                onShowPaywall()
            }
        )
    }
}

@Composable
private fun PowerButton(
    connectionState: ConnectionState,
    isPreparing: Boolean = false,
    onClick: () -> Unit,
    size: Dp = 200.dp,
    focusRequester: FocusRequester? = null
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting
    val isDisconnecting = connectionState is ConnectionState.Disconnecting
    val isSwitching = connectionState is ConnectionState.Switching
    val isError = connectionState is ConnectionState.Error
    val isTransitioning = isConnecting || isDisconnecting || isSwitching || isPreparing

    val infiniteTransition = rememberInfiniteTransition(label = "powerButton")

    // Calculate inner button size (85% of outer size)
    val innerButtonSize = size * 0.85f
    val iconSize = size * 0.28f

    // Glow ring rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse animation for connecting state
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val colorScheme = MaterialTheme.colorScheme
    val tertiaryColor = colorScheme.tertiary
    val secondaryColor = colorScheme.secondary
    val primaryColor = colorScheme.primary
    val errorColor = colorScheme.error

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> tertiaryColor
            isError -> errorColor
            isTransitioning -> secondaryColor
            else -> primaryColor
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "buttonColor"
    )

    // Use proper Material You on* colors for content - these adapt to dynamic color palettes
    val contentColor by animateColorAsState(
        targetValue = when {
            isConnected -> colorScheme.onTertiary
            isError -> colorScheme.onError
            isTransitioning -> colorScheme.onSecondary
            else -> colorScheme.onPrimary
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "contentColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isTransitioning) pulseScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // TV Focus handling
    val isTv = isAndroidTv()

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(size)
                .rotate(rotation)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = 0f),
                            buttonColor.copy(alpha = 0.25f),
                            buttonColor.copy(alpha = 0f),
                            buttonColor.copy(alpha = 0.25f),
                            buttonColor.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main button with Material 3 elevation and TV focus
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        // High-visibility focus for TV - use theme inverse surface for proper Material You adaptation
        val focusBorderColor = MaterialTheme.colorScheme.inverseSurface
        val tvFocusScale = if (isTv && isFocused) 1.05f else 1f

        Surface(
            modifier = Modifier
                .size(innerButtonSize)
                .scale(scale * tvFocusScale)
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (isTv && isFocused) {
                        Modifier
                            .shadow(
                                elevation = 16.dp,
                                shape = CircleShape,
                                ambientColor = focusBorderColor.copy(alpha = 0.5f),
                                spotColor = focusBorderColor.copy(alpha = 0.5f)
                            )
                            .border(
                                width = 5.dp,
                                color = focusBorderColor,
                                shape = CircleShape
                            )
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // Surface has its own indication
                    enabled = !isTransitioning,
                    onClick = onClick
                ),
            shape = CircleShape,
            color = buttonColor,
            shadowElevation = if (isConnected) 16.dp else 6.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                contentColor.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isTransitioning && !isError) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(iconSize),
                        color = contentColor,
                        strokeWidth = 3.dp,
                        trackColor = contentColor.copy(alpha = 0.2f)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = if (isConnected) "Disconnect" else "Connect",
                        tint = contentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}


@Composable
private fun StatusText(
    connectionState: ConnectionState,
    isPreparing: Boolean = false,
    onConnect: (() -> Unit)? = null
) {
    val statusText = when {
        isPreparing -> "Preparing..."
        connectionState is ConnectionState.Connected -> "Connected"
        connectionState is ConnectionState.Connecting -> "Connecting..."
        connectionState is ConnectionState.Disconnecting -> "Disconnecting..."
        connectionState is ConnectionState.Switching -> "Switching..."
        connectionState is ConnectionState.Error -> connectionState.message
        connectionState is ConnectionState.Disconnected -> "Tap to connect"
        else -> ""
    }

    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val statusColor by animateColorAsState(
        targetValue = when {
            isPreparing -> secondaryColor
            connectionState is ConnectionState.Connected -> tertiaryColor
            connectionState is ConnectionState.Error -> MaterialTheme.colorScheme.error
            connectionState is ConnectionState.Connecting ||
            connectionState is ConnectionState.Disconnecting ||
            connectionState is ConnectionState.Switching -> secondaryColor
            connectionState is ConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "statusColor"
    )

    val isClickable = connectionState is ConnectionState.Disconnected && onConnect != null && !isPreparing

    Text(
        text = statusText,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        color = statusColor,
        textAlign = TextAlign.Center,
        modifier = if (isClickable) {
            Modifier.clickable(onClick = onConnect!!)
        } else {
            Modifier
        }
    )
}

@Composable
private fun ServerSelectionCard(
    server: DnsServer?,
    isConnected: Boolean,
    onClick: () -> Unit,
    maxWidth: Dp = Dp.Unspecified,
    isTv: Boolean = false
) {
    val isDarkTheme = isAppInDarkTheme()
    val categoryColor = remember(server, isDarkTheme) {
        if (server != null) CategoryColors.forCategory(server.category, isDarkTheme)
        else null
    }

    // TV Focus handling with high-visibility indication
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val cardShape = RoundedCornerShape(24.dp)

    // High-visibility focus color - use theme inverse surface for proper Material You adaptation
    val focusBorderColor = MaterialTheme.colorScheme.inverseSurface

    // Scale on focus for TV
    val focusScale = if (isTv && isFocused) 1.03f else 1f

    val modifier = if (maxWidth != Dp.Unspecified) {
        Modifier.widthIn(max = maxWidth).fillMaxWidth()
    } else {
        Modifier.fillMaxWidth()
    }

    Surface(
        modifier = modifier
            .scale(focusScale)
            .then(
                if (isTv && isFocused) {
                    Modifier
                        .shadow(
                            elevation = 12.dp,
                            shape = cardShape,
                            ambientColor = focusBorderColor.copy(alpha = 0.4f),
                            spotColor = focusBorderColor.copy(alpha = 0.4f)
                        )
                        .border(
                            width = 4.dp,
                            color = focusBorderColor,
                            shape = cardShape
                        )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = cardShape,
        color = if (isConnected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Server Icon with Material 3 container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isConnected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            categoryColor?.copy(alpha = 0.15f)
                                ?: MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = server?.category?.icon ?: Icons.Rounded.Dns,
                    contentDescription = null,
                    tint = if (isConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        categoryColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server?.name ?: stringResource(R.string.select_server),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isConnected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (server != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (server.isDoH && !server.dohUrl.isNullOrBlank()) {
                            DnsChip(server.dohUrl, isConnected)
                        } else {
                            DnsChip(server.primaryDns, isConnected)
                            DnsChip(server.secondaryDns, isConnected)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to choose a DNS server",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (isConnected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DnsChip(
    ip: String,
    isConnected: Boolean
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isConnected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Text(
            text = ip,
            style = MaterialTheme.typography.labelSmall,
            color = if (isConnected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
