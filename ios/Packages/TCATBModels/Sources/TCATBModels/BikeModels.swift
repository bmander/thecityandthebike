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
