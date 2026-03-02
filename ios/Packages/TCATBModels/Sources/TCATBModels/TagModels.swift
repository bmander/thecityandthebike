import Foundation

public struct TagResponse: Codable, Sendable, Identifiable {
    public var id: String { tagId }

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
