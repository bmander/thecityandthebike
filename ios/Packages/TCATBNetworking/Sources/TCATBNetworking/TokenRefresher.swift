import Foundation
import TCATBModels

actor TokenRefresher {
    private let baseURL: URL
    private let tokenProvider: TokenProvider
    private let session: URLSession
    private var refreshTask: Task<Bool, Error>?

    init(baseURL: URL, tokenProvider: TokenProvider, session: URLSession) {
        self.baseURL = baseURL
        self.tokenProvider = tokenProvider
        self.session = session
    }

    /// Attempts to refresh the access token using the stored refresh token.
    /// Returns true if refresh succeeded, false otherwise.
    /// Coalesces concurrent refresh attempts into a single network call.
    func refreshIfNeeded() async -> Bool {
        if let existingTask = refreshTask {
            return (try? await existingTask.value) ?? false
        }

        let task = Task<Bool, Error> {
            defer { refreshTask = nil }
            return await performRefresh()
        }
        refreshTask = task
        return (try? await task.value) ?? false
    }

    private func performRefresh() async -> Bool {
        guard let refreshToken = await tokenProvider.getRefreshToken() else {
            await tokenProvider.clearTokens()
            return false
        }

        let url = baseURL.appendingPathComponent("auth/refresh")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body = RefreshRequest(refreshToken: refreshToken)
        guard let httpBody = try? JSONEncoder.apiEncoder.encode(body) else {
            await tokenProvider.clearTokens()
            return false
        }
        request.httpBody = httpBody

        do {
            let (data, response) = try await session.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200 else {
                await tokenProvider.clearTokens()
                return false
            }

            let tokenResponse = try JSONDecoder.apiDecoder.decode(TokenResponse.self, from: data)
            await tokenProvider.saveTokens(
                accessToken: tokenResponse.accessToken,
                refreshToken: tokenResponse.refreshToken
            )
            return true
        } catch {
            await tokenProvider.clearTokens()
            return false
        }
    }
}
