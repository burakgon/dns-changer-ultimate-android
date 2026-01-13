package com.dns.changer.ultimate.ui.screens.connect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DnsPickerDialog(
    servers: Map<DnsCategory, List<DnsServer>>,
    selectedServer: DnsServer?,
    onServerSelected: (DnsServer) -> Unit,
    onAddCustomDns: () -> Unit,
    onDeleteCustomDns: (String) -> Unit,
    onDismiss: () -> Unit,
    onFindFastest: () -> Unit,
    initialCategory: DnsCategory? = null
) {
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var searchQuery by remember { mutableStateOf("") }
    val isDarkTheme = isAppInDarkTheme()

    // Filter servers by selected category and search query
    val filteredServers = remember(servers, selectedCategory, searchQuery) {
        val categoryFiltered = if (selectedCategory == null) {
            servers.values.flatten()
        } else {
            servers[selectedCategory] ?: emptyList()
        }

        if (searchQuery.isBlank()) {
            categoryFiltered
        } else {
            val query = searchQuery.lowercase().trim()
            categoryFiltered.filter { server ->
                server.name.lowercase().contains(query) ||
                server.description.lowercase().contains(query) ||
                server.primaryDns.lowercase().contains(query) ||
                server.secondaryDns.lowercase().contains(query) ||
                (server.dohUrl?.lowercase()?.contains(query) == true)
            }
        }
    }

    // Filter servers map for grouped view
    val filteredServersMap = remember(servers, searchQuery) {
        if (searchQuery.isBlank()) {
            servers
        } else {
            val query = searchQuery.lowercase().trim()
            servers.mapValues { (_, serverList) ->
                serverList.filter { server ->
                    server.name.lowercase().contains(query) ||
                    server.description.lowercase().contains(query) ||
                    server.primaryDns.lowercase().contains(query) ||
                    server.secondaryDns.lowercase().contains(query) ||
                    (server.dohUrl?.lowercase()?.contains(query) == true)
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    // Get screen height to limit dialog size
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp * 0.8f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(0.95f)
                .heightIn(max = maxDialogHeight),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.select_server),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row {
                        IconButton(onClick = onAddCustomDns) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add Custom DNS",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Find Fastest Button
                Button(
                    onClick = {
                        onDismiss()
                        onFindFastest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.find_fastest),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Category Filter Chips
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "All" chip
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") },
                        leadingIcon = if (selectedCategory == null) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    // Category chips
                    DnsCategory.entries.forEach { category ->
                        val categoryColor = CategoryColors.forCategory(category, isDarkTheme)
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedCategory == category)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else categoryColor
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = categoryColor.copy(alpha = 0.15f),
                                selectedLabelColor = categoryColor,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Server List with group headers
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (selectedCategory == null) {
                        // Show all with group headers (Custom first, then others)
                        val orderedCategories = listOf(DnsCategory.CUSTOM) + DnsCategory.entries.filter { it != DnsCategory.CUSTOM }
                        orderedCategories.forEach { category ->
                            val serversInCategory = filteredServersMap[category] ?: emptyList()
                            if (serversInCategory.isNotEmpty()) {
                                // Category Header
                                item(key = "header_${category.name}") {
                                    CategoryHeader(category = category)
                                }
                                // Servers in this category
                                items(
                                    items = serversInCategory,
                                    key = { it.id }
                                ) { server ->
                                    DnsServerCard(
                                        server = server,
                                        isSelected = selectedServer?.id == server.id,
                                        onClick = {
                                            onServerSelected(server)
                                            onDismiss()
                                        },
                                        onDelete = if (server.isCustom) {
                                            { onDeleteCustomDns(server.id) }
                                        } else null,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Filtered - no headers needed
                        items(
                            items = filteredServers,
                            key = { it.id }
                        ) { server ->
                            DnsServerCard(
                                server = server,
                                isSelected = selectedServer?.id == server.id,
                                onClick = {
                                    onServerSelected(server)
                                    onDismiss()
                                },
                                onDelete = if (server.isCustom) {
                                    { onDeleteCustomDns(server.id) }
                                } else null,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Search Box at bottom
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_dns),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: DnsCategory) {
    val isDarkTheme = isAppInDarkTheme()
    val categoryColor = remember(category, isDarkTheme) { CategoryColors.forCategory(category, isDarkTheme) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun DnsServerCard(
    server: DnsServer,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Use Material theme colors for proper contrast in all color schemes
    val iconBackgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val iconTintColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon - use solid colors for proper contrast
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconBackgroundColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = server.category.icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Server Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = server.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // DNS IPs or DoH URL as chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (server.isDoH && !server.dohUrl.isNullOrBlank()) {
                        DnsIpChip(
                            ip = server.dohUrl,
                            isSelected = isSelected
                        )
                    } else {
                        DnsIpChip(
                            ip = server.primaryDns,
                            isSelected = isSelected
                        )
                        DnsIpChip(
                            ip = server.secondaryDns,
                            isSelected = isSelected
                        )
                    }
                }
            }

            // Delete button for custom servers
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Selection indicator
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DnsIpChip(
    ip: String,
    isSelected: Boolean
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Text(
            text = ip,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
