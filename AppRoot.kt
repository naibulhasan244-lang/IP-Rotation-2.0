package com.yourname.iprotation2.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourname.iprotation2.ui.history.HistoryScreen
import com.yourname.iprotation2.ui.home.HomeScreen
import com.yourname.iprotation2.ui.proxies.ProxiesScreen
import com.yourname.iprotation2.ui.settings.SettingsScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Default.Home),
    Tab("proxies", "Proxies", Icons.Default.Dns),
    Tab("history", "History", Icons.Default.History),
    Tab("settings", "Settings", Icons.Default.Settings)
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { inner: PaddingValues ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(inner)
        ) {
            composable("home") { HomeScreen() }
            composable("proxies") { ProxiesScreen() }
            composable("history") { HistoryScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
