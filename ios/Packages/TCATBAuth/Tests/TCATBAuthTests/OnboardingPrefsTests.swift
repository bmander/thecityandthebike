import Testing
import Foundation
@testable import TCATBAuth

@Suite("OnboardingPrefs")
struct OnboardingPrefsTests {

    private func makeSuiteName() -> String {
        "com.thecityandthebike.test.\(UUID().uuidString)"
    }

    @Test("defaults to not completed")
    func defaultsToNotCompleted() {
        let defaults = UserDefaults(suiteName: makeSuiteName())!
        let prefs = OnboardingPrefs(defaults: defaults)
        #expect(prefs.isOnboardingCompleted == false)
    }

    @Test("marks onboarding as completed")
    func markCompleted() {
        let defaults = UserDefaults(suiteName: makeSuiteName())!
        let prefs = OnboardingPrefs(defaults: defaults)

        prefs.setOnboardingCompleted()
        #expect(prefs.isOnboardingCompleted == true)
    }

    @Test("reset clears completion state")
    func resetClearsState() {
        let defaults = UserDefaults(suiteName: makeSuiteName())!
        let prefs = OnboardingPrefs(defaults: defaults)

        prefs.setOnboardingCompleted()
        prefs.reset()
        #expect(prefs.isOnboardingCompleted == false)
    }
}
