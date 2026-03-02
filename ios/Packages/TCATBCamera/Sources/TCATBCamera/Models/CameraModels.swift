import Foundation

// INTEGRATION: Move to TCATBModels

/// Local copy of UploadResponse for use within the camera package.
public struct CameraUploadResponse: Codable, Sendable {
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

/// Scoring breakdown returned after a successful submission.
public struct CameraScoringBreakdown: Codable, Sendable, Equatable {
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

/// Response from the create-submission API endpoint.
public struct CameraSubmissionResponse: Codable, Sendable {
    public let submissionId: String
    public let pointsAwarded: Int?
    public let pointsBreakdown: [CameraScoringBreakdown]?

    public init(
        submissionId: String,
        pointsAwarded: Int? = nil,
        pointsBreakdown: [CameraScoringBreakdown]? = nil
    ) {
        self.submissionId = submissionId
        self.pointsAwarded = pointsAwarded
        self.pointsBreakdown = pointsBreakdown
    }

    enum CodingKeys: String, CodingKey {
        case submissionId = "submission_id"
        case pointsAwarded = "points_awarded"
        case pointsBreakdown = "points_breakdown"
    }
}

/// Result of parsing a bike QR code URL.
public struct ParsedBikeUrl: Sendable, Equatable {
    public let provider: String?
    public let bikeId: String

    public init(provider: String?, bikeId: String) {
        self.provider = provider
        self.bikeId = bikeId
    }
}

/// Which side of the bike fender is being photographed.
public enum FenderSide: String, Sendable {
    case left
    case right
}

/// Camera flash mode.
public enum FlashMode: Sendable {
    case auto
    case on
    case off

    public var next: FlashMode {
        switch self {
        case .auto: return .on
        case .on: return .off
        case .off: return .auto
        }
    }

    public var label: String {
        switch self {
        case .auto: return "Flash auto"
        case .on: return "Flash on"
        case .off: return "Flash off"
        }
    }

    public var systemImageName: String {
        switch self {
        case .auto: return "bolt.badge.automatic"
        case .on: return "bolt.fill"
        case .off: return "bolt.slash"
        }
    }
}

/// Known bike-share providers whose QR codes are recognized.
public let whitelistedProviders: Set<String> = ["lime", "bird"]

/// Parse a bike QR code value into a provider and bike ID.
///
/// - lime.bike hostname -> provider="lime", id=last path segment with '=' padding stripped
/// - ride.bird.co hostname -> provider="bird", id=last path segment
/// - Anything else -> provider=nil, id=raw value as-is
public func parseBikeUrl(_ raw: String) -> ParsedBikeUrl {
    guard let url = URL(string: raw),
          let scheme = url.scheme, !scheme.isEmpty,
          let host = url.host, !host.isEmpty else {
        return ParsedBikeUrl(provider: nil, bikeId: raw)
    }

    let pathSegments = url.pathComponents.filter { $0 != "/" }
    guard let lastSegment = pathSegments.last else {
        return ParsedBikeUrl(provider: nil, bikeId: raw)
    }

    switch host {
    case "lime.bike":
        let bikeId = String(lastSegment.trimmingCharacters(in: CharacterSet(charactersIn: "=")))
        return ParsedBikeUrl(provider: "lime", bikeId: bikeId)
    case "ride.bird.co":
        return ParsedBikeUrl(provider: "bird", bikeId: lastSegment)
    default:
        return ParsedBikeUrl(provider: nil, bikeId: raw)
    }
}

/// Returns `true` if the QR code value belongs to a recognized bike-share provider.
public func isRecognizedProvider(_ raw: String) -> Bool {
    guard let provider = parseBikeUrl(raw).provider else { return false }
    return whitelistedProviders.contains(provider)
}
