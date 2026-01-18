package com.dns.changer.ultimate.ui.screens.paywall

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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.models.StoreProduct

enum class SubscriptionPlan {
    MONTHLY,
    YEARLY,
    YEARLY_TRIAL
}

data class PlanDetails(
    val id: SubscriptionPlan,
    val title: String,
    val period: String,
    val price: String,
    val pricePerMonth: String?,
    val savings: String?,
    val badge: String?,
    val isBestValue: Boolean = false,
    val hasFreeTrial: Boolean = false,
    val trialDays: Int = 0,
    val storeProduct: StoreProduct? = null
)

@Composable
fun PaywallScreen(
    products: Map<String, StoreProduct>,
    isLoading: Boolean,
    onPurchase: (StoreProduct) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.YEARLY_TRIAL) }

    // Build plan details from products
    val plans = remember(products) {
        buildPlansList(products)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background gradient
            PaywallBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // Close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Hero Section
                HeroSection()

                Spacer(modifier = Modifier.height(24.dp))

                // Features List
                FeaturesSection()

                Spacer(modifier = Modifier.height(32.dp))

                // Subscription Plans
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose Your Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    plans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            isSelected = selectedPlan == plan.id,
                            onClick = { selectedPlan = plan.id }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CTA Button
                val selectedPlanDetails = plans.find { it.id == selectedPlan }
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Button(
                        onClick = {
                            selectedPlanDetails?.storeProduct?.let { onPurchase(it) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading && selectedPlanDetails?.storeProduct != null,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            val buttonText = if (selectedPlanDetails?.hasFreeTrial == true) {
                                "Start ${selectedPlanDetails.trialDays}-Day Free Trial"
                            } else {
                                "Continue with ${selectedPlanDetails?.title ?: "Plan"}"
                            }
                            Text(
                                text = buttonText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Trial info
                    if (selectedPlanDetails?.hasFreeTrial == true) {
                        Text(
                            text = "Cancel anytime during trial. You won't be charged until the trial ends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Restore & Terms
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Restore Purchases",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onRestore() }
                            .padding(8.dp)
                    )
                }

                // Trust indicators
                TrustIndicators()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PaywallBackground() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle gradient circles in background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.2f, size.height * 0.1f),
                radius = size.width * 0.6f
            ),
            center = Offset(size.width * 0.2f, size.height * 0.1f),
            radius = size.width * 0.6f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.9f, size.height * 0.3f),
                radius = size.width * 0.5f
            ),
            center = Offset(size.width * 0.9f, size.height * 0.3f),
            radius = size.width * 0.5f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tertiaryColor.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.8f),
                radius = size.width * 0.7f
            ),
            center = Offset(size.width * 0.5f, size.height * 0.8f),
            radius = size.width * 0.7f
        )
    }
}

@Composable
private fun HeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated Premium Icon
        AnimatedPremiumIcon()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Upgrade to Premium",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Unlock the full power of DNS protection",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnimatedPremiumIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "premium_icon")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rotating ring
        Canvas(
            modifier = Modifier
                .size(100.dp)
                .rotate(rotation)
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0f),
                        Color(0xFFFFD700).copy(alpha = 0.8f),
                        Color(0xFFFFF8DC).copy(alpha = 0.8f),
                        Color(0xFFFFD700).copy(alpha = 0f)
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Icon background with pulse
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(pulse)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFC107)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun FeaturesSection() {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureItem(
            icon = Icons.Default.PowerSettingsNew,
            title = "Start on Boot",
            description = "Auto-connect when your device starts"
        )
        FeatureItem(
            icon = Icons.Default.Block,
            title = "No Ads",
            description = "Enjoy a completely ad-free experience"
        )
        FeatureItem(
            icon = Icons.Default.Speed,
            title = "Unlimited Speed Tests",
            description = "Find the fastest DNS anytime"
        )
        FeatureItem(
            icon = Icons.Default.Security,
            title = "Priority Support",
            description = "Get help when you need it"
        )
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PlanCard(
    plan: PlanDetails,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (plan.isBestValue) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFC107)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box {
            // Best value badge
            if (plan.badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 0.dp, y = 0.dp)
                        .background(
                            brush = if (plan.isBestValue) {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFFFC107))
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            },
                            shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = plan.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.isBestValue) Color.Black else MaterialTheme.colorScheme.onTertiary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radio indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Plan info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (plan.hasFreeTrial) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "${plan.trialDays}-DAY FREE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (plan.pricePerMonth != null && plan.id != SubscriptionPlan.MONTHLY) {
                        Text(
                            text = "${plan.pricePerMonth}/month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (plan.savings != null) {
                        Text(
                            text = plan.savings,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = plan.price,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/${plan.period}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustIndicators() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TrustItem(icon = Icons.Default.Lock, text = "Secure\nPayment")
        TrustItem(icon = Icons.Default.Verified, text = "Cancel\nAnytime")
        TrustItem(icon = Icons.Default.Shield, text = "Privacy\nProtected")
    }
}

@Composable
private fun TrustItem(icon: ImageVector, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

private fun buildPlansList(products: Map<String, StoreProduct>): List<PlanDetails> {
    val monthlyProduct = products["dc_sub_1_month_7.50try"]
    val yearlyProduct = products["dc_sub_1_year_32.00try"]
    val yearlyTrialProduct = products["dc_sub_trial_1_year_32.00try"]

    val monthlyPrice = monthlyProduct?.price?.formatted ?: "₺7.50"
    val yearlyPrice = yearlyProduct?.price?.formatted ?: "₺32.00"
    val yearlyTrialPrice = yearlyTrialProduct?.price?.formatted ?: "₺32.00"

    // Calculate monthly equivalent and savings
    val monthlyAmount = 7.50
    val yearlyAmount = 32.00
    val yearlyPerMonth = yearlyAmount / 12
    val savingsPercent = ((monthlyAmount * 12 - yearlyAmount) / (monthlyAmount * 12) * 100).toInt()

    return listOf(
        PlanDetails(
            id = SubscriptionPlan.YEARLY_TRIAL,
            title = "Yearly",
            period = "year",
            price = yearlyTrialPrice,
            pricePerMonth = "₺${String.format("%.2f", yearlyPerMonth)}",
            savings = "Save $savingsPercent% vs monthly",
            badge = "BEST VALUE",
            isBestValue = true,
            hasFreeTrial = true,
            trialDays = 3,
            storeProduct = yearlyTrialProduct
        ),
        PlanDetails(
            id = SubscriptionPlan.YEARLY,
            title = "Yearly",
            period = "year",
            price = yearlyPrice,
            pricePerMonth = "₺${String.format("%.2f", yearlyPerMonth)}",
            savings = "Save $savingsPercent% vs monthly",
            badge = null,
            storeProduct = yearlyProduct
        ),
        PlanDetails(
            id = SubscriptionPlan.MONTHLY,
            title = "Monthly",
            period = "month",
            price = monthlyPrice,
            pricePerMonth = null,
            savings = null,
            badge = null,
            storeProduct = monthlyProduct
        )
    )
}
