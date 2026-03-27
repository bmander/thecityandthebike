import Foundation

public final class TokenManager: Sendable {
    private static let accessTokenKey = "auth_token"
    private static let refreshTokenKey = "refresh_token"

    private let keychain: KeychainStore

    public init(keychain: KeychainStore = KeychainHelper()) {
        self.keychain = keychain
    }

    public func saveTokens(accessToken: String, refreshToken: String) {
        if let accessData = accessToken.data(using: .utf8) {
            keychain.save(accessData, for: Self.accessTokenKey)
        }
        if let refreshData = refreshToken.data(using: .utf8) {
            keychain.save(refreshData, for: Self.refreshTokenKey)
        }
    }

    public func getAccessToken() -> String? {
        guard let data = keychain.read(key: Self.accessTokenKey) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    public func getRefreshToken() -> String? {
        guard let data = keychain.read(key: Self.refreshTokenKey) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    public func clearTokens() {
        keychain.delete(Self.accessTokenKey)
        keychain.delete(Self.refreshTokenKey)
    }

    public var hasToken: Bool {
        getAccessToken() != nil
    }

    public func getUserId() -> String? {
        guard let token = getAccessToken() else { return nil }
        return decodeJWTClaim(token: token, claim: "sub")
    }

    public func isAdmin() -> Bool {
        guard let token = getAccessToken() else { return false }
        return decodeJWTBoolClaim(token: token, claim: "admin")
    }

    private func decodeJWTClaim(token: String, claim: String) -> String? {
        guard let payload = decodeJWTPayload(token: token) else { return nil }
        return payload[claim] as? String
    }

    private func decodeJWTBoolClaim(token: String, claim: String) -> Bool {
        guard let payload = decodeJWTPayload(token: token) else { return false }
        return payload[claim] as? Bool ?? false
    }

    private func decodeJWTPayload(token: String) -> [String: Any]? {
        let parts = token.split(separator: ".")
        guard parts.count >= 2 else { return nil }
        var base64 = String(parts[1])
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        let remainder = base64.count % 4
        if remainder > 0 {
            base64 += String(repeating: "=", count: 4 - remainder)
        }
        guard let data = Data(base64Encoded: base64),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        return json
    }
}
