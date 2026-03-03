import Foundation
import TCATBAuth
import TCATBModels
import TCATBNetworking

extension APIClient: @retroactive AuthAPIClient {
    public func login(_ request: LoginRequest) async throws -> TokenResponse {
        try await login(username: request.username, password: request.password, deviceId: request.deviceId)
    }

    public func register(_ request: RegisterRequest) async throws -> MessageResponse {
        try await register(username: request.username, password: request.password, deviceId: request.deviceId)
    }

    public func refreshToken(_ request: RefreshRequest) async throws -> TokenResponse {
        try await refreshToken(refreshToken: request.refreshToken)
    }

    public func logout(_ request: RefreshRequest) async throws -> MessageResponse {
        try await logout(refreshToken: request.refreshToken)
    }
}
