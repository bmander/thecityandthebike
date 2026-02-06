package com.thecityandthebike

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thecityandthebike.ui.screens.LoginScreen
import com.thecityandthebike.ui.screens.MainScreen
import com.thecityandthebike.ui.screens.PhotoCaptureScreen
import com.thecityandthebike.ui.screens.QrScannerScreen
import com.thecityandthebike.ui.screens.RegisterScreen
import com.thecityandthebike.ui.viewmodel.AuthViewModel
import com.thecityandthebike.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ComposeUiFlags.isSemanticAutofillEnabled = true
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val authState by authViewModel.state.collectAsState()

                    NavHost(
                        navController = navController,
                        startDestination = "main"
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
                                isLoggedIn = authState.isLoggedIn,
                                onLogout = {
                                    authViewModel.logout()
                                },
                                onLoginClick = {
                                    navController.navigate("login")
                                },
                                onScanQrCode = {
                                    navController.navigate("scanner")
                                }
                            )
                        }

                        composable("scanner") {
                            QrScannerScreen(
                                onQrCodeScanned = { qrId ->
                                    navController.navigate("photo_capture/${Uri.encode(qrId)}") {
                                        popUpTo("scanner") { inclusive = true }
                                    }
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("photo_capture/{qrId}") { backStackEntry ->
                            val qrId = backStackEntry.arguments?.getString("qrId") ?: ""
                            if (qrId.isEmpty()) {
                                navController.popBackStack()
                                return@composable
                            }
                            val mainViewModel: MainViewModel = hiltViewModel(
                                navController.getBackStackEntry("main")
                            )
                            PhotoCaptureScreen(
                                onPhotoCaptured = { uri ->
                                    mainViewModel.addLocalImage(uri)
                                    mainViewModel.uploadAndCreateSubmission(
                                        contentResolver,
                                        cacheDir,
                                        uri,
                                        qrId
                                    )
                                    navController.popBackStack("main", inclusive = false)
                                },
                                onBack = {
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
