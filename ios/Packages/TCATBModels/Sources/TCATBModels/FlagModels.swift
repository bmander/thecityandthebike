import Foundation

public struct FlagStatusResponse: Codable, Sendable, Equatable {
    public let flagged: Bool
    public let flagCount: Int

    public init(flagged: Bool, flagCount: Int) {
        self.flagged = flagged
        self.flagCount = flagCount
    }

    enum CodingKeys: String, CodingKey {
        case flagged
        case flagCount = "flag_count"
    }
}
