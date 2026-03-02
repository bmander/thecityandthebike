import Foundation

/// Protocol for providing authentication tokens.
/// The auth package will provide the concrete implementation.
public protocol TokenProvider: Sendable {
    func getAccessToken() async -> String?
    func getRefreshToken() async -> String?
    func saveTokens(accessToken: String, refreshToken: String) async
    func clearTokens() async
}
