import Foundation

struct AuthInterceptor: Sendable {
    private let tokenProvider: TokenProvider

    private static let unauthenticatedPaths = [
        "/auth/login",
        "/auth/register",
        "/auth/refresh",
    ]

    init(tokenProvider: TokenProvider) {
        self.tokenProvider = tokenProvider
    }

    func intercept(_ request: URLRequest) async -> URLRequest {
        let path = request.url?.path ?? ""
        for unauthPath in Self.unauthenticatedPaths {
            if path.contains(unauthPath) {
                return request
            }
        }

        guard let token = await tokenProvider.getAccessToken(), !token.isEmpty else {
            return request
        }

        var authenticatedRequest = request
        authenticatedRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        return authenticatedRequest
    }
}
