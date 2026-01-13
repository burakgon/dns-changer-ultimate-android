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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.ui.viewmodel.MainViewModel

@Composable
fun ConnectScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onRequestVpnPermission: (android.content.Intent) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var showServerPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Power Button
            PowerButton(
                connectionState = connectionState,
                onClick = {
                    when (connectionState) {
                        is ConnectionState.Connected -> viewModel.disconnect()
                        is ConnectionState.Disconnected -> {
                            val vpnIntent = uiState.vpnPermissionIntent
                            if (vpnIntent != null) {
                                onRequestVpnPermission(vpnIntent)
                            } else {
                                viewModel.connect()
                            }
                        }
                        else -> {}
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Status Text
            StatusText(connectionState = connectionState)

            Spacer(modifier = Modifier.height(40.dp))

            // Server Selection Card
            ServerSelectionCard(
                server = uiState.selectedServer,
                isConnected = connectionState is ConnectionState.Connected,
                onClick = { showServerPicker = true }
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // Server Picker Dialog
    if (showServerPicker) {
        DnsPickerDialog(
            servers = uiState.servers,
            selectedServer = uiState.selectedServer,
            onServerSelected = { server ->
                viewModel.selectServer(server)
            },
            onAddCustomDns = {
                viewModel.showAddCustomDns()
            },
            onDeleteCustomDns = { serverId ->
                viewModel.removeCustomDns(serverId)
            },
            onDismiss = {
                showServerPicker = false
            }
        )
    }

    // Custom DNS Dialog
    if (uiState.showCustomDnsDialog) {
        AddCustomDnsDialog(
            onDismiss = { viewModel.hideAddCustomDns() },
            onConfirm = { name, primary, secondary ->
                viewModel.addCustomDns(name, primary, secondary)
            }
        )
    }
}

@Composable
private fun PowerButton(
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting
    val isDisconnecting = connectionState is ConnectionState.Disconnecting
    val isSwitching = connectionState is ConnectionState.Switching
    val isError = connectionState is ConnectionState.Error
    val isTransitioning = isConnecting || isDisconnecting || isSwitching

    val infiniteTransition = rememberInfiniteTransition(label = "powerButton")

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

    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> tertiaryColor
            isError -> MaterialTheme.colorScheme.error
            isTransitioning -> secondaryColor
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "buttonColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isTransitioning) pulseScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(200.dp)
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

        // Main button with Material 3 elevation
        Surface(
            modifier = Modifier
                .size(170.dp)
                .scale(scale),
            shape = CircleShape,
            color = buttonColor,
            shadowElevation = if (isConnected) 16.dp else 6.dp,
            tonalElevation = 0.dp,
            onClick = { if (!isTransitioning) onClick() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isTransitioning && !isError) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = if (isConnected) "Disconnect" else "Connect",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun StatusText(connectionState: ConnectionState) {
    val statusText = when (connectionState) {
        is ConnectionState.Connected -> "Connected"
        is ConnectionState.Connecting -> "Connecting..."
        is ConnectionState.Disconnecting -> "Disconnecting..."
        is ConnectionState.Switching -> "Switching..."
        is ConnectionState.Error -> connectionState.message
        is ConnectionState.Disconnected -> "Tap to connect"
    }

    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> tertiaryColor
            is ConnectionState.Error -> MaterialTheme.colorScheme.error
            is ConnectionState.Connecting, is ConnectionState.Disconnecting, is ConnectionState.Switching -> secondaryColor
            is ConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "statusColor"
    )

    Text(
        text = statusText,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        color = statusColor,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ServerSelectionCard(
    server: DnsServer?,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val isDarkTheme = isAppInDarkTheme()
    val categoryColor = remember(server, isDarkTheme) {
        if (server != null) CategoryColors.forCategory(server.category, isDarkTheme)
        else null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isConnected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = 2.dp,
        onClick = onClick
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DnsChip(server.primaryDns, isConnected)
                        DnsChip(server.secondaryDns, isConnected)
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
        shape = RoundedCornerShape(8.dp),
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
