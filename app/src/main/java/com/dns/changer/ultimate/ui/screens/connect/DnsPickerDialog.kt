package com.dns.changer.ultimate.ui.screens.connect

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.delay
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.ui.theme.contentColorFor
import com.dns.changer.ultimate.ui.theme.isAndroidTv

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
    val isTv = isAndroidTv()

    // Focus management for TV
    val findFastestFocusRequester = remember { FocusRequester() }

    // Request initial focus on Find Fastest button when on TV
    LaunchedEffect(isTv) {
        if (isTv) {
            delay(300)
            try {
                findFastestFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Focus request may fail if not yet attached
            }
        }
    }

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

    // Get screen dimensions for adaptive layout
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    // Compact landscape for phones - need more aggressive space saving
    val isCompactLandscape = isLandscape && screenHeightDp < 500

    // Adaptive sizing for different screen modes
    val isWideScreen = isLandscape || isTv || screenWidthDp >= 600
    val maxDialogWidth = when {
        isTv -> screenWidthDp.dp * 0.85f
        isLandscape -> screenWidthDp.dp * 0.9f
        screenWidthDp >= 840 -> 800.dp
        screenWidthDp >= 600 -> 700.dp
        else -> 600.dp
    }
    val maxDialogHeight = when {
        isTv -> screenHeightDp.dp * 0.9f
        isLandscape -> screenHeightDp.dp * 0.95f
        else -> (screenHeightDp * 0.8f).dp
    }
    val dialogFillWidth = if (isTv || isLandscape) 0.98f else 0.95f

    // Use grid for TV/landscape to show more servers
    val useGridLayout = isTv || (isLandscape && screenWidthDp >= 720)
    val gridColumns = when {
        screenWidthDp >= 1200 -> 3
        screenWidthDp >= 800 -> 2
        else -> 2
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = !isTv // Don't dismiss on click outside for TV
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = maxDialogWidth)
                .fillMaxWidth(dialogFillWidth)
                .heightIn(max = maxDialogHeight),
            shape = RoundedCornerShape(if (isTv) 16.dp else 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            if (isCompactLandscape) {
                // Two-column layout for compact landscape
                DnsPickerCompactLandscapeContent(
                    servers = servers,
                    selectedServer = selectedServer,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    filteredServers = filteredServers,
                    filteredServersMap = filteredServersMap,
                    onServerSelected = onServerSelected,
                    onAddCustomDns = onAddCustomDns,
                    onDeleteCustomDns = onDeleteCustomDns,
                    onDismiss = onDismiss,
                    onFindFastest = onFindFastest,
                    isTv = isTv,
                    isDarkTheme = isDarkTheme
                )
            } else {
                // Use BoxWithConstraints for adaptive layout based on actual available space
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Determine compact mode based on actual dialog width
                    // < 340dp = very compact, 340-480dp = compact, > 480dp = normal
                    val isVeryCompact = maxWidth < 340.dp
                    val isCompact = maxWidth < 480.dp

                    // Calculate available height for content to determine if we should show descriptions
                    // On shorter screens or when dialog is constrained, hide descriptions
                    val availableHeight = minOf(maxHeight, maxDialogHeight)
                    val showDescriptions = availableHeight > 500.dp && !isVeryCompact

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = if (isCompact) 16.dp else 24.dp,
                                    end = 8.dp,
                                    top = if (isCompact) 12.dp else 16.dp,
                                    bottom = if (isCompact) 4.dp else 8.dp
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.select_server),
                                style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row {
                                IconButton(onClick = onAddCustomDns) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Add Custom DNS",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

                        // Find Fastest Button with TV focus
                        val findFastestInteraction = remember { MutableInteractionSource() }
                        val findFastestFocused by findFastestInteraction.collectIsFocusedAsState()

                        Button(
                            onClick = {
                                onDismiss()
                                onFindFastest()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (isCompact) 12.dp else 20.dp,
                                    vertical = if (isCompact) 4.dp else 8.dp
                                )
                                .height(if (isCompact) 44.dp else 48.dp)
                                .focusRequester(findFastestFocusRequester)
                                .focusable(interactionSource = findFastestInteraction)
                                .then(
                                    if (isTv && findFastestFocused) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.onTertiary,
                                            shape = RoundedCornerShape(if (isCompact) 10.dp else 12.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            shape = RoundedCornerShape(if (isCompact) 10.dp else 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                            )
                            Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 8.dp))
                            Text(
                                text = stringResource(R.string.find_fastest),
                                style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Category Filter Chips - consistent small sizing across all devices
                        // Use labelSmall for compact text, slightly larger icons for touch targets
                        val chipTextStyle = MaterialTheme.typography.labelSmall
                        val chipIconSize = 14.dp

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // "All" chip - use Material You onPrimary for proper theme adaptation
                            val allChipBgColor = MaterialTheme.colorScheme.primary
                            val allChipContentColor = MaterialTheme.colorScheme.onPrimary
                            val isAllSelected = selectedCategory == null
                            FilterChip(
                                selected = isAllSelected,
                                onClick = { selectedCategory = null },
                                label = {
                                    Text(
                                        text = "All",
                                        style = chipTextStyle,
                                        color = if (isAllSelected) allChipContentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingIcon = if (isAllSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(chipIconSize),
                                            tint = allChipContentColor
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = allChipBgColor,
                                    selectedLabelColor = allChipContentColor,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            // Category chips
                            DnsCategory.entries.forEach { category ->
                                val categoryColor = CategoryColors.forCategory(category, isDarkTheme)
                                val isThisCategorySelected = selectedCategory == category
                                // Use Material You compatible content color for selected state
                                val selectedContentColor = contentColorFor(categoryColor)
                                FilterChip(
                                    selected = isThisCategorySelected,
                                    onClick = {
                                        selectedCategory = if (selectedCategory == category) null else category
                                    },
                                    label = {
                                        Text(
                                            text = category.displayName,
                                            style = chipTextStyle,
                                            color = if (isThisCategorySelected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(chipIconSize),
                                            tint = if (isThisCategorySelected) selectedContentColor else categoryColor
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = categoryColor,
                                        selectedLabelColor = selectedContentColor,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // Server List - use grid for TV/landscape, column for portrait
                        if (useGridLayout) {
                            // Grid layout for TV/landscape - shows more servers at once
                            val allServers = if (selectedCategory == null) {
                                filteredServersMap.values.flatten()
                            } else {
                                filteredServers
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridColumns),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                contentPadding = PaddingValues(
                                    horizontal = if (isCompactLandscape) 8.dp else 12.dp,
                                    vertical = if (isCompactLandscape) 4.dp else 8.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 6.dp else 8.dp),
                                verticalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 6.dp else 8.dp)
                            ) {
                                items(
                                    items = allServers,
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
                                        isCompact = true, // Always compact in grid
                                        showDescriptions = false, // No descriptions in grid for space
                                        isTv = isTv,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            // Column layout for portrait phones
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                contentPadding = PaddingValues(bottom = if (isCompact) 8.dp else 16.dp)
                            ) {
                                if (selectedCategory == null) {
                                    // Show all with group headers (Custom first, then others)
                                    val orderedCategories = listOf(DnsCategory.CUSTOM) + DnsCategory.entries.filter { it != DnsCategory.CUSTOM }
                                    orderedCategories.forEach { category ->
                                        val serversInCategory = filteredServersMap[category] ?: emptyList()
                                        if (serversInCategory.isNotEmpty()) {
                                            // Category Header
                                            item(key = "header_${category.name}") {
                                                CategoryHeader(category = category, isCompact = isCompact)
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
                                                    isCompact = isCompact,
                                                    showDescriptions = showDescriptions,
                                                    isTv = isTv,
                                                    modifier = Modifier.padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = if (isCompact) 3.dp else 4.dp)
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
                                            isCompact = isCompact,
                                            showDescriptions = showDescriptions,
                                            isTv = isTv,
                                            modifier = Modifier.padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = if (isCompact) 3.dp else 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Search Box at bottom
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (isCompact) 12.dp else 20.dp,
                                    vertical = if (isCompact) 8.dp else 12.dp
                                ),
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
    }
}

@Composable
private fun CategoryHeader(category: DnsCategory, isCompact: Boolean = false) {
    val isDarkTheme = isAppInDarkTheme()
    val categoryColor = remember(category, isDarkTheme) { CategoryColors.forCategory(category, isDarkTheme) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isCompact) 14.dp else 20.dp,
                end = if (isCompact) 14.dp else 20.dp,
                top = if (isCompact) 10.dp else 16.dp,
                bottom = if (isCompact) 4.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 24.dp else 28.dp)
                .background(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(if (isCompact) 6.dp else 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
            )
        }
        Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
        Text(
            text = category.displayName,
            style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
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
    isCompact: Boolean = false,
    showDescriptions: Boolean = true,
    isTv: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isAppInDarkTheme()
    val categoryColor = remember(server.category, isDarkTheme) {
        CategoryColors.forCategory(server.category, isDarkTheme)
    }

    // TV Focus handling with improved visibility
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // High-visibility focus color - use theme colors for proper Material You adaptation
    val focusBorderColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.inverseSurface
    } else {
        MaterialTheme.colorScheme.primary
    }

    // Use category colors for icon, with Material You compatible content when selected
    val iconBackgroundColor = if (isSelected) {
        categoryColor
    } else {
        categoryColor.copy(alpha = 0.15f)
    }
    val iconTintColor = if (isSelected) {
        contentColorFor(categoryColor)
    } else {
        categoryColor
    }

    // Compact sizes for TV/grid layout
    val iconSize = if (isCompact) 36.dp else 48.dp
    val iconInnerSize = if (isCompact) 18.dp else 24.dp
    val cardPadding = if (isCompact) 10.dp else 16.dp
    val checkSize = if (isCompact) 24.dp else 32.dp
    val checkIconSize = if (isCompact) 16.dp else 20.dp
    val cardShape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp)

    // Scale animation for TV focus
    val focusScale = if (isTv && isFocused) 1.03f else 1f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(focusScale)
            .then(
                if (isTv && isFocused) {
                    Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = cardShape,
                            ambientColor = focusBorderColor.copy(alpha = 0.3f),
                            spotColor = focusBorderColor.copy(alpha = 0.3f)
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
        color = if (isFocused && isTv) {
            // Slightly brighter background when focused on TV
            if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
        } else if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon - use solid colors for proper contrast
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .background(
                        color = iconBackgroundColor,
                        shape = RoundedCornerShape(if (isCompact) 8.dp else 12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = server.category.icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(iconInnerSize)
                )
            }

            Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 16.dp))

            // Server Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (showDescriptions) {
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
                }

                // DNS IPs or DoH URL as chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (server.isDoH && !server.dohUrl.isNullOrBlank()) {
                        DnsIpChip(
                            ip = server.dohUrl,
                            isSelected = isSelected,
                            isCompact = isCompact
                        )
                    } else {
                        DnsIpChip(
                            ip = server.primaryDns,
                            isSelected = isSelected,
                            isCompact = isCompact
                        )
                        DnsIpChip(
                            ip = server.secondaryDns,
                            isSelected = isSelected,
                            isCompact = isCompact
                        )
                    }
                }
            }

            // Delete button for custom servers
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(if (isCompact) 32.dp else 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
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
                        .size(checkSize)
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
                        modifier = Modifier.size(checkIconSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun DnsIpChip(
    ip: String,
    isSelected: Boolean,
    isCompact: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(if (isCompact) 4.dp else 6.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Text(
            text = ip,
            style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.padding(
                horizontal = if (isCompact) 6.dp else 8.dp,
                vertical = if (isCompact) 2.dp else 4.dp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DnsPickerCompactLandscapeContent(
    servers: Map<DnsCategory, List<DnsServer>>,
    selectedServer: DnsServer?,
    selectedCategory: DnsCategory?,
    onCategorySelected: (DnsCategory?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredServers: List<DnsServer>,
    filteredServersMap: Map<DnsCategory, List<DnsServer>>,
    onServerSelected: (DnsServer) -> Unit,
    onAddCustomDns: () -> Unit,
    onDeleteCustomDns: (String) -> Unit,
    onDismiss: () -> Unit,
    onFindFastest: () -> Unit,
    isTv: Boolean,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Left column: Title, Find Fastest, Category filters
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
                .padding(end = 8.dp)
        ) {
            // Header with title and buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.select_server),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(
                        onClick = onAddCustomDns,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Custom DNS",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Find Fastest Button
            Button(
                onClick = {
                    onDismiss()
                    onFindFastest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.find_fastest),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips - vertical layout
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // "All" chip
                item {
                    val allChipBgColor = MaterialTheme.colorScheme.primary
                    val allChipContentColor = MaterialTheme.colorScheme.onPrimary
                    val isAllSelected = selectedCategory == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { onCategorySelected(null) },
                        label = {
                            Text(
                                text = "All",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAllSelected) allChipContentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = if (isAllSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = allChipContentColor
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = allChipBgColor,
                            selectedLabelColor = allChipContentColor,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category chips
                items(DnsCategory.entries.size) { index ->
                    val category = DnsCategory.entries[index]
                    val categoryColor = CategoryColors.forCategory(category, isDarkTheme)
                    val isThisCategorySelected = selectedCategory == category
                    val selectedContentColor = contentColorFor(categoryColor)
                    FilterChip(
                        selected = isThisCategorySelected,
                        onClick = {
                            onCategorySelected(if (selectedCategory == category) null else category)
                        },
                        label = {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isThisCategorySelected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isThisCategorySelected) selectedContentColor else categoryColor
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = categoryColor,
                            selectedLabelColor = selectedContentColor,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Right column: Server list and search
        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
        ) {
            // Server Grid
            val allServers = if (selectedCategory == null) {
                filteredServersMap.values.flatten()
            } else {
                filteredServers
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = allServers,
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
                        isCompact = true,
                        showDescriptions = false, // No descriptions in compact landscape grid
                        isTv = isTv,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Search Box at bottom
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_dns),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(12.dp),
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
