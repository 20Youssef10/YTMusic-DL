package com.example.ytdlpdownloader.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ytdlpdownloader.data.model.AppSettings
import com.example.ytdlpdownloader.ui.history.HistoryScreen
import com.example.ytdlpdownloader.ui.home.HomeScreen
import com.example.ytdlpdownloader.ui.home.HomeViewModel
import com.example.ytdlpdownloader.ui.log.LogScreen
import com.example.ytdlpdownloader.ui.navigation.Screen
import com.example.ytdlpdownloader.ui.navigation.bottomNavItems
import com.example.ytdlpdownloader.ui.queue.QueueScreen
import com.example.ytdlpdownloader.ui.settings.SettingsScreen
import com.example.ytdlpdownloader.ui.theme.YtDlpDownloaderTheme
import com.example.ytdlpdownloader.util.PreferencesManager
import com.example.ytdlpdownloader.util.extractUrlFromText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle intent (shared URL)
        handleIntent(intent)

        setContent {
            val settings by preferencesManager.appSettings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )

            YtDlpDownloaderTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.useDynamicColor
            ) {
                MainNavHost(homeViewModel = homeViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val sharedText = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
        sharedText?.let { text ->
            val url = extractUrlFromText(text) ?: text
            homeViewModel.onSharedUrl(url)
        }
    }
}

// ─── Main Navigation Host ─────────────────────────────────────────────────────

@Composable
fun MainNavHost(homeViewModel: HomeViewModel) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    Scaffold(
        bottomBar = {
            val showBottomBar = bottomNavItems.any {
                currentDestination?.route == it.screen.route
            }
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.route == item.screen.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToQueue = {
                        navController.navigate(Screen.Queue.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    viewModel = homeViewModel
                )
            }

            composable(Screen.Queue.route) {
                QueueScreen(
                    onViewLog = { downloadId ->
                        navController.navigate(Screen.Log.createRoute(downloadId))
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateToQueue = {
                        navController.navigate(Screen.Queue.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.Log.route,
                arguments = listOf(
                    androidx.navigation.navArgument("downloadId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) {
                LogScreen(
                    onBack = { navController.navigateUp() }
                )
            }
        }
    }
}
