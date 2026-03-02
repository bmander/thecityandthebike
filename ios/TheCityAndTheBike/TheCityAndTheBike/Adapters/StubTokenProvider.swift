import TCATBNetworking

/// Minimal TokenProvider that returns nil for all tokens.
/// Feed is public and doesn't require authentication.
/// Replace with a real implementation when wiring TCATBAuth.
struct StubTokenProvider: TokenProvider {
    func getAccessToken() async -> String? { nil }
    func getRefreshToken() async -> String? { nil }
    func saveTokens(accessToken: String, refreshToken: String) async {}
    func clearTokens() async {}
}
