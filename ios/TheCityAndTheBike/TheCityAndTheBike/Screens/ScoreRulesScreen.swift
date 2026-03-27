import SwiftUI

/// Score rules screen matching the Android `ScoreRulesScreen` composable.
///
/// Displays photo and tag point rules. The Android version reads rules from
/// `ScoreRules.photoRules` and `ScoreRules.tagRules` in the data model.
/// TODO: Wire up actual ScoreRules model from TCATBModels once available.
struct ScoreRulesScreen: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.lg) {
                // Photo Points section
                VStack(alignment: .leading, spacing: AppSpacing.xs) {
                    Text("Photo Points")
                        .font(AppTypography.headlineSmall)

                    // TODO: Replace with ScoreRules.photoRules from TCATBModels
                    Text("Score rules will appear here.")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(.secondary)

                    Text("A single photo can trigger several bonuses at once \u{2014} the points add up!")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(.secondary)
                }

                // Tag Points section
                VStack(alignment: .leading, spacing: AppSpacing.xs) {
                    Text("Tag Points")
                        .font(AppTypography.headlineSmall)

                    // TODO: Replace with ScoreRules.tagRules from TCATBModels
                    Text("Tag score rules will appear here.")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(AppSpacing.md)
        }
        .navigationTitle("Score Rules")
        .navigationBarTitleDisplayMode(.inline)
    }
}
