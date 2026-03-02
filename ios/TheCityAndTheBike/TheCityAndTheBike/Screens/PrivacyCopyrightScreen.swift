import SwiftUI

/// Legal info screen matching the Android `PrivacyCopyrightScreen` composable,
/// which wraps the shared `PrivacyCopyrightPage` content in a Scaffold with
/// a top app bar.
///
/// Displays privacy policy and copyright/licensing information. String content
/// matches the Android `strings.xml` values for the onboarding privacy/copyright
/// page, which is reused as the standalone screen.
struct PrivacyCopyrightScreen: View {
    @Environment(\.openURL) private var openURL

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.lg) {
                // Privacy section
                VStack(alignment: .leading, spacing: AppSpacing.xs) {
                    Text("Privacy")
                        .font(AppTypography.titleSmall)
                        .fontWeight(.semibold)

                    // swiftlint:disable:next line_length
                    Text("This app collects as little information about you as possible. We don't ask for your name or email. We don't track your location. When you upload a photo, we remove hidden details like the time it was taken. We only keep the date you uploaded it. We store an app-scoped device identifier to enforce bans and protect the service from abuse. It is not linked to any hardware serial number and cannot track you across other apps. That said, someone could still figure out who you are based on where the bikes are and what's visible in your photos. Keep that in mind.")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(.secondary)
                }

                // Copyright section
                VStack(alignment: .leading, spacing: AppSpacing.xs) {
                    Text("Copyright")
                        .font(AppTypography.titleSmall)
                        .fontWeight(.semibold)

                    // swiftlint:disable:next line_length
                    Text("The copyright of all photos you upload belong to you. By uploading the file you agree to license the image under Creative Commons BY-NC 4.0. This license requires that reusers give credit to the creator. It allows reusers to distribute, remix, adapt, and build upon the material in any medium or format, for noncommercial purposes only.")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(.secondary)

                    Button("Learn more about CC BY-NC 4.0") {
                        if let url = URL(string: "https://creativecommons.org/licenses/by-nc/4.0/") {
                            openURL(url)
                        }
                    }
                    .font(AppTypography.bodyMedium)
                    .accessibilityHint("Opens in browser")
                }
            }
            .padding(.horizontal, AppSpacing.xl)
            .padding(.vertical, AppSpacing.lg)
        }
        .navigationTitle("Privacy & Copyright")
        .navigationBarTitleDisplayMode(.inline)
    }
}
