// INTEGRATION: Move to TCATBModels
// These types are local copies for standalone development.
// During integration, replace with imports from TCATBModels.

import Foundation

public struct UserSummary: Codable, Sendable, Equatable, Identifiable {
    public let name: String
    public let id: String

    public init(name: String, id: String) {
        self.name = name
        self.id = id
    }
}

public struct BikeOwner: Codable, Sendable {
    public let user: UserSummary
    public let submissionCount: Int

    public init(user: UserSummary, submissionCount: Int) {
        self.user = user
        self.submissionCount = submissionCount
    }

    enum CodingKeys: String, CodingKey {
        case user
        case submissionCount = "submission_count"
    }
}

public struct BikeListItem: Codable, Sendable, Identifiable {
    public var id: String { bikeQrId }
    public let bikeQrId: String
    public let provider: String?
    public let submissionCount: Int
    public let owner: UserSummary?

    public init(bikeQrId: String, provider: String? = nil, submissionCount: Int, owner: UserSummary? = nil) {
        self.bikeQrId = bikeQrId
        self.provider = provider
        self.submissionCount = submissionCount
        self.owner = owner
    }

    enum CodingKeys: String, CodingKey {
        case bikeQrId = "bike_qr_id"
        case provider
        case submissionCount = "submission_count"
        case owner
    }
}

public struct PaginatedBikes: Codable, Sendable {
    public let items: [BikeListItem]
    public let total: Int
    public let limit: Int
    public let offset: Int

    public init(items: [BikeListItem], total: Int, limit: Int, offset: Int) {
        self.items = items
        self.total = total
        self.limit = limit
        self.offset = offset
    }
}

public struct BikeDetailResponse: Codable, Sendable {
    public let bikeQrId: String
    public let provider: String?
    public let bikeBrand: String?
    public let firstSeenAt: String?
    public let lastSeenAt: String?
    public let notes: String?
    public let submissionCount: Int
    public let owners: [BikeOwner]
    public let firstCapturedBy: UserSummary?
    public let lastCapturedBy: UserSummary?

    public init(
        bikeQrId: String,
        provider: String? = nil,
        bikeBrand: String? = nil,
        firstSeenAt: String? = nil,
        lastSeenAt: String? = nil,
        notes: String? = nil,
        submissionCount: Int,
        owners: [BikeOwner] = [],
        firstCapturedBy: UserSummary? = nil,
        lastCapturedBy: UserSummary? = nil
    ) {
        self.bikeQrId = bikeQrId
        self.provider = provider
        self.bikeBrand = bikeBrand
        self.firstSeenAt = firstSeenAt
        self.lastSeenAt = lastSeenAt
        self.notes = notes
        self.submissionCount = submissionCount
        self.owners = owners
        self.firstCapturedBy = firstCapturedBy
        self.lastCapturedBy = lastCapturedBy
    }

    enum CodingKeys: String, CodingKey {
        case bikeQrId = "bike_qr_id"
        case provider
        case bikeBrand = "bike_brand"
        case firstSeenAt = "first_seen_at"
        case lastSeenAt = "last_seen_at"
        case notes
        case submissionCount = "submission_count"
        case owners
        case firstCapturedBy = "first_captured_by"
        case lastCapturedBy = "last_captured_by"
    }
}

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

public struct TagResponse: Codable, Sendable {
    public let tagId: String
    public let submissionId: String
    public let userId: String
    public let imageUrl: String
    public let ring: [[Float]]?
    public let ringWidth: Int?
    public let ringHeight: Int?
    public let createdAt: String
    public let pointsAwarded: Int?
    public let pointsBreakdown: [ScoringBreakdown]?

    public init(
        tagId: String,
        submissionId: String,
        userId: String,
        imageUrl: String,
        ring: [[Float]]? = nil,
        ringWidth: Int? = nil,
        ringHeight: Int? = nil,
        createdAt: String,
        pointsAwarded: Int? = nil,
        pointsBreakdown: [ScoringBreakdown]? = nil
    ) {
        self.tagId = tagId
        self.submissionId = submissionId
        self.userId = userId
        self.imageUrl = imageUrl
        self.ring = ring
        self.ringWidth = ringWidth
        self.ringHeight = ringHeight
        self.createdAt = createdAt
        self.pointsAwarded = pointsAwarded
        self.pointsBreakdown = pointsBreakdown
    }

    enum CodingKeys: String, CodingKey {
        case tagId = "tag_id"
        case submissionId = "submission_id"
        case userId = "user_id"
        case imageUrl = "image_url"
        case ring
        case ringWidth = "ring_width"
        case ringHeight = "ring_height"
        case createdAt = "created_at"
        case pointsAwarded = "points_awarded"
        case pointsBreakdown = "points_breakdown"
    }
}

public struct TagDetailResponse: Codable, Sendable {
    public let tagId: String
    public let imageUrl: String
    public let createdAt: String
    public let submissionCount: Int
    public let firstCapturedAt: String?
    public let lastCapturedAt: String?
    public let firstCapturedBy: UserSummary?
    public let lastCapturedBy: UserSummary?

    public init(
        tagId: String,
        imageUrl: String,
        createdAt: String,
        submissionCount: Int,
        firstCapturedAt: String? = nil,
        lastCapturedAt: String? = nil,
        firstCapturedBy: UserSummary? = nil,
        lastCapturedBy: UserSummary? = nil
    ) {
        self.tagId = tagId
        self.imageUrl = imageUrl
        self.createdAt = createdAt
        self.submissionCount = submissionCount
        self.firstCapturedAt = firstCapturedAt
        self.lastCapturedAt = lastCapturedAt
        self.firstCapturedBy = firstCapturedBy
        self.lastCapturedBy = lastCapturedBy
    }

    enum CodingKeys: String, CodingKey {
        case tagId = "tag_id"
        case imageUrl = "image_url"
        case createdAt = "created_at"
        case submissionCount = "submission_count"
        case firstCapturedAt = "first_captured_at"
        case lastCapturedAt = "last_captured_at"
        case firstCapturedBy = "first_captured_by"
        case lastCapturedBy = "last_captured_by"
    }
}
