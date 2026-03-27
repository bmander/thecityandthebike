import XCTest
import TCATBModels
@testable import TCATBNetworking

final class APIClientTests: XCTestCase {
    private var client: APIClient!
    private var tokenProvider: MockTokenProvider!
    private var session: URLSession!

    override func setUp() {
        super.setUp()
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        session = URLSession(configuration: config)
        tokenProvider = MockTokenProvider(accessToken: "test-token")
        client = APIClient(environment: .staging, tokenProvider: tokenProvider, session: session)
    }

    override func tearDown() {
        MockURLProtocol.requestHandler = nil
        super.tearDown()
    }

    // MARK: - URL Construction

    func testBuildURLWithPath() {
        let url = client.buildURL(path: "users/me")
        XCTAssertEqual(url.absoluteString, "https://tcatb-api-staging-821600862601.us-central1.run.app/users/me")
    }

    func testBuildURLWithQueryItems() {
        let url = client.buildURL(path: "submissions", queryItems: [
            URLQueryItem(name: "limit", value: "10"),
            URLQueryItem(name: "cursor", value: "abc123"),
        ])
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        XCTAssertEqual(components.path, "/submissions")
        let queryDict = Dictionary(uniqueKeysWithValues: components.queryItems!.map { ($0.name, $0.value) })
        XCTAssertEqual(queryDict["limit"], "10")
        XCTAssertEqual(queryDict["cursor"], "abc123")
    }

    // MARK: - Request Building

    func testBuildRequestSetsAuthHeader() async throws {
        let request = try await client.buildRequest(method: "GET", path: "users/me")
        XCTAssertEqual(request.httpMethod, "GET")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer test-token")
    }

    func testBuildRequestSkipsAuthForLogin() async throws {
        let request = try await client.buildRequest(method: "POST", path: "auth/login")
        XCTAssertNil(request.value(forHTTPHeaderField: "Authorization"))
    }

    func testBuildRequestSkipsAuthForRegister() async throws {
        let request = try await client.buildRequest(method: "POST", path: "auth/register")
        XCTAssertNil(request.value(forHTTPHeaderField: "Authorization"))
    }

    func testBuildRequestSkipsAuthWhenNoToken() async throws {
        tokenProvider.accessToken = nil
        let request = try await client.buildRequest(method: "GET", path: "users/me")
        XCTAssertNil(request.value(forHTTPHeaderField: "Authorization"))
    }

    func testBuildRequestSetsContentTypeWhenBodyPresent() async throws {
        let body = LoginRequest(username: "user", password: "pass")
        let request = try await client.buildRequest(method: "POST", path: "auth/login", body: body)
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/json")
    }

    func testBuildRequestOmitsContentTypeWhenNoBody() async throws {
        let request = try await client.buildRequest(method: "GET", path: "users/me")
        XCTAssertNil(request.value(forHTTPHeaderField: "Content-Type"))
    }

    func testBuildRequestEncodesBody() async throws {
        let body = LoginRequest(username: "user", password: "pass")
        let request = try await client.buildRequest(method: "POST", path: "auth/login", body: body)
        XCTAssertNotNil(request.httpBody)
        let decoded = try JSONDecoder().decode(LoginRequest.self, from: request.httpBody!)
        XCTAssertEqual(decoded.username, "user")
        XCTAssertEqual(decoded.password, "pass")
    }

    // MARK: - Response Handling

    func testSuccessfulGetRequest() async throws {
        MockURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.httpMethod, "GET")
            let user = UserResponse(userId: "u1", username: "alice")
            let data = try JSONEncoder.apiEncoder.encode(user)
            let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (response, data)
        }

        let user: UserResponse = try await client.getCurrentUser()
        XCTAssertEqual(user.userId, "u1")
        XCTAssertEqual(user.username, "alice")
    }

    func testNotFoundThrowsError() async {
        MockURLProtocol.requestHandler = { request in
            let response = HTTPURLResponse(url: request.url!, statusCode: 404, httpVersion: nil, headerFields: nil)!
            return (response, Data())
        }

        do {
            let _: UserResponse = try await client.getCurrentUser()
            XCTFail("Expected NetworkError.notFound")
        } catch let error as NetworkError {
            XCTAssertEqual(error, .notFound)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testUnauthorizedWithRefreshRetriesRequest() async throws {
        tokenProvider.refreshToken = "old-refresh"
        var requestCount = 0
        MockURLProtocol.requestHandler = { request in
            requestCount += 1
            let urlString = request.url!.absoluteString
            if urlString.contains("auth/refresh") {
                // Token refresh succeeds
                let tokens = TokenResponse(accessToken: "new-token", refreshToken: "new-refresh")
                let data = try JSONEncoder.apiEncoder.encode(tokens)
                let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
                return (response, data)
            } else if requestCount == 1 {
                // First request returns 401
                let response = HTTPURLResponse(url: request.url!, statusCode: 401, httpVersion: nil, headerFields: nil)!
                return (response, Data())
            } else {
                // Retry succeeds with new token
                XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer new-token")
                let user = UserResponse(userId: "u1", username: "alice")
                let data = try JSONEncoder.apiEncoder.encode(user)
                let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
                return (response, data)
            }
        }

        let user: UserResponse = try await client.getCurrentUser()
        XCTAssertEqual(user.userId, "u1")
    }

    func testUnauthorizedWithFailedRefreshThrowsError() async {
        tokenProvider.refreshToken = "bad-refresh"
        MockURLProtocol.requestHandler = { request in
            let urlString = request.url!.absoluteString
            if urlString.contains("auth/refresh") {
                // Token refresh fails
                let response = HTTPURLResponse(url: request.url!, statusCode: 401, httpVersion: nil, headerFields: nil)!
                return (response, Data())
            } else {
                // Original request returns 401
                let response = HTTPURLResponse(url: request.url!, statusCode: 401, httpVersion: nil, headerFields: nil)!
                return (response, Data())
            }
        }

        do {
            let _: UserResponse = try await client.getCurrentUser()
            XCTFail("Expected NetworkError.unauthorized")
        } catch let error as NetworkError {
            XCTAssertEqual(error, .unauthorized)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testServerErrorThrowsError() async {
        MockURLProtocol.requestHandler = { request in
            let body = #"{"detail":{"msg":"Internal server error"}}"#.data(using: .utf8)!
            let response = HTTPURLResponse(url: request.url!, statusCode: 500, httpVersion: nil, headerFields: nil)!
            return (response, body)
        }

        do {
            let _: UserResponse = try await client.getCurrentUser()
            XCTFail("Expected NetworkError.serverError")
        } catch let error as NetworkError {
            if case .serverError(let code, let message) = error {
                XCTAssertEqual(code, 500)
                XCTAssertEqual(message, "Internal server error")
            } else {
                XCTFail("Expected serverError, got \(error)")
            }
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }
}
