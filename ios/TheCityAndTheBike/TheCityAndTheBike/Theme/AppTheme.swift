import SwiftUI

/// Spacing constants matching Material 3 spacing conventions used
/// throughout the Android app.
enum AppSpacing {
    static let xxs: CGFloat = 4
    static let xs: CGFloat = 8
    static let sm: CGFloat = 12
    static let md: CGFloat = 16
    static let lg: CGFloat = 24
    static let xl: CGFloat = 32
    static let xxl: CGFloat = 48
}

/// Typography scale matching Material 3 type scale from the Android
/// `MaterialTheme.typography` used in `Theme.kt`.
enum AppTypography {
    static let displayLarge = Font.system(size: 57, weight: .regular)
    static let displayMedium = Font.system(size: 45, weight: .regular)
    static let displaySmall = Font.system(size: 36, weight: .regular)

    static let headlineLarge = Font.system(size: 32, weight: .regular)
    static let headlineMedium = Font.system(size: 28, weight: .regular)
    static let headlineSmall = Font.system(size: 24, weight: .regular)

    static let titleLarge = Font.system(size: 22, weight: .regular)
    static let titleMedium = Font.system(size: 16, weight: .medium)
    static let titleSmall = Font.system(size: 14, weight: .medium)

    static let bodyLarge = Font.system(size: 16, weight: .regular)
    static let bodyMedium = Font.system(size: 14, weight: .regular)
    static let bodySmall = Font.system(size: 12, weight: .regular)

    static let labelLarge = Font.system(size: 14, weight: .medium)
    static let labelMedium = Font.system(size: 12, weight: .medium)
    static let labelSmall = Font.system(size: 11, weight: .medium)
}

/// View modifier that injects the app color scheme and tint into
/// the environment, mirroring the `TheCityAndTheBikeTheme` composable
/// from the Android `Theme.kt`.
struct AppTheme: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme

    func body(content: Content) -> some View {
        content
            .environment(\.appColorScheme, colorScheme == .dark ? .dark : .light)
            .tint(colorScheme == .dark ? AppColors.darkPrimary : AppColors.lightPrimary)
    }
}

extension View {
    func appTheme() -> some View {
        modifier(AppTheme())
    }
}
