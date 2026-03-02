// INTEGRATION: Move to TCATBModels
// Local copies of auth model types for standalone package development.
// Once integrated into the full app, import these from TCATBModels instead.

import Foundation

public struct LoginRequest: Codable, Sendable {
    public let username: String
    public let password: String
    public let deviceId: String?

    public init(username: String, password: String, deviceId: String? = nil) {
        self.username = username
        self.password = password
        self.deviceId = deviceId
    }

    enum CodingKeys: String, CodingKey {
        case username
        case password
        case deviceId = "android_id"
    }
}

public struct RegisterRequest: Codable, Sendable {
    public let username: String
    public let password: String
    public let deviceId: String?

    public init(username: String, password: String, deviceId: String? = nil) {
        self.username = username
        self.password = password
        self.deviceId = deviceId
    }

    enum CodingKeys: String, CodingKey {
        case username
        case password
        case deviceId = "android_id"
    }
}

public struct TokenResponse: Codable, Sendable {
    public let accessToken: String
    public let refreshToken: String
    public let tokenType: String

    public init(accessToken: String, refreshToken: String, tokenType: String = "bearer") {
        self.accessToken = accessToken
        self.refreshToken = refreshToken
        self.tokenType = tokenType
    }

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case refreshToken = "refresh_token"
        case tokenType = "token_type"
    }
}

public struct RefreshRequest: Codable, Sendable {
    public let refreshToken: String

    public init(refreshToken: String) {
        self.refreshToken = refreshToken
    }

    enum CodingKeys: String, CodingKey {
        case refreshToken = "refresh_token"
    }
}

public struct MessageResponse: Codable, Sendable {
    public let msg: String

    public init(msg: String) {
        self.msg = msg
    }
}
