import Foundation

public final class OnboardingPrefs: @unchecked Sendable {
    private static let completedKey = "onboarding_completed"

    private let defaults: UserDefaults

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    public var isOnboardingCompleted: Bool {
        defaults.bool(forKey: Self.completedKey)
    }

    public func setOnboardingCompleted() {
        defaults.set(true, forKey: Self.completedKey)
    }

    public func reset() {
        defaults.removeObject(forKey: Self.completedKey)
    }
}
