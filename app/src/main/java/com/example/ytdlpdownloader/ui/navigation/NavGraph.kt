package com.example.ytdlpdownloader.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Queue : Screen("queue")
    object History : Screen("history")
    object Settings : Screen("settings")
    object FormatPicker : Screen("format_picker/{downloadId}") {
        fun createRoute(downloadId: String) = "format_picker/$downloadId"
    }
    object Log : Screen("log/{downloadId}") {
        fun createRoute(downloadId: String) = "log/$downloadId"
    }
    object Templates : Screen("templates")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Home,
        label = "Home",
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        screen = Screen.Queue,
        label = "Queue",
        icon = Icons.Default.QueueMusic
    ),
    BottomNavItem(
        screen = Screen.History,
        label = "History",
        icon = Icons.Default.History
    ),
    BottomNavItem(
        screen = Screen.Settings,
        label = "Settings",
        icon = Icons.Default.Settings
    )
)
