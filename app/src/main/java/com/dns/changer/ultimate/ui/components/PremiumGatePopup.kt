package com.dns.changer.ultimate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.res.Configuration
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.ui.theme.DnsShapes
import com.dns.changer.ultimate.ui.theme.rememberSemanticColors
import kotlin.math.roundToInt

// Gold colors for premium badge (these are intentional branding colors)
private val GoldColor = Color(0xFFFFD700)
private val GoldColorLight = Color(0xFFFFF8DC)

@Composable
fun PremiumGatePopup(
    visible: Boolean,
    featureIcon: ImageVector = Icons.Default.Speed,
    featureTitle: String = stringResource(R.string.unlock_feature),
    featureDescription: String = stringResource(R.string.premium_description),
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit,
    onGoPremium: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isCompactLandscape = isLandscape && configuration.screenHeightDp < 500

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
            // Scrim background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
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

                Card(
                    modifier = Modifier
                        .widthIn(max = if (isCompactLandscape) 700.dp else 500.dp)
                        .fillMaxWidth(if (isCompactLandscape) 0.95f else 0.92f)
                        .offset { IntOffset(0, -floatOffset.roundToInt().dp.roundToPx()) }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Prevent click through */ },
                    shape = DnsShapes.Dialog,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    if (isCompactLandscape) {
                        // Horizontal layout for compact landscape
                        Row {
                            // Header with gradient on left
                            PremiumHeaderCompact(
                                icon = featureIcon,
                                title = featureTitle,
                                onClose = onDismiss,
                                modifier = Modifier.weight(0.4f)
                            )

                            // Content section on right
                            PremiumContentCompact(
                                description = featureDescription,
                                onWatchAd = {
                                    onDismiss()
                                    onWatchAd()
                                },
                                onGoPremium = {
                                    onDismiss()
                                    onGoPremium()
                                },
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    } else {
                        // Standard vertical layout
                        Column {
                            // Header with gradient
                            PremiumHeader(
                                icon = featureIcon,
                                title = featureTitle,
                                onClose = onDismiss
                            )

                            // Content section
                            PremiumContent(
                                description = featureDescription,
                                onWatchAd = {
                                    onDismiss()
                                    onWatchAd()
                                },
                                onGoPremium = {
                                    onDismiss()
                                    onGoPremium()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumHeader(
    icon: ImageVector,
    title: String,
    onClose: () -> Unit
) {
    // Use Material You colors for the gradient
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    // Content color for proper contrast on the gradient background
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(primaryColor, secondaryColor, tertiaryColor)
                ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
    ) {
        // Decorative circles
        DecorativeCircles(contentColor)

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = contentColor.copy(alpha = 0.8f)
            )
        }

        // Animated icon
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedFeatureIcon(icon = icon, contentColor = contentColor)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun PremiumHeaderCompact(
    icon: ImageVector,
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use Material You colors for the gradient
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, secondaryColor, tertiaryColor)
                ),
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
            )
    ) {
        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Animated icon and title
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedFeatureIconCompact(icon = icon, contentColor = contentColor)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnimatedFeatureIconCompact(icon: ImageVector, contentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "iconAnimation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rotating ring
        Canvas(
            modifier = Modifier
                .size(52.dp)
                .rotate(rotation)
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        contentColor.copy(alpha = 0f),
                        contentColor.copy(alpha = 0.8f),
                        contentColor.copy(alpha = 0f)
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center icon background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DecorativeCircles(contentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Large translucent circle - top right
        drawCircle(
            color = contentColor.copy(alpha = 0.1f),
            radius = 100.dp.toPx(),
            center = Offset(size.width * 0.85f, size.height * 0.2f)
        )

        // Medium circle - bottom left
        drawCircle(
            color = contentColor.copy(alpha = 0.08f),
            radius = 60.dp.toPx(),
            center = Offset(size.width * 0.15f, size.height * 0.8f)
        )

        // Small circle - center right
        drawCircle(
            color = contentColor.copy(alpha = 0.12f),
            radius = 30.dp.toPx(),
            center = Offset(size.width * 0.9f, size.height * 0.6f)
        )
    }
}

@Composable
private fun AnimatedFeatureIcon(icon: ImageVector, contentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "iconAnimation")

    // Rotating ring - 8 second rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsing glow
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing glow background
        Box(
            modifier = Modifier
                .size(70.dp)
                .scale(pulseScale)
                .alpha(glowAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            contentColor.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Rotating ring
        Canvas(
            modifier = Modifier
                .size(76.dp)
                .rotate(rotation)
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        contentColor.copy(alpha = 0f),
                        contentColor.copy(alpha = 0.8f),
                        contentColor.copy(alpha = 0f)
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Center icon background
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun PremiumContent(
    description: String,
    onWatchAd: () -> Unit,
    onGoPremium: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Continue Free button with shimmer
            ShimmerButton(
                text = stringResource(R.string.continue_free),
                subtitle = stringResource(R.string.quick_video),
                onClick = onWatchAd
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Or divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = stringResource(R.string.or),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Go Premium button
            PremiumButton(
                text = stringResource(R.string.go_premium),
                onClick = onGoPremium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Benefits row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BenefitChip(
                    icon = Icons.Default.Block,
                    text = stringResource(R.string.no_ads)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BenefitChip(
                    icon = Icons.Default.Star,
                    text = stringResource(R.string.all_features)
                )
            }
        }
    }
}

@Composable
private fun PremiumContentCompact(
    description: String,
    onWatchAd: () -> Unit,
    onGoPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Continue Free button with shimmer - compact
            ShimmerButtonCompact(
                text = stringResource(R.string.continue_free),
                subtitle = stringResource(R.string.quick_video),
                onClick = onWatchAd
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Or divider - compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = stringResource(R.string.or),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Go Premium button - compact
            PremiumButtonCompact(
                text = stringResource(R.string.go_premium),
                onClick = onGoPremium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Benefits row - compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BenefitChipCompact(
                    icon = Icons.Default.Block,
                    text = stringResource(R.string.no_ads)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BenefitChipCompact(
                    icon = Icons.Default.Star,
                    text = stringResource(R.string.all_features)
                )
            }
        }
    }
}

@Composable
private fun ShimmerButtonCompact(
    text: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val semanticColors = rememberSemanticColors()
    val buttonColor = semanticColors.success

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = DnsShapes.Button,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
    ) {
        val buttonContentColor = MaterialTheme.colorScheme.surface
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = buttonContentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = buttonContentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = buttonContentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PremiumButtonCompact(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(GoldColor.copy(alpha = 0.5f), GoldColorLight.copy(alpha = 0.3f))
                ),
                shape = DnsShapes.Button
            ),
        shape = DnsShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GoldColor, GoldColorLight)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BenefitChipCompact(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = DnsShapes.Chip,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ShimmerButton(
    text: String,
    subtitle: String,
    onClick: () -> Unit
) {
    // Use semantic success colors for the "continue free" action
    val semanticColors = rememberSemanticColors()
    val buttonColor = semanticColors.success

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = DnsShapes.Button,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = buttonColor,
                    shape = DnsShapes.Button
                ),
            contentAlignment = Alignment.Center
        ) {
            // Shimmer overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            startX = shimmerOffset * 500f,
                            endX = (shimmerOffset + 0.5f) * 500f
                        ),
                        shape = DnsShapes.Button
                    )
            )

            // Use surface color for content on success background for contrast
            val buttonContentColor = MaterialTheme.colorScheme.surface
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = buttonContentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = buttonContentColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = buttonContentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(GoldColor.copy(alpha = 0.5f), GoldColorLight.copy(alpha = 0.3f))
                ),
                shape = DnsShapes.Button
            ),
        shape = DnsShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        // Crown icon with gold gradient - use dark color for icon on gold background
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GoldColor, GoldColorLight)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.8f), // Dark icon on gold background
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BenefitChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = DnsShapes.Chip,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
