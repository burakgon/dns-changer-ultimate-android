package com.dns.changer.ultimate.ui.screens.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer

// Category colors - theme-aware for good contrast in both light and dark modes
@Stable
object CategoryColors {
    // Light mode colors (darker for contrast on light backgrounds)
    private val speedLight = Color(0xFFE65100) // Deep Orange 800
    private val privacyLight = Color(0xFF1565C0) // Blue 800
    private val securityLight = Color(0xFF2E7D32) // Green 800
    private val adBlockingLight = Color(0xFFC62828) // Red 800
    private val familyLight = Color(0xFF6A1B9A) // Purple 800
    private val customLight = Color(0xFF00838F) // Cyan 800

    // Dark mode colors (lighter/more vibrant for contrast on dark backgrounds)
    private val speedDark = Color(0xFFFFB74D) // Orange 300
    private val privacyDark = Color(0xFF64B5F6) // Blue 300
    private val securityDark = Color(0xFF81C784) // Green 300
    private val adBlockingDark = Color(0xFFE57373) // Red 300
    private val familyDark = Color(0xFFBA68C8) // Purple 300
    private val customDark = Color(0xFF4DD0E1) // Cyan 300

    fun forCategory(category: DnsCategory, isDarkTheme: Boolean): Color = when (category) {
        DnsCategory.SPEED -> if (isDarkTheme) speedDark else speedLight
        DnsCategory.PRIVACY -> if (isDarkTheme) privacyDark else privacyLight
        DnsCategory.SECURITY -> if (isDarkTheme) securityDark else securityLight
        DnsCategory.AD_BLOCKING -> if (isDarkTheme) adBlockingDark else adBlockingLight
        DnsCategory.FAMILY -> if (isDarkTheme) familyDark else familyLight
        DnsCategory.CUSTOM -> if (isDarkTheme) customDark else customLight
    }
}

// Helper to determine if current theme is dark based on surface color luminance
@Composable
fun isAppInDarkTheme(): Boolean {
    val surface = MaterialTheme.colorScheme.surface
    // Calculate luminance - if less than 0.5, it's dark
    return (0.299f * surface.red + 0.587f * surface.green + 0.114f * surface.blue) < 0.5f
}

// Sealed class for list items to enable proper diffing
private sealed class ServerListItem {
    data class Header(val category: DnsCategory) : ServerListItem()
    data class Server(val server: DnsServer) : ServerListItem()
}

@Composable
fun ServerPickerSheet(
    servers: Map<DnsCategory, List<DnsServer>>,
    selectedServer: DnsServer?,
    onServerSelected: (DnsServer) -> Unit,
    onAddCustomDns: () -> Unit,
    onDismiss: () -> Unit
) {
    // Pre-compute the flat list for better LazyColumn performance
    val listItems = remember(servers) {
        buildList {
            DnsCategory.entries.forEach { category ->
                val serversInCategory = servers[category] ?: emptyList()
                if (serversInCategory.isNotEmpty()) {
                    add(ServerListItem.Header(category))
                    serversInCategory.forEach { server ->
                        add(ServerListItem.Server(server))
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header with Material 3 styling
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onAddCustomDns,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Custom DNS",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = stringResource(R.string.select_server),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.done),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Optimized Server List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(
                items = listItems,
                key = { item ->
                    when (item) {
                        is ServerListItem.Header -> "header_${item.category.name}"
                        is ServerListItem.Server -> item.server.id
                    }
                },
                contentType = { item ->
                    when (item) {
                        is ServerListItem.Header -> "header"
                        is ServerListItem.Server -> "server"
                    }
                }
            ) { item ->
                when (item) {
                    is ServerListItem.Header -> {
                        CategorySectionHeader(category = item.category)
                    }
                    is ServerListItem.Server -> {
                        ServerRow(
                            server = item.server,
                            isSelected = selectedServer?.id == item.server.id,
                            onClick = { onServerSelected(item.server) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(category: DnsCategory) {
    val isDarkTheme = isAppInDarkTheme()
    val categoryColor = remember(category, isDarkTheme) { CategoryColors.forCategory(category, isDarkTheme) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ServerRow(
    server: DnsServer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Use stable remembered colors - no animation during scroll
    val isDarkTheme = isAppInDarkTheme()
    val categoryColor = remember(server.category, isDarkTheme) { CategoryColors.forCategory(server.category, isDarkTheme) }

    // Simple conditional color without animation for better scroll performance
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with Material 3 container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = categoryColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = server.category.icon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (server.description.isNotEmpty()) {
                    Text(
                        text = server.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${server.primaryDns} • ${server.secondaryDns}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
