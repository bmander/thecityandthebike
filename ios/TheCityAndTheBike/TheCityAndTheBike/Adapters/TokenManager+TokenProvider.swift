import TCATBAuth
import TCATBNetworking

struct TokenManagerTokenProvider: TokenProvider {
    private let tokenManager: TokenManager

    init(tokenManager: TokenManager) {
        self.tokenManager = tokenManager
    }

    func getAccessToken() async -> String? {
        tokenManager.getAccessToken()
    }

    func getRefreshToken() async -> String? {
        tokenManager.getRefreshToken()
    }

    func saveTokens(accessToken: String, refreshToken: String) async {
        tokenManager.saveTokens(accessToken: accessToken, refreshToken: refreshToken)
    }

    func clearTokens() async {
        tokenManager.clearTokens()
    }
}
