import Foundation

public struct ScoringBreakdown: Codable, Sendable, Equatable {
    public let eventType: String
    public let label: String
    public let points: Int

    public init(eventType: String, label: String, points: Int) {
        self.eventType = eventType
        self.label = label
        self.points = points
    }

    enum CodingKeys: String, CodingKey {
        case eventType = "event_type"
        case label
        case points
    }
}

public struct SubmissionResponse: Codable, Sendable, Identifiable {
    public var id: String { submissionId }

    public let submissionId: String
    public let userId: String
    public let bikeQrId: String
    public let imageUrl: String?
    public let imageUrlThumbnail: String?
    public let capturedDate: String?
    public let uploadedAt: String?
    public let userCaption: String?
    public let username: String?
    public let provider: String?
    public let side: String?
    public let pointsAwarded: Int?
    public let pointsBreakdown: [ScoringBreakdown]?
    public let flagCount: Int?

    public init(
        submissionId: String,
        userId: String,
        bikeQrId: String,
        imageUrl: String? = nil,
        imageUrlThumbnail: String? = nil,
        capturedDate: String? = nil,
        uploadedAt: String? = nil,
        userCaption: String? = nil,
        username: String? = nil,
        provider: String? = nil,
        side: String? = nil,
        pointsAwarded: Int? = nil,
        pointsBreakdown: [ScoringBreakdown]? = nil,
        flagCount: Int? = nil
    ) {
        self.submissionId = submissionId
        self.userId = userId
        self.bikeQrId = bikeQrId
        self.imageUrl = imageUrl
        self.imageUrlThumbnail = imageUrlThumbnail
        self.capturedDate = capturedDate
        self.uploadedAt = uploadedAt
        self.userCaption = userCaption
        self.username = username
        self.provider = provider
        self.side = side
        self.pointsAwarded = pointsAwarded
        self.pointsBreakdown = pointsBreakdown
        self.flagCount = flagCount
    }

    enum CodingKeys: String, CodingKey {
        case submissionId = "submission_id"
        case userId = "user_id"
        case bikeQrId = "bike_qr_id"
        case imageUrl = "image_url"
        case imageUrlThumbnail = "image_url_thumbnail"
        case capturedDate = "captured_date"
        case uploadedAt = "uploaded_at"
        case userCaption = "user_caption"
        case username
        case provider
        case side
        case pointsAwarded = "points_awarded"
        case pointsBreakdown = "points_breakdown"
        case flagCount = "flag_count"
    }
}

public struct PaginatedSubmissions: Codable, Sendable {
    public let items: [SubmissionResponse]
    public let total: Int
    public let limit: Int
    public let offset: Int

    public init(items: [SubmissionResponse], total: Int, limit: Int, offset: Int) {
        self.items = items
        self.total = total
        self.limit = limit
        self.offset = offset
    }
}

public struct CursorPaginatedSubmissions: Codable, Sendable {
    public let items: [SubmissionResponse]
    public let nextCursor: String?
    public let hasMore: Bool

    public init(items: [SubmissionResponse], nextCursor: String? = nil, hasMore: Bool = false) {
        self.items = items
        self.nextCursor = nextCursor
        self.hasMore = hasMore
    }

    enum CodingKeys: String, CodingKey {
        case items
        case nextCursor = "next_cursor"
        case hasMore = "has_more"
    }
}

public struct ProcessedMaskResult: Codable, Sendable {
    public let ring: [[Float]]
    public let width: Int
    public let height: Int

    public init(ring: [[Float]], width: Int, height: Int) {
        self.ring = ring
        self.width = width
        self.height = height
    }
}
