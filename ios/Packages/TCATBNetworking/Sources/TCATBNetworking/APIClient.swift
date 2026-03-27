import Foundation
import TCATBModels

public final class APIClient: Sendable {
    public let baseURL: URL
    let session: URLSession
    let authInterceptor: AuthInterceptor
    let tokenRefresher: TokenRefresher

    public init(environment: APIEnvironment, tokenProvider: TokenProvider, session: URLSession = .shared) {
        self.baseURL = environment.baseURL
        self.session = session
        self.authInterceptor = AuthInterceptor(tokenProvider: tokenProvider)
        self.tokenRefresher = TokenRefresher(
            baseURL: environment.baseURL,
            tokenProvider: tokenProvider,
            session: session
        )
    }

    // MARK: - Request Building

    func buildURL(path: String, queryItems: [URLQueryItem]? = nil) -> URL {
        var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)!
        if let queryItems, !queryItems.isEmpty {
            components.queryItems = queryItems
        }
        return components.url!
    }

    func buildRequest(
        method: String,
        path: String,
        queryItems: [URLQueryItem]? = nil,
        body: (any Encodable)? = nil,
        contentType: String = "application/json"
    ) async throws -> URLRequest {
        let url = buildURL(path: path, queryItems: queryItems)
        var request = URLRequest(url: url)
        request.httpMethod = method

        if let body {
            request.setValue(contentType, forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONEncoder.apiEncoder.encode(body)
        }

        request = await authInterceptor.intercept(request)
        return request
    }

    // MARK: - Response Handling

    func performRequest<T: Decodable>(_ request: URLRequest) async throws -> T {
        let (data, response) = try await execute(request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.unknown("Invalid response type")
        }

        if httpResponse.statusCode == 401 {
            let refreshed = await tokenRefresher.refreshIfNeeded()
            if refreshed {
                var retryRequest = request
                retryRequest = await authInterceptor.intercept(retryRequest)
                let (retryData, retryResponse) = try await execute(retryRequest)
                guard let retryHTTP = retryResponse as? HTTPURLResponse else {
                    throw NetworkError.unknown("Invalid response type")
                }
                return try handleResponse(data: retryData, statusCode: retryHTTP.statusCode)
            } else {
                throw NetworkError.unauthorized
            }
        }

        return try handleResponse(data: data, statusCode: httpResponse.statusCode)
    }

    func performRequestNoContent(_ request: URLRequest) async throws -> MessageResponse {
        return try await performRequest(request)
    }

    private func execute(_ request: URLRequest) async throws -> (Data, URLResponse) {
        do {
            return try await session.data(for: request)
        } catch {
            throw NetworkError.networkFailure(error.localizedDescription)
        }
    }

    private func handleResponse<T: Decodable>(data: Data, statusCode: Int) throws -> T {
        guard (200...299).contains(statusCode) else {
            throw NetworkError.fromHTTPStatus(statusCode, body: data)
        }

        do {
            return try JSONDecoder.apiDecoder.decode(T.self, from: data)
        } catch {
            throw NetworkError.decodingError(error.localizedDescription)
        }
    }
}
