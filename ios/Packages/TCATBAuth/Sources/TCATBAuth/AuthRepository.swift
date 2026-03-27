import Foundation
import TCATBModels

public enum AuthError: Error, Equatable {
    case invalidCredentials
    case registrationFailed(String)
    case rateLimited(retryAfter: Int?)
    case banned(String)
    case networkError(String)
    case unknown(String)

    public var displayMessage: String {
        switch self {
        case .invalidCredentials:
            return "Invalid credentials"
        case .registrationFailed(let message):
            return message
        case .rateLimited:
            return "Too many attempts. Please try again later."
        case .banned(let message):
            return message
        case .networkError:
            return "Network error. Check your connection and try again."
        case .unknown:
            return "An unexpected error occurred."
        }
    }
}

public final class AuthRepository: Sendable {
    private let apiClient: AuthAPIClient
    private let tokenManager: TokenManager

    public init(apiClient: AuthAPIClient, tokenManager: TokenManager) {
        self.apiClient = apiClient
        self.tokenManager = tokenManager
    }

    public var isLoggedIn: Bool {
        tokenManager.hasToken
    }

    public func login(username: String, password: String, deviceId: String? = nil) async throws {
        let request = LoginRequest(username: username, password: password, deviceId: deviceId)
        let response = try await apiClient.login(request)
        tokenManager.saveTokens(accessToken: response.accessToken, refreshToken: response.refreshToken)
    }

    public func register(username: String, password: String, deviceId: String? = nil) async throws {
        let request = RegisterRequest(username: username, password: password, deviceId: deviceId)
        _ = try await apiClient.register(request)
    }

    public func logout() async {
        if let refreshToken = tokenManager.getRefreshToken() {
            let request = RefreshRequest(refreshToken: refreshToken)
            _ = try? await apiClient.logout(request)
        }
        tokenManager.clearTokens()
    }

    public func refreshToken() async throws {
        guard let refreshToken = tokenManager.getRefreshToken() else {
            tokenManager.clearTokens()
            throw AuthError.invalidCredentials
        }
        let request = RefreshRequest(refreshToken: refreshToken)
        let response = try await apiClient.refreshToken(request)
        tokenManager.saveTokens(accessToken: response.accessToken, refreshToken: response.refreshToken)
    }
}
