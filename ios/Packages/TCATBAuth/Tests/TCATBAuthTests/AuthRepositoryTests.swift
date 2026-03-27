import Testing
import Foundation
@testable import TCATBAuth

@Suite("AuthRepository")
struct AuthRepositoryTests {

    private func makeRepository(
        apiClient: MockAuthAPIClient = MockAuthAPIClient()
    ) -> (AuthRepository, MockAuthAPIClient, MockKeychainStore) {
        let keychain = MockKeychainStore()
        let tokenManager = TokenManager(keychain: keychain)
        let repo = AuthRepository(apiClient: apiClient, tokenManager: tokenManager)
        return (repo, apiClient, keychain)
    }

    @Test("isLoggedIn is false when no tokens")
    func notLoggedInByDefault() {
        let (repo, _, _) = makeRepository()
        #expect(repo.isLoggedIn == false)
    }

    @Test("login stores tokens and marks logged in")
    func loginStoresTokens() async throws {
        let (repo, _, keychain) = makeRepository()

        try await repo.login(username: "user", password: "pass")

        #expect(repo.isLoggedIn == true)
        let tokenManager = TokenManager(keychain: keychain)
        #expect(tokenManager.getAccessToken() == "access")
        #expect(tokenManager.getRefreshToken() == "refresh")
    }

    @Test("register calls API without storing tokens")
    func registerDoesNotStoreTokens() async throws {
        let (repo, apiClient, _) = makeRepository()

        try await repo.register(username: "newuser", password: "password123")

        #expect(apiClient.registerCallCount == 1)
        #expect(repo.isLoggedIn == false)
    }

    @Test("logout clears tokens")
    func logoutClearsTokens() async throws {
        let (repo, _, _) = makeRepository()

        try await repo.login(username: "user", password: "pass")
        #expect(repo.isLoggedIn == true)

        await repo.logout()
        #expect(repo.isLoggedIn == false)
    }

    @Test("logout sends refresh token to API")
    func logoutCallsAPI() async throws {
        let (repo, apiClient, _) = makeRepository()

        try await repo.login(username: "user", password: "pass")
        await repo.logout()

        #expect(apiClient.logoutCallCount == 1)
    }

    @Test("logout still clears tokens when API fails")
    func logoutClearsTokensOnAPIFailure() async throws {
        let apiClient = MockAuthAPIClient()
        apiClient.logoutResult = .failure(AuthError.networkError("offline"))
        let (repo, _, _) = makeRepository(apiClient: apiClient)

        try await repo.login(username: "user", password: "pass")
        await repo.logout()

        #expect(repo.isLoggedIn == false)
    }

    @Test("refreshToken updates stored tokens")
    func refreshTokenUpdatesTokens() async throws {
        let (repo, _, keychain) = makeRepository()

        try await repo.login(username: "user", password: "pass")
        try await repo.refreshToken()

        let tokenManager = TokenManager(keychain: keychain)
        #expect(tokenManager.getAccessToken() == "new-access")
        #expect(tokenManager.getRefreshToken() == "new-refresh")
    }

    @Test("refreshToken throws when no refresh token exists")
    func refreshTokenThrowsWhenNoToken() async {
        let (repo, _, _) = makeRepository()

        do {
            try await repo.refreshToken()
            Issue.record("Expected AuthError.invalidCredentials")
        } catch {
            #expect(error as? AuthError == .invalidCredentials)
        }
    }
}
