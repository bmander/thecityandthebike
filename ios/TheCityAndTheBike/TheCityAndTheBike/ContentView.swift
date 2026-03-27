import SwiftUI

/// Root view that manages the app lifecycle flow:
/// splash -> onboarding (if first run) -> main content.
///
/// Mirrors the Android `AppNavGraph` start-destination logic where
/// `Splash` transitions to either `Onboarding` or `Main` based on
/// `OnboardingPrefs.isOnboardingCompleted()`.
struct ContentView: View {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var isShowingSplash = true
    @State private var navigationPath = NavigationPath()
    @State private var dependencies = AppDependencies()

    var body: some View {
        Group {
            if isShowingSplash {
                SplashScreen {
                    withAnimation {
                        isShowingSplash = false
                    }
                }
            } else if !hasCompletedOnboarding {
                OnboardingScreen {
                    withAnimation {
                        hasCompletedOnboarding = true
                    }
                }
            } else {
                AppNavigation(path: $navigationPath, dependencies: dependencies)
            }
        }
    }
}
