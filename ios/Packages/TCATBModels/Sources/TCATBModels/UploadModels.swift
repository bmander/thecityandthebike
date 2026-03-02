import Foundation

public struct UploadResponse: Codable, Sendable {
    public let url: String
    public let filename: String
    public let thumbnailUrl: String?

    public init(url: String, filename: String, thumbnailUrl: String? = nil) {
        self.url = url
        self.filename = filename
        self.thumbnailUrl = thumbnailUrl
    }

    enum CodingKeys: String, CodingKey {
        case url
        case filename
        case thumbnailUrl = "thumbnail_url"
    }
}
