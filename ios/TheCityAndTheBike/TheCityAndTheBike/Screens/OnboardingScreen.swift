import SwiftUI

/// First-run walkthrough matching the Android `OnboardingScreen` composable.
///
/// Displays the "What is this?" and "How do I do it?" sections with
/// numbered step rows, followed by a "Get Started" button. String content
/// matches the Android `strings.xml` values.
struct OnboardingScreen: View {
    let onFinished: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: AppSpacing.md) {
                    Spacer(minLength: AppSpacing.xl)

                    // "What is this?" section
                    Text("What is this?")
                        .font(AppTypography.headlineMedium)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)

                    Text("The City and The Bike is a community-built photo album of tags and street art on bike share vehicles in Seattle.")
                        .font(AppTypography.bodyLarge)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)

                    Spacer(minLength: AppSpacing.xl)

                    // "How do I do it?" section
                    Text("How do I do it?")
                        .font(AppTypography.headlineMedium)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)

                    Spacer(minLength: AppSpacing.xs)

                    VStack(spacing: AppSpacing.md) {
                        StepRow(number: 1, text: "Scan the bike QR code")
                        StepRow(number: 2, text: "Take a picture of the rear wheel")
                        StepRow(number: 3, text: "Try to catch them all")
                    }

                    Spacer(minLength: AppSpacing.xl)
                }
                .padding(.horizontal, AppSpacing.xl)
            }

            Button(action: onFinished) {
                Text("Get Started")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding(.horizontal, AppSpacing.xl)
            .padding(.vertical, AppSpacing.xxl)
            .accessibilityIdentifier("onboarding_get_started")
        }
    }
}

/// Numbered step row matching the Android `StepRow` composable.
/// Displays a circular badge with the step number followed by description text.
private struct StepRow: View {
    let number: Int
    let text: String

    var body: some View {
        HStack(spacing: AppSpacing.sm) {
            Text("\(number)")
                .font(AppTypography.labelSmall)
                .fontWeight(.bold)
                .foregroundStyle(.white)
                .frame(width: 24, height: 24)
                .background(Circle().fill(.tint))

            Text(text)
                .font(AppTypography.bodyLarge)

            Spacer()
        }
        .accessibilityElement(children: .combine)
    }
}
