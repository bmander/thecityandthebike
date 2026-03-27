import SwiftUI

/// Splash screen shown for 2 seconds on app launch, matching the Android
/// `SplashScreen` composable which displays a full-bleed splash image
/// with a 2000ms delay before calling `onTimeout`.
struct SplashScreen: View {
    let onTimeout: () -> Void

    var body: some View {
        Image("Splash")
            .resizable()
            .scaledToFill()
            .ignoresSafeArea()
            .task {
                try? await Task.sleep(for: .seconds(2))
                onTimeout()
            }
    }
}
