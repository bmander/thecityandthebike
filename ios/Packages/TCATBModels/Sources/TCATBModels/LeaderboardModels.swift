import Foundation

public enum LeaderboardPeriod: String, Codable, Sendable, CaseIterable {
    case weekly
    case monthly
    case allTime = "all_time"
}

public struct LeaderboardEntry: Codable, Sendable, Equatable {
    public let rank: Int
    public let userId: String
    public let username: String
    public let score: Int

    public init(rank: Int, userId: String, username: String, score: Int) {
        self.rank = rank
        self.userId = userId
        self.username = username
        self.score = score
    }

    enum CodingKeys: String, CodingKey {
        case rank
        case userId = "user_id"
        case username
        case score
    }
}

public struct LeaderboardResponse: Codable, Sendable {
    public let period: String
    public let startDate: String?
    public let endDate: String?
    public let entries: [LeaderboardEntry]

    public init(period: String, startDate: String? = nil, endDate: String? = nil, entries: [LeaderboardEntry]) {
        self.period = period
        self.startDate = startDate
        self.endDate = endDate
        self.entries = entries
    }

    enum CodingKeys: String, CodingKey {
        case period
        case startDate = "start_date"
        case endDate = "end_date"
        case entries
    }
}
