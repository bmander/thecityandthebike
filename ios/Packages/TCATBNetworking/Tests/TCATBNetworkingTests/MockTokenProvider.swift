import Foundation
@testable import TCATBNetworking

final class MockTokenProvider: TokenProvider, @unchecked Sendable {
    var accessToken: String?
    var refreshToken: String?

    init(accessToken: String? = nil, refreshToken: String? = nil) {
        self.accessToken = accessToken
        self.refreshToken = refreshToken
    }

    func getAccessToken() async -> String? { accessToken }
    func getRefreshToken() async -> String? { refreshToken }
    func saveTokens(accessToken: String, refreshToken: String) async {
        self.accessToken = accessToken
        self.refreshToken = refreshToken
    }
    func clearTokens() async {
        accessToken = nil
        refreshToken = nil
    }
}
