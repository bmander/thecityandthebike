import XCTest
@testable import TCATBNetworking

final class NetworkErrorTests: XCTestCase {

    func testFromHTTPStatus401() {
        let error = NetworkError.fromHTTPStatus(401, body: nil)
        XCTAssertEqual(error, .unauthorized)
    }

    func testFromHTTPStatus403() {
        let error = NetworkError.fromHTTPStatus(403, body: nil)
        XCTAssertEqual(error, .forbidden)
    }

    func testFromHTTPStatus404() {
        let error = NetworkError.fromHTTPStatus(404, body: nil)
        XCTAssertEqual(error, .notFound)
    }

    func testFromHTTPStatus422WithBody() {
        let body = #"{"detail":{"msg":"Invalid field value"}}"#.data(using: .utf8)!
        let error = NetworkError.fromHTTPStatus(422, body: body)
        XCTAssertEqual(error, .validationError("Invalid field value"))
    }

    func testFromHTTPStatus429() {
        let error = NetworkError.fromHTTPStatus(429, body: nil)
        XCTAssertEqual(error, .rateLimited(retryAfter: nil))
    }

    func testFromHTTPStatus500() {
        let body = #"{"detail":{"msg":"Server exploded"}}"#.data(using: .utf8)!
        let error = NetworkError.fromHTTPStatus(500, body: body)
        XCTAssertEqual(error, .serverError(statusCode: 500, message: "Server exploded"))
    }

    func testFromHTTPStatus502() {
        let error = NetworkError.fromHTTPStatus(502, body: nil)
        if case .serverError(let code, _) = error {
            XCTAssertEqual(code, 502)
        } else {
            XCTFail("Expected serverError")
        }
    }

    func testFromHTTPStatusUnknown() {
        let error = NetworkError.fromHTTPStatus(418, body: nil)
        if case .unknown = error {
            // pass
        } else {
            XCTFail("Expected unknown error for status 418")
        }
    }

    func testDisplayMessages() {
        XCTAssertFalse(NetworkError.unauthorized.displayMessage.isEmpty)
        XCTAssertFalse(NetworkError.forbidden.displayMessage.isEmpty)
        XCTAssertFalse(NetworkError.notFound.displayMessage.isEmpty)
        XCTAssertFalse(NetworkError.rateLimited(retryAfter: nil).displayMessage.isEmpty)
        XCTAssertFalse(NetworkError.networkFailure("timeout").displayMessage.isEmpty)
        XCTAssertFalse(NetworkError.decodingError("bad json").displayMessage.isEmpty)
        XCTAssertFalse(NetworkError.unknown("wat").displayMessage.isEmpty)
    }

    func testErrorBodyWithStringDetail() {
        let body = #"{"detail":"Not found"}"#.data(using: .utf8)!
        let error = NetworkError.fromHTTPStatus(422, body: body)
        XCTAssertEqual(error, .validationError("Not found"))
    }
}
