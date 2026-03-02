import Foundation

/// Hashable route enum matching the Android navigation routes from `Routes.kt`.
///
/// Each case corresponds to a `@Serializable` route object or data class
/// in the Android codebase. Routes that serve as tab roots (feed, leaderboard,
/// bikes) are not pushed onto the NavigationStack; they live inside MainTabView.
enum Route: Hashable {
    // Tab roots (handled by MainTabView, not pushed onto NavigationStack)
    case feed
    case leaderboard
    case bikes

    // Pre-auth flow
    case splash
    case onboarding

    // Auth
    case login
    case register

    // Submission detail (from various contexts)
    case imageDetail(submissionId: String)
    case bikeImageDetail(submissionId: String)
    case tagImageDetail(submissionId: String)
    case userImageDetail(submissionId: String)
    case meImageDetail(submissionId: String)

    // Entity screens
    case bikeDetail(bikeQrId: String)
    case tagDetail(tagId: String)
    case user(userId: String)
    case me

    // Camera flow
    case qrScanner
    case photoCapture(qrId: String, side: String = "right")
    case photoPreview(qrId: String, photoUri: String, side: String = "right")

    // Info screens
    case about
    case privacyCopyright
    case scoreRules
}
