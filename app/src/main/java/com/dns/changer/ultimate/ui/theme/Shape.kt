package com.dns.changer.ultimate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Custom shapes for specific components
object DnsShapes {
    val Card = RoundedCornerShape(28.dp)
    val Button = RoundedCornerShape(14.dp)
    val SmallButton = RoundedCornerShape(10.dp)
    val Chip = RoundedCornerShape(8.dp)
    val Dialog = RoundedCornerShape(28.dp)
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}
