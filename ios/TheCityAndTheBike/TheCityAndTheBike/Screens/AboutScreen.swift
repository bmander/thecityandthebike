import SwiftUI

/// App info screen matching the Android `AboutScreen` composable.
///
/// Displays the app name, attribution, contact email, GitHub link,
/// and version info. Uses `openURL` for external links, mirroring
/// the Android `Intent.ACTION_SENDTO` / `Intent.ACTION_VIEW` intents.
struct AboutScreen: View {
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("The City and The Bike")
                .font(AppTypography.headlineMedium)

            Spacer().frame(height: AppSpacing.xs)

            Text("An art project by Brandon Martin-Anderson")
                .font(AppTypography.bodyLarge)

            Spacer().frame(height: AppSpacing.md)

            Button("thecityandthebike@gmail.com") {
                if let url = URL(string: "mailto:thecityandthebike@gmail.com") {
                    openURL(url)
                }
            }
            .font(AppTypography.bodyLarge)

            Spacer().frame(height: AppSpacing.xs)

            Button("github.com/bmander/thecityandthebike") {
                if let url = URL(string: "https://github.com/bmander/thecityandthebike") {
                    openURL(url)
                }
            }
            .font(AppTypography.bodyLarge)

            Spacer().frame(height: AppSpacing.md)

            let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
            let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
            Text("\(version) (\(build))")
                .font(AppTypography.bodyMedium)
                .foregroundStyle(.secondary)

            Spacer()
        }
        .padding(AppSpacing.md)
        .navigationTitle("About")
        .navigationBarTitleDisplayMode(.inline)
    }
}
