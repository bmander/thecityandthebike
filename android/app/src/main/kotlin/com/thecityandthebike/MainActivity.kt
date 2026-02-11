package com.thecityandthebike

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.thecityandthebike.data.local.OnboardingPrefs
import com.thecityandthebike.navigation.Bike
import com.thecityandthebike.navigation.BikeImageDetail
import com.thecityandthebike.navigation.ImageDetail
import com.thecityandthebike.navigation.Login
import com.thecityandthebike.navigation.Main
import com.thecityandthebike.navigation.Onboarding
import com.thecityandthebike.navigation.PhotoCapture
import com.thecityandthebike.navigation.PhotoPreview
import com.thecityandthebike.navigation.PrivacyCopyright
import com.thecityandthebike.navigation.Register
import com.thecityandthebike.navigation.Scanner
import com.thecityandthebike.navigation.Splash
import com.thecityandthebike.navigation.User
import com.thecityandthebike.navigation.UserImageDetail
import com.thecityandthebike.ui.screens.BikeScreen
import com.thecityandthebike.ui.screens.ImageDetailScreen
import com.thecityandthebike.ui.screens.LoginScreen
import com.thecityandthebike.ui.screens.MainScreen
import com.thecityandthebike.ui.screens.OnboardingScreen
import com.thecityandthebike.ui.screens.PhotoCaptureScreen
import com.thecityandthebike.ui.screens.PhotoPreviewScreen
import com.thecityandthebike.ui.screens.PrivacyCopyrightScreen
import com.thecityandthebike.ui.screens.QrScannerScreen
import com.thecityandthebike.ui.screens.RegisterScreen
import com.thecityandthebike.ui.screens.SplashScreen
import com.thecityandthebike.ui.screens.UserScreen
import com.thecityandthebike.ui.viewmodel.AuthViewModel
import com.thecityandthebike.ui.viewmodel.BikeViewModel
import com.thecityandthebike.ui.viewmodel.ImageDetailViewModel
import com.thecityandthebike.ui.viewmodel.MainViewModel
import com.thecityandthebike.ui.viewmodel.PhotoPreviewViewModel
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
                    val authState by authViewModel.state.collectAsStateWithLifecycle()

                    NavHost(
                        navController = navController,
                        startDestination = Splash
                    ) {
                        composable<Splash> {
                            SplashScreen(
                                onTimeout = {
                                    val destination: Any = if (onboardingPrefs.isOnboardingCompleted()) Main else Onboarding
                                    navController.navigate(destination) {
                                        popUpTo<Splash> { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable<Onboarding> {
                            OnboardingScreen(
                                onFinished = {
                                    onboardingPrefs.setOnboardingCompleted()
                                    navController.navigate(Main) {
                                        popUpTo<Onboarding> { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable<Login> {
                            LoginScreen(
                                state = authState,
                                onLogin = { username, password ->
                                    authViewModel.login(username, password)
                                },
                                onNavigateToRegister = {
                                    navController.navigate(Register)
                                },
                                onClearError = { authViewModel.clearError() }
                            )

                            // Navigate to main when logged in
                            LaunchedEffect(authState.isLoggedIn) {
                                if (authState.isLoggedIn) {
                                    navController.navigate(Main) {
                                        popUpTo<Login> { inclusive = true }
                                    }
                                }
                            }
                        }

                        composable<Register> {
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

                        composable<Main> {
                            val mainViewModel: MainViewModel = hiltViewModel()
                            MainScreen(
                                viewModel = mainViewModel,
                                isLoggedIn = authState.isLoggedIn,
                                onLogout = {
                                    authViewModel.logout()
                                },
                                onLoginClick = {
                                    navController.navigate(Login)
                                },
                                onScanQrCode = {
                                    navController.navigate(Scanner)
                                },
                                onShowPrivacyCopyright = {
                                    navController.navigate(PrivacyCopyright)
                                },
                                onImageClick = { submissionId ->
                                    navController.navigate(ImageDetail(submissionId))
                                }
                            )
                        }

                        composable<PrivacyCopyright> {
                            PrivacyCopyrightScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<ImageDetail>(
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) {
                            val viewModel: ImageDetailViewModel = hiltViewModel()
                            val detailState by viewModel.state.collectAsStateWithLifecycle()
                            when {
                                detailState.isLoading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                                detailState.submission != null -> {
                                    ImageDetailScreen(
                                        submission = detailState.submission!!,
                                        onBack = { navController.popBackStack() },
                                        onBikeClick = { bikeQrId ->
                                            navController.navigate(Bike(bikeQrId))
                                        },
                                        onUserClick = { userId ->
                                            navController.navigate(User(userId))
                                        }
                                    )
                                }
                                else -> {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }
                        }

                        composable<Bike> { backStackEntry ->
                            val bikeViewModel: BikeViewModel = hiltViewModel()
                            BikeScreen(
                                viewModel = bikeViewModel,
                                onBack = { navController.popBackStack() },
                                onImageClick = { submissionId ->
                                    navController.navigate(BikeImageDetail(submissionId))
                                }
                            )
                        }

                        composable<BikeImageDetail>(
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) {
                            val viewModel: ImageDetailViewModel = hiltViewModel()
                            val detailState by viewModel.state.collectAsStateWithLifecycle()
                            when {
                                detailState.isLoading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                                detailState.submission != null -> {
                                    ImageDetailScreen(
                                        submission = detailState.submission!!,
                                        onBack = { navController.popBackStack() },
                                        onUserClick = { userId ->
                                            navController.navigate(User(userId))
                                        }
                                    )
                                }
                                else -> {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }
                        }

                        composable<User> { backStackEntry ->
                            val userViewModel: UserViewModel = hiltViewModel()
                            UserScreen(
                                viewModel = userViewModel,
                                onBack = { navController.popBackStack() },
                                onImageClick = { submissionId ->
                                    navController.navigate(UserImageDetail(submissionId))
                                }
                            )
                        }

                        composable<UserImageDetail>(
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) {
                            val viewModel: ImageDetailViewModel = hiltViewModel()
                            val detailState by viewModel.state.collectAsStateWithLifecycle()
                            when {
                                detailState.isLoading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                                detailState.submission != null -> {
                                    ImageDetailScreen(
                                        submission = detailState.submission!!,
                                        onBack = { navController.popBackStack() },
                                        onBikeClick = { bikeQrId ->
                                            navController.navigate(Bike(bikeQrId))
                                        }
                                    )
                                }
                                else -> {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }
                        }

                        composable<Scanner> {
                            QrScannerScreen(
                                onQrCodeScanned = { qrId ->
                                    navController.navigate(PhotoCapture(qrId)) {
                                        popUpTo<Scanner> { inclusive = true }
                                    }
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<PhotoCapture> { backStackEntry ->
                            val route = backStackEntry.toRoute<PhotoCapture>()
                            if (route.qrId.isEmpty()) {
                                navController.popBackStack()
                                return@composable
                            }
                            PhotoCaptureScreen(
                                onPhotoCaptured = { uri ->
                                    navController.navigate(PhotoPreview(route.qrId, uri.toString())) {
                                        popUpTo<PhotoCapture> { inclusive = true }
                                    }
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<PhotoPreview> { backStackEntry ->
                            val route = backStackEntry.toRoute<PhotoPreview>()
                            if (route.qrId.isEmpty() || route.photoUri.isEmpty()) {
                                navController.popBackStack()
                                return@composable
                            }
                            val photoUri = android.net.Uri.parse(route.photoUri)
                            val viewModel: PhotoPreviewViewModel = hiltViewModel()
                            PhotoPreviewScreen(
                                photoUri = photoUri,
                                onConfirm = {
                                    viewModel.upload(photoUri, route.qrId)
                                    navController.popBackStack<Main>(inclusive = false)
                                },
                                onRetake = {
                                    navController.navigate(PhotoCapture(route.qrId)) {
                                        popUpTo<PhotoPreview> { inclusive = true }
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
