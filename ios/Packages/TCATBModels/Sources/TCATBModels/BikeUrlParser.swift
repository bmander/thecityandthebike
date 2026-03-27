import Foundation

public struct ParsedBikeUrl: Sendable, Equatable {
    public let provider: String?
    public let bikeId: String

    public init(provider: String?, bikeId: String) {
        self.provider = provider
        self.bikeId = bikeId
    }
}

public let whitelistedProviders: Set<String> = ["lime", "bird"]

/// Parse a bike QR code value into a provider and bike ID.
///
/// Rules:
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
        let trimmed = lastSegment.trimmingCharacters(in: CharacterSet(charactersIn: "="))
        return ParsedBikeUrl(provider: "lime", bikeId: trimmed)
    case "ride.bird.co":
        return ParsedBikeUrl(provider: "bird", bikeId: lastSegment)
    default:
        return ParsedBikeUrl(provider: nil, bikeId: raw)
    }
}

public func isRecognizedProvider(_ raw: String) -> Bool {
    guard let provider = parseBikeUrl(raw).provider else { return false }
    return whitelistedProviders.contains(provider)
}
