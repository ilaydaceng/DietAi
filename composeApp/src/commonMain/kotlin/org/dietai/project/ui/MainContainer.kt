package org.dietai.project.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import org.dietai.project.navigation.MainTab
import org.dietai.project.navigation.Screen
import org.dietai.project.ui.home.HomeScreen
import org.dietai.project.ui.profile.ProfileScreen
import org.dietai.project.ui.clock.ClockScreen
import org.dietai.project.ui.exercise.ExerciseScreen
import org.dietai.project.chat.ChatScreen // ChatScreen import edildi

@Composable
fun MainContainer(rootNavController: NavController) {
    val tabNavController = rememberNavController()
    val snackbarHostState = LocalSnackbarHostState.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    MainTab.Home,
                    MainTab.MealLog,
                    MainTab.ChatTab,
                    MainTab.Clock,
                    MainTab.ExerciseLog,
                    MainTab.Profile
                )

                items.forEach { screen ->
                    val isSelected = currentDestination?.hasRoute(screen::class) == true

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            tabNavController.navigate(screen) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(tabNavController.graph.startDestinationRoute ?: return@navigate) {
                                    saveState = true
                                }
                            }
                        },
                        icon = {
                            val icon = when (screen) {
                                MainTab.Home -> Icons.Default.Home
                                MainTab.Profile -> Icons.Default.Person
                                MainTab.MealLog -> Icons.Default.Restaurant
                                MainTab.ChatTab -> Icons.Default.Chat
                                MainTab.ExerciseLog -> Icons.Default.DirectionsRun
                                MainTab.Clock -> Icons.Default.Schedule
                                else -> Icons.Default.Menu
                            }
                            Icon(icon, contentDescription = null)
                        },
                        label = {
                            val label = when (screen) {
                                MainTab.Home -> "Ana Sayfa"
                                MainTab.Profile -> "Profil"
                                MainTab.MealLog -> "Öğün"
                                MainTab.ChatTab -> "AI Diyetisyen"
                                MainTab.ExerciseLog -> "Spor"
                                MainTab.Clock -> "Saat"
                                else -> "Menü"
                            }
                            Text(label)
                        },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainTab.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<MainTab.Home> {
                HomeScreen(
                    kullaniciTuru = "Danışan",
                    cikisYap = {
                        rootNavController.navigate(Screen.Auth) {
                            popUpTo(Screen.Main) { inclusive = true }
                        }
                    },
                    onNavigateToChat = { chatId, dietitianName ->
                        rootNavController.navigate(Screen.DirectMessage(chatId, dietitianName))
                    }
                )
            }

            composable<MainTab.MealLog> {
                org.dietai.project.ui.meal.MealScreen()
            }

            // AI Diyetisyen (Chat) Sekmesi
            composable<MainTab.ChatTab> {
                ChatScreen()
            }

            composable<MainTab.Clock> {
                ClockScreen()
            }

            composable<MainTab.ExerciseLog> {
                ExerciseScreen()
            }

            composable<MainTab.Profile> {
                ProfileScreen(
                    onSignOut = {
                        rootNavController.navigate(Screen.Auth) {
                            popUpTo(Screen.Main) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}