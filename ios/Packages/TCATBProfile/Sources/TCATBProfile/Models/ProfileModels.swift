// INTEGRATION: Move to TCATBModels

import Foundation

// MARK: - User Models

public struct UserResponse: Codable, Identifiable, Sendable {
    public let userId: String
    public let username: String
    public let isAdmin: Bool
    public let isBanned: Bool
    public let createdAt: String?

    public var id: String { userId }

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case username
        case isAdmin = "is_admin"
        case isBanned = "is_banned"
        case createdAt = "created_at"
    }

    public init(
        userId: String,
        username: String,
        isAdmin: Bool = false,
        isBanned: Bool = false,
        createdAt: String? = nil
    ) {
        self.userId = userId
        self.username = username
        self.isAdmin = isAdmin
        self.isBanned = isBanned
        self.createdAt = createdAt
    }
}

public struct LeaderboardRank: Codable, Sendable {
    public let period: String
    public let rank: Int?

    public init(period: String, rank: Int? = nil) {
        self.period = period
        self.rank = rank
    }
}

public struct UserDetailResponse: Codable, Identifiable, Sendable {
    public let userId: String
    public let username: String
    public let isAdmin: Bool
    public let isBanned: Bool
    public let submissionCount: Int
    public let firstSeenAt: String?
    public let lastSeenAt: String?
    public let ownedBikeCount: Int
    public let leaderboardRanks: [LeaderboardRank]

    public var id: String { userId }

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case username
        case isAdmin = "is_admin"
        case isBanned = "is_banned"
        case submissionCount = "submission_count"
        case firstSeenAt = "first_seen_at"
        case lastSeenAt = "last_seen_at"
        case ownedBikeCount = "owned_bike_count"
        case leaderboardRanks = "leaderboard_ranks"
    }

    public init(
        userId: String,
        username: String,
        isAdmin: Bool = false,
        isBanned: Bool = false,
        submissionCount: Int,
        firstSeenAt: String? = nil,
        lastSeenAt: String? = nil,
        ownedBikeCount: Int,
        leaderboardRanks: [LeaderboardRank]
    ) {
        self.userId = userId
        self.username = username
        self.isAdmin = isAdmin
        self.isBanned = isBanned
        self.submissionCount = submissionCount
        self.firstSeenAt = firstSeenAt
        self.lastSeenAt = lastSeenAt
        self.ownedBikeCount = ownedBikeCount
        self.leaderboardRanks = leaderboardRanks
    }
}

// MARK: - Leaderboard Models

public struct LeaderboardEntry: Codable, Identifiable, Sendable {
    public let rank: Int
    public let userId: String
    public let username: String
    public let score: Int

    public var id: String { userId }

    enum CodingKeys: String, CodingKey {
        case rank
        case userId = "user_id"
        case username
        case score
    }

    public init(rank: Int, userId: String, username: String, score: Int) {
        self.rank = rank
        self.userId = userId
        self.username = username
        self.score = score
    }
}

public struct LeaderboardResponse: Codable, Sendable {
    public let period: String
    public let startDate: String?
    public let endDate: String?
    public let entries: [LeaderboardEntry]

    enum CodingKeys: String, CodingKey {
        case period
        case startDate = "start_date"
        case endDate = "end_date"
        case entries
    }

    public init(period: String, startDate: String? = nil, endDate: String? = nil, entries: [LeaderboardEntry]) {
        self.period = period
        self.startDate = startDate
        self.endDate = endDate
        self.entries = entries
    }
}

public enum LeaderboardPeriod: String, CaseIterable, Sendable {
    case daily
    case weekly
    case monthly
    case allTime = "all_time"

    public var displayName: String {
        switch self {
        case .daily: return "Daily"
        case .weekly: return "Weekly"
        case .monthly: return "Monthly"
        case .allTime: return "All Time"
        }
    }
}

// MARK: - Submission Models

public struct ScoringBreakdown: Codable, Sendable {
    public let eventType: String
    public let label: String
    public let points: Int

    enum CodingKeys: String, CodingKey {
        case eventType = "event_type"
        case label
        case points
    }

    public init(eventType: String, label: String, points: Int) {
        self.eventType = eventType
        self.label = label
        self.points = points
    }
}

public struct SubmissionResponse: Codable, Identifiable, Sendable {
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

    public var id: String { submissionId }

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
}

public struct CursorPaginatedSubmissions: Codable, Sendable {
    public let items: [SubmissionResponse]
    public let nextCursor: String?
    public let hasMore: Bool

    enum CodingKeys: String, CodingKey {
        case items
        case nextCursor = "next_cursor"
        case hasMore = "has_more"
    }

    public init(items: [SubmissionResponse], nextCursor: String? = nil, hasMore: Bool = false) {
        self.items = items
        self.nextCursor = nextCursor
        self.hasMore = hasMore
    }
}

// MARK: - Score Rules

public struct ScoreRule: Sendable {
    public let id: String
    public let label: String
    public let points: Int

    public init(id: String, label: String, points: Int) {
        self.id = id
        self.label = label
        self.points = points
    }
}

public enum ScoreRules {
    public static let photoRules: [ScoreRule] = [
        ScoreRule(id: "add_image", label: "Took a photo", points: 2),
        ScoreRule(id: "first_bike_today", label: "First photo of this bike today", points: 5),
        ScoreRule(id: "first_bike_for_user", label: "Your first photo of this bike", points: 5),
        ScoreRule(id: "first_bike_ever", label: "First photo of this bike ever", points: 10),
    ]

    public static let tagRules: [ScoreRule] = [
        ScoreRule(id: "tag_1", label: "1st tag on a submission", points: 2),
        ScoreRule(id: "tag_2", label: "2nd tag", points: 1),
        ScoreRule(id: "tag_3", label: "3rd tag", points: 1),
        ScoreRule(id: "tag_4_plus", label: "4th+ tags", points: 0),
    ]
}
