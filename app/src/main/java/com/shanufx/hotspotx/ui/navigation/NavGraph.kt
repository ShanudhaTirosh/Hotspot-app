package com.shanufx.hotspotx.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.shanufx.hotspotx.ui.components.PermissionGuard
import com.shanufx.hotspotx.ui.components.requiredPermissions
import com.shanufx.hotspotx.ui.dashboard.DashboardScreen
import com.shanufx.hotspotx.ui.devices.DevicesScreen
import com.shanufx.hotspotx.ui.controls.ControlsScreen
import com.shanufx.hotspotx.ui.settings.SettingsScreen
import com.shanufx.hotspotx.ui.usage.UsageScreen
import com.shanufx.hotspotx.ui.theme.*

sealed class NavRoute(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : NavRoute("dashboard", "Dashboard", Icons.Rounded.Home)
    object Devices   : NavRoute("devices",   "Devices",   Icons.Rounded.Devices)
    object Usage     : NavRoute("usage",     "Usage",     Icons.Rounded.BarChart)
    object Controls  : NavRoute("controls",  "Controls",  Icons.Rounded.Settings)
    object Settings  : NavRoute("settings",  "Settings",  Icons.Rounded.Tune)
}

private val tabs = listOf(
    NavRoute.Dashboard,
    NavRoute.Devices,
    NavRoute.Usage,
    NavRoute.Controls,
    NavRoute.Settings
)

@Composable
fun HotspotXNavGraph() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    PermissionGuard(permissions = requiredPermissions()) {
        Scaffold(
            containerColor = BackgroundDark,
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(NavRoute.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanPrimary,
                                selectedTextColor = CyanPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = NavRoute.Dashboard.route,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) }
            ) {
                composable(NavRoute.Dashboard.route) { DashboardScreen() }
                composable(NavRoute.Devices.route)   { DevicesScreen() }
                composable(NavRoute.Usage.route)     { UsageScreen() }
                composable(NavRoute.Controls.route)  { ControlsScreen() }
                composable(NavRoute.Settings.route)  { SettingsScreen() }
            }
        }
    }
}
