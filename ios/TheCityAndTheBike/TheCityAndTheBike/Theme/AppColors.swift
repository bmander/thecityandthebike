import SwiftUI

/// Color palette ported from Android `Color.kt`.
///
/// All hex values match the Material 3 color scheme generated from
/// seed color #E33419 used in the Android app.
struct AppColors {
    // MARK: - Light Theme (seed: #E33419)

    static let lightPrimary = Color(red: 0xB5 / 255, green: 0x20 / 255, blue: 0x00 / 255)
    static let lightOnPrimary = Color.white
    static let lightPrimaryContainer = Color(red: 0xFF / 255, green: 0xDA / 255, blue: 0xD2 / 255)
    static let lightOnPrimaryContainer = Color(red: 0x3E / 255, green: 0x05 / 255, blue: 0x00 / 255)
    static let lightSecondary = Color(red: 0x77 / 255, green: 0x57 / 255, blue: 0x4E / 255)
    static let lightOnSecondary = Color.white
    static let lightSecondaryContainer = Color(red: 0xFF / 255, green: 0xDA / 255, blue: 0xD2 / 255)
    static let lightOnSecondaryContainer = Color(red: 0x2C / 255, green: 0x15 / 255, blue: 0x0F / 255)
    static let lightTertiary = Color(red: 0x6C / 255, green: 0x5D / 255, blue: 0x2F / 255)
    static let lightOnTertiary = Color.white
    static let lightTertiaryContainer = Color(red: 0xF6 / 255, green: 0xE1 / 255, blue: 0xA6 / 255)
    static let lightOnTertiaryContainer = Color(red: 0x23 / 255, green: 0x1B / 255, blue: 0x00 / 255)
    static let lightError = Color(red: 0xBA / 255, green: 0x1A / 255, blue: 0x1A / 255)
    static let lightOnError = Color.white
    static let lightErrorContainer = Color(red: 0xFF / 255, green: 0xDA / 255, blue: 0xD6 / 255)
    static let lightOnErrorContainer = Color(red: 0x41 / 255, green: 0x00 / 255, blue: 0x02 / 255)
    static let lightBackground = Color(red: 0xFF / 255, green: 0xFB / 255, blue: 0xFF / 255)
    static let lightOnBackground = Color(red: 0x20 / 255, green: 0x1A / 255, blue: 0x18 / 255)
    static let lightSurface = Color(red: 0xFF / 255, green: 0xFB / 255, blue: 0xFF / 255)
    static let lightOnSurface = Color(red: 0x20 / 255, green: 0x1A / 255, blue: 0x18 / 255)
    static let lightSurfaceVariant = Color(red: 0xF5 / 255, green: 0xDD / 255, blue: 0xD7 / 255)
    static let lightOnSurfaceVariant = Color(red: 0x53 / 255, green: 0x43 / 255, blue: 0x3F / 255)
    static let lightOutline = Color(red: 0x85 / 255, green: 0x73 / 255, blue: 0x6E / 255)
    static let lightInverseSurface = Color(red: 0x36 / 255, green: 0x2F / 255, blue: 0x2D / 255)
    static let lightInverseOnSurface = Color(red: 0xFB / 255, green: 0xEE / 255, blue: 0xEB / 255)
    static let lightInversePrimary = Color(red: 0xFF / 255, green: 0xB4 / 255, blue: 0xA1 / 255)
    static let lightDestructiveAction = Color(red: 0xBA / 255, green: 0x1A / 255, blue: 0x1A / 255)
    static let lightOnDestructiveAction = Color.white

    // MARK: - Dark Theme (seed: #E33419)

    static let darkPrimary = Color(red: 0xFF / 255, green: 0xB4 / 255, blue: 0xA1 / 255)
    static let darkOnPrimary = Color(red: 0x64 / 255, green: 0x0E / 255, blue: 0x00 / 255)
    static let darkPrimaryContainer = Color(red: 0x8C / 255, green: 0x17 / 255, blue: 0x00 / 255)
    static let darkOnPrimaryContainer = Color(red: 0xFF / 255, green: 0xDA / 255, blue: 0xD2 / 255)
    static let darkSecondary = Color(red: 0xE7 / 255, green: 0xBD / 255, blue: 0xB3 / 255)
    static let darkOnSecondary = Color(red: 0x44 / 255, green: 0x2A / 255, blue: 0x22 / 255)
    static let darkSecondaryContainer = Color(red: 0x5D / 255, green: 0x3F / 255, blue: 0x37 / 255)
    static let darkOnSecondaryContainer = Color(red: 0xFF / 255, green: 0xDA / 255, blue: 0xD2 / 255)
    static let darkTertiary = Color(red: 0xD9 / 255, green: 0xC5 / 255, blue: 0x8D / 255)
    static let darkOnTertiary = Color(red: 0x3B / 255, green: 0x2F / 255, blue: 0x05 / 255)
    static let darkTertiaryContainer = Color(red: 0x53 / 255, green: 0x46 / 255, blue: 0x1A / 255)
    static let darkOnTertiaryContainer = Color(red: 0xF6 / 255, green: 0xE1 / 255, blue: 0xA6 / 255)
    static let darkError = Color(red: 0xFF / 255, green: 0xB4 / 255, blue: 0xAB / 255)
    static let darkOnError = Color(red: 0x69 / 255, green: 0x00 / 255, blue: 0x05 / 255)
    static let darkErrorContainer = Color(red: 0x93 / 255, green: 0x00 / 255, blue: 0x0A / 255)
    static let darkOnErrorContainer = Color(red: 0xFF / 255, green: 0xDA / 255, blue: 0xD6 / 255)
    static let darkBackground = Color(red: 0x20 / 255, green: 0x1A / 255, blue: 0x18 / 255)
    static let darkOnBackground = Color(red: 0xED / 255, green: 0xE0 / 255, blue: 0xDC / 255)
    static let darkSurface = Color(red: 0x20 / 255, green: 0x1A / 255, blue: 0x18 / 255)
    static let darkOnSurface = Color(red: 0xED / 255, green: 0xE0 / 255, blue: 0xDC / 255)
    static let darkSurfaceVariant = Color(red: 0x53 / 255, green: 0x43 / 255, blue: 0x3F / 255)
    static let darkOnSurfaceVariant = Color(red: 0xD8 / 255, green: 0xC2 / 255, blue: 0xBB / 255)
    static let darkOutline = Color(red: 0xA0 / 255, green: 0x8C / 255, blue: 0x87 / 255)
    static let darkInverseSurface = Color(red: 0xED / 255, green: 0xE0 / 255, blue: 0xDC / 255)
    static let darkInverseOnSurface = Color(red: 0x36 / 255, green: 0x2F / 255, blue: 0x2D / 255)
    static let darkInversePrimary = Color(red: 0xB5 / 255, green: 0x20 / 255, blue: 0x00 / 255)
    static let darkDestructiveAction = Color(red: 0xFF / 255, green: 0xB4 / 255, blue: 0xAB / 255)
    static let darkOnDestructiveAction = Color(red: 0x69 / 255, green: 0x00 / 255, blue: 0x05 / 255)
}

// MARK: - Environment-based color scheme

/// Semantic color scheme mirroring the Android `MaterialTheme.colorScheme` and
/// `ExtendedColorScheme` from `Theme.kt`. Injected via SwiftUI environment
/// so views can access theme colors without knowing the current appearance.
struct AppColorScheme {
    let primary: Color
    let onPrimary: Color
    let primaryContainer: Color
    let onPrimaryContainer: Color
    let secondary: Color
    let onSecondary: Color
    let secondaryContainer: Color
    let onSecondaryContainer: Color
    let tertiary: Color
    let onTertiary: Color
    let tertiaryContainer: Color
    let onTertiaryContainer: Color
    let error: Color
    let onError: Color
    let errorContainer: Color
    let onErrorContainer: Color
    let background: Color
    let onBackground: Color
    let surface: Color
    let onSurface: Color
    let surfaceVariant: Color
    let onSurfaceVariant: Color
    let outline: Color
    let inverseSurface: Color
    let inverseOnSurface: Color
    let inversePrimary: Color
    let destructiveAction: Color
    let onDestructiveAction: Color

    static let light = AppColorScheme(
        primary: AppColors.lightPrimary,
        onPrimary: AppColors.lightOnPrimary,
        primaryContainer: AppColors.lightPrimaryContainer,
        onPrimaryContainer: AppColors.lightOnPrimaryContainer,
        secondary: AppColors.lightSecondary,
        onSecondary: AppColors.lightOnSecondary,
        secondaryContainer: AppColors.lightSecondaryContainer,
        onSecondaryContainer: AppColors.lightOnSecondaryContainer,
        tertiary: AppColors.lightTertiary,
        onTertiary: AppColors.lightOnTertiary,
        tertiaryContainer: AppColors.lightTertiaryContainer,
        onTertiaryContainer: AppColors.lightOnTertiaryContainer,
        error: AppColors.lightError,
        onError: AppColors.lightOnError,
        errorContainer: AppColors.lightErrorContainer,
        onErrorContainer: AppColors.lightOnErrorContainer,
        background: AppColors.lightBackground,
        onBackground: AppColors.lightOnBackground,
        surface: AppColors.lightSurface,
        onSurface: AppColors.lightOnSurface,
        surfaceVariant: AppColors.lightSurfaceVariant,
        onSurfaceVariant: AppColors.lightOnSurfaceVariant,
        outline: AppColors.lightOutline,
        inverseSurface: AppColors.lightInverseSurface,
        inverseOnSurface: AppColors.lightInverseOnSurface,
        inversePrimary: AppColors.lightInversePrimary,
        destructiveAction: AppColors.lightDestructiveAction,
        onDestructiveAction: AppColors.lightOnDestructiveAction
    )

    static let dark = AppColorScheme(
        primary: AppColors.darkPrimary,
        onPrimary: AppColors.darkOnPrimary,
        primaryContainer: AppColors.darkPrimaryContainer,
        onPrimaryContainer: AppColors.darkOnPrimaryContainer,
        secondary: AppColors.darkSecondary,
        onSecondary: AppColors.darkOnSecondary,
        secondaryContainer: AppColors.darkSecondaryContainer,
        onSecondaryContainer: AppColors.darkOnSecondaryContainer,
        tertiary: AppColors.darkTertiary,
        onTertiary: AppColors.darkOnTertiary,
        tertiaryContainer: AppColors.darkTertiaryContainer,
        onTertiaryContainer: AppColors.darkOnTertiaryContainer,
        error: AppColors.darkError,
        onError: AppColors.darkOnError,
        errorContainer: AppColors.darkErrorContainer,
        onErrorContainer: AppColors.darkOnErrorContainer,
        background: AppColors.darkBackground,
        onBackground: AppColors.darkOnBackground,
        surface: AppColors.darkSurface,
        onSurface: AppColors.darkOnSurface,
        surfaceVariant: AppColors.darkSurfaceVariant,
        onSurfaceVariant: AppColors.darkOnSurfaceVariant,
        outline: AppColors.darkOutline,
        inverseSurface: AppColors.darkInverseSurface,
        inverseOnSurface: AppColors.darkInverseOnSurface,
        inversePrimary: AppColors.darkInversePrimary,
        destructiveAction: AppColors.darkDestructiveAction,
        onDestructiveAction: AppColors.darkOnDestructiveAction
    )
}

// MARK: - Environment key

private struct AppColorSchemeKey: EnvironmentKey {
    static let defaultValue = AppColorScheme.light
}

extension EnvironmentValues {
    var appColorScheme: AppColorScheme {
        get { self[AppColorSchemeKey.self] }
        set { self[AppColorSchemeKey.self] = newValue }
    }
}
