import SwiftUI
import TCATBFeed

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
                onNavigate: { route in
                    path.append(route)
                }
            )
            .environment(\.imageBaseURL, dependencies.imageBaseURL)
            .navigationDestination(for: Route.self) { route in
                destination(for: route)
            }
        }
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
            // TODO: Replace with LoginScreen from TCATBAuth
            Text("Login")
                .navigationTitle("Login")
        case .register:
            // TODO: Replace with RegisterScreen from TCATBAuth
            Text("Register")
                .navigationTitle("Register")
        case .imageDetail(let submissionId):
            // TODO: Replace with ImageDetailScreen from TCATBFeed
            Text("Image Detail: \(submissionId)")
                .navigationTitle("Detail")
        case .bikeDetail(let bikeQrId):
            // TODO: Replace with BikeDetailScreen from TCATBBikes
            Text("Bike: \(bikeQrId)")
                .navigationTitle("Bike")
        case .bikeImageDetail(let submissionId):
            // TODO: Replace with ImageDetailScreen (bike context)
            Text("Bike Image Detail: \(submissionId)")
                .navigationTitle("Detail")
        case .tagDetail(let tagId):
            // TODO: Replace with TagScreen from TCATBFeed
            Text("Tag: \(tagId)")
                .navigationTitle("Tag")
        case .tagImageDetail(let submissionId):
            // TODO: Replace with ImageDetailScreen (tag context)
            Text("Tag Image Detail: \(submissionId)")
                .navigationTitle("Detail")
        case .user(let userId):
            // TODO: Replace with UserScreen from TCATBProfile
            Text("User: \(userId)")
                .navigationTitle("User")
        case .userImageDetail(let submissionId):
            // TODO: Replace with ImageDetailScreen (user context)
            Text("User Image Detail: \(submissionId)")
                .navigationTitle("Detail")
        case .me:
            // TODO: Replace with MeScreen from TCATBProfile
            Text("My Profile")
                .navigationTitle("Me")
        case .meImageDetail(let submissionId):
            // TODO: Replace with ImageDetailScreen (me context)
            Text("My Image Detail: \(submissionId)")
                .navigationTitle("Detail")
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
}
