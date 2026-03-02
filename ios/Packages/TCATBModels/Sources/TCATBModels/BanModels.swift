import Foundation

public struct BanResponse: Codable, Sendable {
    public let msg: String
    public let isBanned: Bool
    public let deviceBanned: Bool

    public init(msg: String, isBanned: Bool, deviceBanned: Bool = false) {
        self.msg = msg
        self.isBanned = isBanned
        self.deviceBanned = deviceBanned
    }

    enum CodingKeys: String, CodingKey {
        case msg
        case isBanned = "is_banned"
        case deviceBanned = "device_banned"
    }
}
