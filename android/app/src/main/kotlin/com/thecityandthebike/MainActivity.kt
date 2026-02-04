package com.thecityandthebike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thecityandthebike.ui.screens.LoginScreen
import com.thecityandthebike.ui.screens.MainScreen
import com.thecityandthebike.ui.screens.RegisterScreen
import com.thecityandthebike.ui.viewmodel.AuthViewModel
import com.thecityandthebike.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val authState by authViewModel.state.collectAsState()

                    val startDestination = if (authState.isLoggedIn) "main" else "login"

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("login") {
                            LoginScreen(
                                state = authState,
                                onLogin = { username, password ->
                                    authViewModel.login(username, password)
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                },
                                onClearError = { authViewModel.clearError() }
                            )

                            // Navigate to main when logged in
                            if (authState.isLoggedIn) {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }

                        composable("register") {
                            RegisterScreen(
                                state = authState,
                                onRegister = { username, email, password ->
                                    authViewModel.register(username, email, password)
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onClearError = { authViewModel.clearError() },
                                onClearRegistrationSuccess = { authViewModel.clearRegistrationSuccess() }
                            )
                        }

                        composable("main") {
                            val mainViewModel: MainViewModel = hiltViewModel()
                            MainScreen(
                                viewModel = mainViewModel,
                                onLogout = {
                                    authViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
