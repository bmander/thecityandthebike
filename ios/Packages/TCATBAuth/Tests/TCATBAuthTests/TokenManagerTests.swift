import Testing
import Foundation
@testable import TCATBAuth

/// In-memory keychain substitute for testing TokenManager without hitting real Keychain Services.
final class MockKeychainStore: KeychainStore, @unchecked Sendable {
    private var store: [String: Data] = [:]

    @discardableResult
    func save(_ data: Data, for key: String) -> Bool {
        store[key] = data
        return true
    }

    func read(key: String) -> Data? {
        store[key]
    }

    @discardableResult
    func delete(_ key: String) -> Bool {
        store.removeValue(forKey: key)
        return true
    }
}

@Suite("TokenManager")
struct TokenManagerTests {

    @Test("saves and reads access token")
    func saveAndReadAccessToken() {
        let keychain = MockKeychainStore()
        let manager = TokenManager(keychain: keychain)

        manager.saveTokens(accessToken: "access123", refreshToken: "refresh456")

        #expect(manager.getAccessToken() == "access123")
        #expect(manager.getRefreshToken() == "refresh456")
    }

    @Test("hasToken returns false when no token saved")
    func hasTokenWhenEmpty() {
        let keychain = MockKeychainStore()
        let manager = TokenManager(keychain: keychain)

        #expect(manager.hasToken == false)
    }

    @Test("hasToken returns true after saving tokens")
    func hasTokenAfterSave() {
        let keychain = MockKeychainStore()
        let manager = TokenManager(keychain: keychain)

        manager.saveTokens(accessToken: "token", refreshToken: "refresh")
        #expect(manager.hasToken == true)
    }

    @Test("clearTokens removes all tokens")
    func clearTokens() {
        let keychain = MockKeychainStore()
        let manager = TokenManager(keychain: keychain)

        manager.saveTokens(accessToken: "access", refreshToken: "refresh")
        manager.clearTokens()

        #expect(manager.getAccessToken() == nil)
        #expect(manager.getRefreshToken() == nil)
        #expect(manager.hasToken == false)
    }

    @Test("getUserId decodes sub claim from JWT")
    func getUserIdFromJWT() {
        let keychain = MockKeychainStore()
        let manager = TokenManager(keychain: keychain)

        // JWT payload: {"sub": "user-123", "admin": false}
        // Base64URL of {"sub":"user-123","admin":false} = eyJzdWIiOiJ1c2VyLTEyMyIsImFkbWluIjpmYWxzZX0
        let token = "header.eyJzdWIiOiJ1c2VyLTEyMyIsImFkbWluIjpmYWxzZX0.signature"
        manager.saveTokens(accessToken: token, refreshToken: "refresh")

        #expect(manager.getUserId() == "user-123")
    }

    @Test("isAdmin decodes admin claim from JWT")
    func isAdminFromJWT() {
        let keychain = MockKeychainStore()
        let manager = TokenManager(keychain: keychain)

        // JWT payload: {"sub": "user-1", "admin": true}
        let token = "header.eyJzdWIiOiJ1c2VyLTEiLCJhZG1pbiI6dHJ1ZX0.signature"
        manager.saveTokens(accessToken: token, refreshToken: "refresh")

        #expect(manager.isAdmin() == true)
    }

    @Test("isAdmin returns false when no token")
    func isAdminWithoutToken() {
        let keychain = MockKeychainStore()
        let manager = TokenManager(keychain: keychain)

        #expect(manager.isAdmin() == false)
    }

    @Test("getUserId returns nil for invalid token")
    func getUserIdInvalidToken() {
        let keychain = MockKeychainStore()
        let manager = TokenManager(keychain: keychain)

        manager.saveTokens(accessToken: "not-a-jwt", refreshToken: "refresh")
        #expect(manager.getUserId() == nil)
    }
}
