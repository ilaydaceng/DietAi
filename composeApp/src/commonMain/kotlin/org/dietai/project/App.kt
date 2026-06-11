package org.dietai.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview

// Navigation
import org.dietai.project.navigation.Screen
import org.dietai.project.ui.theme.DietAiTheme
import androidx.compose.material3.SnackbarHostState
import org.dietai.project.ui.LocalSnackbarHostState
import org.dietai.project.ui.auth.AuthScreen
import org.dietai.project.ui.dietitian.DiyetisyenHomeScreen
import org.dietai.project.ui.MainContainer
import org.dietai.project.ui.splash.SplashScreen
import org.dietai.project.ui.onboarding.OnboardingScreen
import androidx.navigation.toRoute
import org.dietai.project.ui.auth.ForgotPasswordScreen
import org.dietai.project.ui.dietitian.ClientDetailScreen
import org.dietai.project.ui.messaging.DirectMessageScreen

@Composable
@Preview
fun App() {
    DietAiTheme {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            Scaffold(
                snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    NavHost(
                navController = navController,
                startDestination = Screen.Splash
            ) {
                composable<Screen.Splash> {
                    SplashScreen(
                        onNavigateToHome = {
                            navController.navigate(Screen.Main) {
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                        },
                        onNavigateToAuth = {
                            navController.navigate(Screen.Onboarding) {
                                popUpTo(Screen.Splash) { inclusive = true }
                            }
                        }
                    )
                }

                composable<Screen.Onboarding> {
                    OnboardingScreen(
                        onFinish = {
                            navController.navigate(Screen.Auth) {
                                popUpTo(Screen.Onboarding) { inclusive = true }
                            }
                        }
                    )
                }

                composable<Screen.ForgotPassword> {
                    ForgotPasswordScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<Screen.Auth> {
                    AuthScreen(
                        girisBasarili = { rol ->
                            if (rol == "Diyetisyen") {
                                navController.navigate(Screen.DietitianHome) {
                                    popUpTo(Screen.Auth) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Main) {
                                    popUpTo(Screen.Auth) { inclusive = true }
                                }
                            }
                        },
                        onSifremiUnuttum = {
                            navController.navigate(Screen.ForgotPassword)
                        }
                    )
                }

                composable<Screen.Main> {
                    MainContainer(rootNavController = navController)
                }

                composable<Screen.DietitianHome> {
                    DiyetisyenHomeScreen(
                        cikisYap = {
                            navController.navigate(Screen.Auth) {
                                popUpTo(Screen.DietitianHome) { inclusive = true }
                            }
                        },
                        onDanisanSecildi = { userId ->
                            navController.navigate(Screen.ClientDetail(userId))
                        }
                    )
                }

                composable<Screen.ClientDetail> { backStackEntry ->
                    val clientDetail: Screen.ClientDetail = backStackEntry.toRoute()
                    ClientDetailScreen(
                        userId = clientDetail.userId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToChat = { chatId, clientName ->
                            navController.navigate(Screen.DirectMessage(chatId, clientName))
                        }
                    )
                }

                composable<Screen.DirectMessage> { backStackEntry ->
                    val args: Screen.DirectMessage = backStackEntry.toRoute()
                    DirectMessageScreen(
                        chatId = args.chatId,
                        otherUserName = args.otherUserName,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                    }
                }
            }
        }
    }
}