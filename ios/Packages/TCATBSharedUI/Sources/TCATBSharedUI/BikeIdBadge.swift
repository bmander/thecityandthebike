import SwiftUI

private let maxIdLength = 16

public struct BikeIdBadge: View {
    let provider: String?
    let bikeQrId: String

    public init(provider: String?, bikeQrId: String) {
        self.provider = provider
        self.bikeQrId = bikeQrId
    }

    public var body: some View {
        HStack(spacing: 0) {
            if let provider {
                Text(provider.uppercased())
                    .font(.caption2)
                    .fontWeight(.medium)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 3)
                    .background(providerColor(provider))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
                    .padding(.leading, 7)
                    .padding(.vertical, 2)
                    .padding(.trailing, 3)
            }
            Text(abbreviateId(bikeQrId))
                .font(.headline)
                .fontDesign(.monospaced)
                .fontWeight(.bold)
                .lineLimit(1)
                .padding(.leading, 2)
                .padding(.trailing, 7)
                .padding(.vertical, 2)
        }
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(.separator, lineWidth: 1)
        )
    }
}

private func providerColor(_ provider: String) -> Color {
    switch provider.lowercased() {
    case "lime": return Color.green
    case "bird": return Color.blue
    default: return Color.accentColor
    }
}

private func abbreviateId(_ id: String) -> String {
    guard id.count > maxIdLength else { return id }
    let keep = (maxIdLength - 1) / 2
    return String(id.prefix(keep)) + "\u{2026}" + String(id.suffix(keep))
}
