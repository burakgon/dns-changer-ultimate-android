package com.dns.changer.ultimate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.graphics.vector.ImageVector
import com.dns.changer.ultimate.R

sealed class Screen(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Connect : Screen(
        route = "connect",
        titleResId = R.string.connect_tab,
        selectedIcon = Icons.Filled.Power,
        unselectedIcon = Icons.Outlined.Power
    )

    data object SpeedTest : Screen(
        route = "speed_test",
        titleResId = R.string.speed_test_tab,
        selectedIcon = Icons.Filled.Speed,
        unselectedIcon = Icons.Outlined.Speed
    )

    data object LeakTest : Screen(
        route = "leak_test",
        titleResId = R.string.leak_test_tab,
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )

    data object Settings : Screen(
        route = "settings",
        titleResId = R.string.settings_tab,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    companion object {
        val bottomNavItems = listOf(Connect, SpeedTest, LeakTest, Settings)
    }
}
