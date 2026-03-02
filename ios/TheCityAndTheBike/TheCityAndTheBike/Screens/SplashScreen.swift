import SwiftUI

/// Splash screen shown for 2 seconds on app launch, matching the Android
/// `SplashScreen` composable which displays a full-bleed splash image
/// with a 2000ms delay before calling `onTimeout`.
struct SplashScreen: View {
    let onTimeout: () -> Void

    var body: some View {
        ZStack {
            // TODO: Replace with actual splash image from assets
            // Android uses R.drawable.splash with ContentScale.Crop
            Color(red: 0xB5 / 255, green: 0x20 / 255, blue: 0x00 / 255)
                .ignoresSafeArea()

            VStack(spacing: AppSpacing.md) {
                Text("The City\nand\nThe Bike")
                    .font(AppTypography.displayMedium)
                    .fontWeight(.bold)
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
            }
        }
        .task {
            try? await Task.sleep(for: .seconds(2))
            onTimeout()
        }
    }
}
