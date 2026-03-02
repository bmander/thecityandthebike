import Foundation

public struct MultipartFormData: Sendable {
    private let boundary: String
    private var parts: [(name: String, data: Data, fileName: String?, mimeType: String?)]

    public init(boundary: String = UUID().uuidString) {
        self.boundary = boundary
        self.parts = []
    }

    public var contentType: String {
        "multipart/form-data; boundary=\(boundary)"
    }

    public mutating func addField(name: String, value: String) {
        if let data = value.data(using: .utf8) {
            parts.append((name: name, data: data, fileName: nil, mimeType: nil))
        }
    }

    public mutating func addFile(name: String, data: Data, fileName: String, mimeType: String) {
        parts.append((name: name, data: data, fileName: fileName, mimeType: mimeType))
    }

    public func buildBody() -> Data {
        var body = Data()
        for part in parts {
            body.append("--\(boundary)\r\n")
            if let fileName = part.fileName, let mimeType = part.mimeType {
                body.append("Content-Disposition: form-data; name=\"\(part.name)\"; filename=\"\(fileName)\"\r\n")
                body.append("Content-Type: \(mimeType)\r\n\r\n")
            } else {
                body.append("Content-Disposition: form-data; name=\"\(part.name)\"\r\n\r\n")
            }
            body.append(part.data)
            body.append("\r\n")
        }
        body.append("--\(boundary)--\r\n")
        return body
    }
}

extension Data {
    mutating func append(_ string: String) {
        if let data = string.data(using: .utf8) {
            append(data)
        }
    }
}
