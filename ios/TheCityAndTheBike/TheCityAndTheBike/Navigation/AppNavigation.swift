import SwiftUI
import TCATBAuth
import TCATBFeed
import TCATBBikes
import TCATBProfile

/// Primary navigation container using `NavigationStack` with a `NavigationPath`.
///
/// Mirrors the Android `AppNavGraph` composable. The root of the stack is
/// `MainTabView` (equivalent to the Android `Main` composable destination).
/// All other routes are pushed onto the stack via `.navigationDestination`.
struct AppNavigation: View {
    @Binding var path: NavigationPath
    var dependencies: AppDependencies

    var body: some View {
        NavigationStack(path: $path) {
            MainTabView(
                feedViewModel: dependencies.feedViewModel,
                bikesListViewModel: dependencies.bikesListViewModel,
                leaderboardViewModel: dependencies.leaderboardViewModel,
                authViewModel: dependencies.authViewModel,
                onNavigate: { route in
                    path.append(route)
                },
                onLogout: {
                    dependencies.authViewModel.logout()
                    path = NavigationPath()
                }
            )
            .navigationDestination(for: Route.self) { route in
                destination(for: route)
            }
        }
        .environment(\.imageBaseURL, dependencies.imageBaseURL)
    }

    @ViewBuilder
    private func destination(for route: Route) -> some View {
        switch route {
        case .about:
            AboutScreen()
        case .privacyCopyright:
            PrivacyCopyrightScreen()
        case .scoreRules:
            ScoreRulesScreen()
        case .login:
            LoginScreen(
                state: dependencies.authViewModel.state,
                onLogin: { username, password in
                    dependencies.authViewModel.login(username: username, password: password)
                },
                onNavigateToRegister: { path.append(Route.register) },
                onClearError: { dependencies.authViewModel.clearError() }
            )
            .onChange(of: dependencies.authViewModel.state.isLoggedIn) {
                if dependencies.authViewModel.state.isLoggedIn {
                    path = NavigationPath()
                }
            }
        case .register:
            RegisterScreen(
                state: dependencies.authViewModel.state,
                onRegister: { username, password in
                    dependencies.authViewModel.register(username: username, password: password)
                },
                onNavigateBack: { path.removeLast() },
                onClearError: { dependencies.authViewModel.clearError() },
                onClearRegistrationSuccess: { dependencies.authViewModel.clearRegistrationSuccess() }
            )
        case .imageDetail(let submissionId):
            imageDetailScreen(submissionId: submissionId)
        case .bikeDetail(let bikeQrId):
            bikeDetailScreen(bikeQrId: bikeQrId)
        case .bikeImageDetail(let submissionId):
            imageDetailScreen(submissionId: submissionId)
        case .tagDetail(let tagId):
            // TODO: Replace with TagScreen from TCATBFeed
            Text("Tag: \(tagId)")
                .navigationTitle("Tag")
        case .tagImageDetail(let submissionId):
            imageDetailScreen(submissionId: submissionId)
        case .user(let userId):
            userScreen(userId: userId)
        case .userImageDetail(let submissionId):
            imageDetailScreen(submissionId: submissionId)
        case .me:
            MeScreen(
                viewModel: MeViewModel(
                    userId: dependencies.tokenManager.getUserId(),
                    apiClient: dependencies.apiClient
                ),
                deleteAccountViewModel: DeleteAccountViewModel(apiClient: dependencies.apiClient),
                onBack: { path.removeLast() },
                onImageClick: { submissionId in
                    path.append(Route.meImageDetail(submissionId: submissionId))
                },
                onLogout: {
                    dependencies.authViewModel.logout()
                    path = NavigationPath()
                },
                onDeleted: {
                    dependencies.authViewModel.logout()
                    path = NavigationPath()
                }
            )
        case .meImageDetail(let submissionId):
            imageDetailScreen(submissionId: submissionId)
        case .qrScanner:
            // TODO: Replace with QrScannerScreen from TCATBCamera
            Text("QR Scanner")
                .navigationTitle("Scan")
        case .photoCapture(let qrId, let side):
            // TODO: Replace with PhotoCaptureScreen from TCATBCamera
            Text("Photo Capture: \(qrId) (\(side))")
                .navigationTitle("Capture")
        case .photoPreview(let qrId, _, let side):
            // TODO: Replace with PhotoPreviewScreen from TCATBCamera
            Text("Photo Preview: \(qrId) (\(side))")
                .navigationTitle("Preview")
        // Tab roots and pre-auth routes are not pushed onto the stack
        case .splash, .onboarding, .feed, .bikes, .leaderboard:
            EmptyView()
        }
    }

    private func bikeDetailScreen(bikeQrId: String) -> some View {
        let viewModel = BikeViewModel(
            bikeQrId: bikeQrId,
            apiClient: dependencies.apiClient
        )
        return BikeScreen(
            viewModel: viewModel,
            imageBaseURL: dependencies.imageBaseURL,
            onImageTapped: { submissionId in
                path.append(Route.bikeImageDetail(submissionId: submissionId))
            },
            onUserTapped: { userId in
                path.append(Route.user(userId: userId))
            },
            onBack: { path.removeLast() }
        )
    }

    private func userScreen(userId: String) -> some View {
        let viewModel = UserViewModel(
            userId: userId,
            apiClient: dependencies.apiClient,
            currentUserIsAdmin: dependencies.tokenManager.isAdmin()
        )
        return UserScreen(
            viewModel: viewModel,
            imageBaseURL: dependencies.imageBaseURL,
            onBack: { path.removeLast() },
            onImageClick: { submissionId in
                path.append(Route.userImageDetail(submissionId: submissionId))
            }
        )
    }

    private func imageDetailScreen(submissionId: String) -> some View {
        let viewModel = ImageDetailViewModel(
            submissionId: submissionId,
            apiClient: dependencies.apiClient,
            currentUserId: dependencies.tokenManager.getUserId(),
            isAdmin: dependencies.tokenManager.isAdmin()
        )
        return ImageDetailScreen(
            viewModel: viewModel,
            onHome: { path = NavigationPath() },
            onBikeClick: { bikeQrId in path.append(Route.bikeDetail(bikeQrId: bikeQrId)) },
            onUserClick: { userId in path.append(Route.user(userId: userId)) },
            onTagClick: { tagId in path.append(Route.tagDetail(tagId: tagId)) },
            onShowScoreRules: { path.append(Route.scoreRules) }
        )
    }
}
