import Foundation

public struct UserResponse: Codable, Sendable, Identifiable {
    public var id: String { userId }

    public let userId: String
    public let username: String
    public let isAdmin: Bool
    public let isBanned: Bool
    public let createdAt: String?

    public init(userId: String, username: String, isAdmin: Bool = false, isBanned: Bool = false, createdAt: String? = nil) {
        self.userId = userId
        self.username = username
        self.isAdmin = isAdmin
        self.isBanned = isBanned
        self.createdAt = createdAt
    }

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case username
        case isAdmin = "is_admin"
        case isBanned = "is_banned"
        case createdAt = "created_at"
    }
}

public struct LeaderboardRank: Codable, Sendable, Equatable {
    public let period: String
    public let rank: Int?

    public init(period: String, rank: Int? = nil) {
        self.period = period
        self.rank = rank
    }
}

public struct UserDetailResponse: Codable, Sendable, Identifiable {
    public var id: String { userId }

    public let userId: String
    public let username: String
    public let isAdmin: Bool
    public let isBanned: Bool
    public let submissionCount: Int
    public let firstSeenAt: String?
    public let lastSeenAt: String?
    public let ownedBikeCount: Int
    public let leaderboardRanks: [LeaderboardRank]

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
}
