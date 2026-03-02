import SwiftUI
import TCATBBikes
import TCATBFeed

/// The three main tabs, matching the Android `MainTab` enum in `MainScreen.kt`.
enum MainTab: String, CaseIterable {
    case feed = "Feed"
    case leaderboard = "Leaderboard"
    case bikes = "Bikes"

    var systemImage: String {
        switch self {
        case .feed: "photo.on.rectangle"
        case .leaderboard: "trophy"
        case .bikes: "bicycle"
        }
    }
}

/// Tab-based main content view matching the Android `MainScreen` composable
/// with its `HorizontalPager` of Feed, Leaderboard, and Bikes tabs.
///
/// Includes a toolbar menu (matching the Android `MenuButton` / `DropdownMenu`)
/// with About, Score Rules, Privacy & Copyright, Me (if logged in), and Log out.
/// Also shows a Camera FAB when logged in or a Login FAB when not, matching
/// the Android `CameraFAB` / `LoginFAB` overlay.
struct MainTabView: View {
    @State private var selectedTab: MainTab = .feed
    var feedViewModel: MainViewModel
    var bikesListViewModel: BikesListViewModel
    // TODO: Wire up actual auth state from TCATBAuth
    var isLoggedIn: Bool = false
    var onNavigate: ((Route) -> Void)?
    var onLogout: (() -> Void)?

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                FeedScreen(
                    viewModel: feedViewModel,
                    onImageClick: { id in onNavigate?(.imageDetail(submissionId: id)) },
                    onShowScoreRules: { onNavigate?(.scoreRules) }
                )
                    .tabItem {
                        Label(MainTab.feed.rawValue, systemImage: MainTab.feed.systemImage)
                    }
                    .tag(MainTab.feed)

                // TODO: Replace with LeaderboardScreen from TCATBProfile
                Text("Leaderboard")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .tabItem {
                        Label(MainTab.leaderboard.rawValue, systemImage: MainTab.leaderboard.systemImage)
                    }
                    .tag(MainTab.leaderboard)

                BikesContent(
                    viewModel: bikesListViewModel,
                    onBikeTapped: { bikeQrId in onNavigate?(.bikeDetail(bikeQrId: bikeQrId)) }
                )
                    .tabItem {
                        Label(MainTab.bikes.rawValue, systemImage: MainTab.bikes.systemImage)
                    }
                    .tag(MainTab.bikes)
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("About") {
                            onNavigate?(.about)
                        }
                        Button("Score Rules") {
                            onNavigate?(.scoreRules)
                        }
                        Button("Privacy & Copyright") {
                            onNavigate?(.privacyCopyright)
                        }
                        if isLoggedIn {
                            Button("Me") {
                                onNavigate?(.me)
                            }
                            Button("Log out", role: .destructive) {
                                onLogout?()
                            }
                        }
                    } label: {
                        Image(systemName: "line.3.horizontal")
                            .imageScale(.large)
                    }
                }
            }

            // Camera FAB (logged in) or Login FAB (logged out),
            // matching the Android bottom-center overlay
            if isLoggedIn {
                Button {
                    onNavigate?(.qrScanner)
                } label: {
                    Image(systemName: "camera.fill")
                        .font(.title2)
                        .foregroundStyle(.white)
                        .frame(width: 56, height: 56)
                        .background(Circle().fill(.tint))
                        .shadow(radius: 4, y: 2)
                }
                .padding(.bottom, AppSpacing.md)
                .accessibilityLabel("Scan QR code")
            } else {
                Button {
                    onNavigate?(.login)
                } label: {
                    Label("Log in", systemImage: "person.crop.circle")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .padding(.horizontal, AppSpacing.lg)
                        .padding(.vertical, AppSpacing.sm)
                        .background(Capsule().fill(.tint))
                        .shadow(radius: 4, y: 2)
                }
                .padding(.bottom, AppSpacing.md)
            }
        }
    }
}
