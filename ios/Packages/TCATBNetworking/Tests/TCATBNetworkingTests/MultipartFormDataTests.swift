import XCTest
@testable import TCATBNetworking

final class MultipartFormDataTests: XCTestCase {

    func testContentTypeIncludesBoundary() {
        let form = MultipartFormData(boundary: "test-boundary")
        XCTAssertEqual(form.contentType, "multipart/form-data; boundary=test-boundary")
    }

    func testAddFieldProducesCorrectBody() {
        var form = MultipartFormData(boundary: "boundary123")
        form.addField(name: "username", value: "alice")
        let body = String(data: form.buildBody(), encoding: .utf8)!

        XCTAssertTrue(body.contains("--boundary123\r\n"))
        XCTAssertTrue(body.contains("Content-Disposition: form-data; name=\"username\"\r\n\r\nalice"))
        XCTAssertTrue(body.contains("--boundary123--\r\n"))
    }

    func testAddFileProducesCorrectHeaders() {
        var form = MultipartFormData(boundary: "boundary123")
        let imageData = Data([0x01, 0x02, 0x03, 0x04]) // Use non-JPEG bytes that are valid in a String context
        form.addFile(name: "image", data: imageData, fileName: "photo.jpg", mimeType: "image/jpeg")
        let body = form.buildBody()

        // Check headers by looking for known substrings in the raw data
        let headerString = "Content-Disposition: form-data; name=\"image\"; filename=\"photo.jpg\""
        let typeString = "Content-Type: image/jpeg"
        XCTAssertTrue(body.range(of: headerString.data(using: .utf8)!) != nil)
        XCTAssertTrue(body.range(of: typeString.data(using: .utf8)!) != nil)
    }

    func testMultiplePartsIncludesAll() {
        var form = MultipartFormData(boundary: "b")
        form.addField(name: "field1", value: "value1")
        form.addField(name: "field2", value: "value2")
        form.addFile(name: "file", data: Data([0xFF, 0xD8]), fileName: "test.bin", mimeType: "application/octet-stream")
        let body = form.buildBody()

        // Count boundary occurrences using Data.range(of:) instead of String conversion
        // (binary file data may not be valid UTF-8)
        let boundaryData = "--b\r\n".data(using: .utf8)!
        var count = 0
        var searchRange = body.startIndex..<body.endIndex
        while let range = body.range(of: boundaryData, in: searchRange) {
            count += 1
            searchRange = range.upperBound..<body.endIndex
        }
        XCTAssertEqual(count, 3) // 3 parts = 3 boundary lines

        let closingBoundary = "--b--\r\n".data(using: .utf8)!
        XCTAssertNotNil(body.range(of: closingBoundary))
    }

    func testEmptyFormProducesClosingBoundary() {
        let form = MultipartFormData(boundary: "empty")
        let body = String(data: form.buildBody(), encoding: .utf8)!
        XCTAssertEqual(body, "--empty--\r\n")
    }
}
