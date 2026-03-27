import Foundation
import TCATBModels

public protocol AuthAPIClient: Sendable {
    func login(_ request: LoginRequest) async throws -> TokenResponse
    func register(_ request: RegisterRequest) async throws -> MessageResponse
    func refreshToken(_ request: RefreshRequest) async throws -> TokenResponse
    func logout(_ request: RefreshRequest) async throws -> MessageResponse
}
