package com.thecityandthebike

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thecityandthebike.data.local.OnboardingPrefs
import com.thecityandthebike.ui.screens.BikeScreen
import com.thecityandthebike.ui.screens.ImageDetailScreen
import com.thecityandthebike.ui.screens.LoginScreen
import com.thecityandthebike.ui.screens.MainScreen
import com.thecityandthebike.ui.screens.OnboardingScreen
import com.thecityandthebike.ui.screens.PhotoCaptureScreen
import com.thecityandthebike.ui.screens.PrivacyCopyrightScreen
import com.thecityandthebike.ui.screens.QrScannerScreen
import com.thecityandthebike.ui.screens.RegisterScreen
import com.thecityandthebike.ui.screens.SplashScreen
import com.thecityandthebike.ui.screens.UserScreen
import com.thecityandthebike.ui.viewmodel.AuthViewModel
import com.thecityandthebike.ui.viewmodel.BikeViewModel
import com.thecityandthebike.ui.viewmodel.MainViewModel
import com.thecityandthebike.ui.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var onboardingPrefs: OnboardingPrefs

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
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onTimeout = {
                                    val destination = if (onboardingPrefs.isOnboardingCompleted()) "main" else "onboarding"
                                    navController.navigate(destination) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("onboarding") {
                            OnboardingScreen(
                                onFinished = {
                                    onboardingPrefs.setOnboardingCompleted()
                                    navController.navigate("main") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

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
                            LaunchedEffect(authState.isLoggedIn) {
                                if (authState.isLoggedIn) {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
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
                                },
                                onShowPrivacyCopyright = {
                                    navController.navigate("privacy_copyright")
                                },
                                onImageClick = { submissionId ->
                                    navController.navigate("image_detail/${Uri.encode(submissionId)}")
                                }
                            )
                        }

                        composable("privacy_copyright") {
                            PrivacyCopyrightScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            "image_detail/{submissionId}",
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) { backStackEntry ->
                            val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
                            if (submissionId.isEmpty()) {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                                return@composable
                            }
                            val mainEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("main")
                            }
                            val mainViewModel: MainViewModel = hiltViewModel(mainEntry)
                            val mainState by mainViewModel.state.collectAsState()
                            if (mainState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                                return@composable
                            }
                            val submission = mainState.submissions.find { it.submissionId == submissionId }
                            if (submission != null) {
                                ImageDetailScreen(
                                    submission = submission,
                                    onBack = { navController.popBackStack() },
                                    onBikeClick = { bikeQrId ->
                                        navController.navigate("bike/${Uri.encode(bikeQrId)}")
                                    },
                                    onUserClick = { userId ->
                                        navController.navigate("user/${Uri.encode(userId)}")
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
                        }

                        composable("bike/{bikeQrId}") { backStackEntry ->
                            val bikeViewModel: BikeViewModel = hiltViewModel()
                            BikeScreen(
                                viewModel = bikeViewModel,
                                onBack = { navController.popBackStack() },
                                onImageClick = { submissionId ->
                                    navController.navigate("bike_image_detail/${Uri.encode(submissionId)}")
                                }
                            )
                        }

                        composable(
                            "bike_image_detail/{submissionId}",
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) { backStackEntry ->
                            val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
                            if (submissionId.isEmpty()) {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                                return@composable
                            }
                            val bikeEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("bike/{bikeQrId}")
                            }
                            val bikeViewModel: BikeViewModel = hiltViewModel(bikeEntry)
                            val bikeState by bikeViewModel.state.collectAsState()
                            if (bikeState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                                return@composable
                            }
                            val submission = bikeState.submissions.find { it.submissionId == submissionId }
                            if (submission != null) {
                                ImageDetailScreen(
                                    submission = submission,
                                    onBack = { navController.popBackStack() },
                                    onUserClick = { userId ->
                                        navController.navigate("user/${Uri.encode(userId)}")
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
                        }

                        composable("user/{userId}") { backStackEntry ->
                            val userViewModel: UserViewModel = hiltViewModel()
                            UserScreen(
                                viewModel = userViewModel,
                                onBack = { navController.popBackStack() },
                                onImageClick = { submissionId ->
                                    navController.navigate("user_image_detail/${Uri.encode(submissionId)}")
                                }
                            )
                        }

                        composable(
                            "user_image_detail/{submissionId}",
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) { backStackEntry ->
                            val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
                            if (submissionId.isEmpty()) {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                                return@composable
                            }
                            val userEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("user/{userId}")
                            }
                            val userViewModel: UserViewModel = hiltViewModel(userEntry)
                            val userState by userViewModel.state.collectAsState()
                            if (userState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                                return@composable
                            }
                            val submission = userState.submissions.find { it.submissionId == submissionId }
                            if (submission != null) {
                                ImageDetailScreen(
                                    submission = submission,
                                    onBack = { navController.popBackStack() },
                                    onBikeClick = { bikeQrId ->
                                        navController.navigate("bike/${Uri.encode(bikeQrId)}")
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
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
                            val mainEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("main")
                            }
                            val mainViewModel: MainViewModel = hiltViewModel(mainEntry)
                            PhotoCaptureScreen(
                                onPhotoCaptured = { uri ->
                                    mainViewModel.addLocalImage(uri)
                                    mainViewModel.uploadAndCreateSubmission(uri, qrId)
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
