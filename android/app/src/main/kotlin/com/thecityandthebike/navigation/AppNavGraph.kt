package com.thecityandthebike.navigation

import android.app.DownloadManager
import androidx.core.net.toUri
import android.content.Context
import android.os.Environment
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.thecityandthebike.data.local.OnboardingPrefs
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
import com.thecityandthebike.util.imageUrlToUri
import com.thecityandthebike.ui.viewmodel.AuthViewModel
import com.thecityandthebike.ui.viewmodel.BikeViewModel
import com.thecityandthebike.ui.viewmodel.BikesListViewModel
import com.thecityandthebike.ui.viewmodel.ImageDetailViewModel
import com.thecityandthebike.ui.viewmodel.LeaderboardViewModel
import com.thecityandthebike.ui.viewmodel.MainViewModel
import com.thecityandthebike.ui.viewmodel.MeViewModel
import com.thecityandthebike.ui.viewmodel.PhotoPreviewViewModel
import com.thecityandthebike.ui.viewmodel.UserViewModel

@Composable
fun AppNavGraph(onboardingPrefs: OnboardingPrefs) {
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

        composable<Main> { backStackEntry ->
            val mainViewModel: MainViewModel = hiltViewModel()
            val leaderboardViewModel: LeaderboardViewModel = hiltViewModel()
            val bikesListViewModel: BikesListViewModel = hiltViewModel()
            val meViewModel: MeViewModel = hiltViewModel()
            val meState by meViewModel.state.collectAsStateWithLifecycle()
            ObserveDeletion(backStackEntry) {
                mainViewModel.removeSubmission(it)
                meViewModel.removeSubmission(it)
            }
            MainScreen(
                viewModel = mainViewModel,
                leaderboardViewModel = leaderboardViewModel,
                bikesListViewModel = bikesListViewModel,
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
                },
                onUserClick = { userId ->
                    navController.navigate(User(userId))
                },
                onBikeClick = { bikeQrId ->
                    navController.navigate(Bike(bikeQrId))
                },
                meState = meState,
                onMeImageClick = { submissionId ->
                    navController.navigate(ImageDetail(submissionId))
                },
                onMeLoadMore = { meViewModel.loadMoreSubmissions() },
                onMeClearError = { meViewModel.clearError() }
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
            ImageDetailRoute(
                onHome = { navController.popBackStack<Main>(inclusive = false) },
                onDeleted = navController.handleDeletion(),
                onBikeClick = { bikeQrId -> navController.navigate(Bike(bikeQrId)) },
                onUserClick = { userId -> navController.navigate(User(userId)) }
            )
        }

        composable<Bike> { backStackEntry ->
            val bikeViewModel: BikeViewModel = hiltViewModel()
            ObserveDeletion(backStackEntry) { bikeViewModel.removeSubmission(it) }
            BikeScreen(
                viewModel = bikeViewModel,
                onBack = { navController.popBackStack() },
                onImageClick = { submissionId ->
                    navController.navigate(BikeImageDetail(submissionId))
                },
                onUserClick = { userId -> navController.navigate(User(userId)) }
            )
        }

        composable<BikeImageDetail>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            ImageDetailRoute(
                onHome = { navController.popBackStack<Main>(inclusive = false) },
                onDeleted = navController.handleDeletion(),
                onUserClick = { userId -> navController.navigate(User(userId)) }
            )
        }

        composable<User> { backStackEntry ->
            val userViewModel: UserViewModel = hiltViewModel()
            ObserveDeletion(backStackEntry) { userViewModel.removeSubmission(it) }
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
            ImageDetailRoute(
                onHome = { navController.popBackStack<Main>(inclusive = false) },
                onDeleted = navController.handleDeletion(),
                onBikeClick = { bikeQrId -> navController.navigate(Bike(bikeQrId)) }
            )
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
                side = route.side,
                onSideChanged = { newSide ->
                    navController.navigate(PhotoCapture(route.qrId, newSide)) {
                        popUpTo<PhotoCapture> { inclusive = true }
                    }
                },
                onPhotoCaptured = { uri ->
                    navController.navigate(PhotoPreview(route.qrId, uri.toString(), route.side)) {
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
            val photoUri = route.photoUri.toUri()
            val viewModel: PhotoPreviewViewModel = hiltViewModel()
            PhotoPreviewScreen(
                photoUri = photoUri,
                onConfirm = {
                    viewModel.upload(photoUri, route.qrId, route.side)
                    navController.popBackStack<Main>(inclusive = false)
                },
                onRetake = {
                    navController.navigate(PhotoCapture(route.qrId, route.side)) {
                        popUpTo<PhotoPreview> { inclusive = true }
                    }
                }
            )
        }
    }
}

private const val DELETED_SUBMISSION_KEY = "deletedSubmissionId"

@Composable
private fun ObserveDeletion(
    backStackEntry: NavBackStackEntry,
    onDeleted: (String) -> Unit
) {
    val deletedSubmissionId by backStackEntry.savedStateHandle
        .getStateFlow<String?>(DELETED_SUBMISSION_KEY, null)
        .collectAsStateWithLifecycle()
    LaunchedEffect(deletedSubmissionId) {
        deletedSubmissionId?.let { id ->
            onDeleted(id)
            backStackEntry.savedStateHandle[DELETED_SUBMISSION_KEY] = null
        }
    }
}

private fun NavController.handleDeletion(): (String) -> Unit = { submissionId ->
    previousBackStackEntry?.savedStateHandle?.set(DELETED_SUBMISSION_KEY, submissionId)
    popBackStack()
}

@Composable
private fun ImageDetailRoute(
    onHome: () -> Unit,
    onDeleted: (String) -> Unit,
    onBikeClick: ((String) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null
) {
    val viewModel: ImageDetailViewModel = hiltViewModel()
    val detailState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(detailState.isDeleted) {
        if (detailState.isDeleted) {
            onDeleted(viewModel.submissionId)
        }
    }

    when {
        detailState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        detailState.submission != null -> {
            ImageDetailScreen(
                submission = detailState.submission!!,
                onHome = onHome,
                onBikeClick = onBikeClick,
                onUserClick = onUserClick,
                isOwner = detailState.isOwner,
                isDeleting = detailState.isDeleting,
                onDelete = { viewModel.deleteSubmission() },
                onDownload = {
                    detailState.submission?.imageUrl?.let { url ->
                        val uri = imageUrlToUri(url)
                        val request = DownloadManager.Request(uri)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                "tcatb_${viewModel.submissionId}.jpg"
                            )
                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        dm.enqueue(request)
                    }
                },
                isLoggedIn = viewModel.isLoggedIn(),
                tags = detailState.tags,
                selectedTagId = detailState.selectedTagId,
                onTagTapped = { viewModel.selectTag(it) },
                isTagDeletable = { viewModel.isTagOwner(it) },
                onDeleteTag = { viewModel.deleteTag(it) },
                isTagMode = detailState.isTagMode,
                isCreatingTag = detailState.isCreatingTag,
                onEnterTagMode = { viewModel.enterTagMode() },
                onExitTagMode = { viewModel.exitTagMode() },
                onCreateTag = { file -> viewModel.createTag(file) },
                isProcessingMask = detailState.isProcessingMask,
                processedRing = detailState.processedRing,
                processedMaskWidth = detailState.processedMaskWidth,
                processedMaskHeight = detailState.processedMaskHeight,
                onProcessMask = { file -> viewModel.processMask(file) },
                onConfirmTag = { file -> viewModel.createTag(file) },
                onBackToDrawing = { viewModel.goBackToDrawing() },
            )
        }
        else -> {
            LaunchedEffect(Unit) { onHome() }
        }
    }
}
