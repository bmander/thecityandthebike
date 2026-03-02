import SwiftUI

private let flavorTexts = [
    "Waking up servers\u{2026}",
    "Sending to space\u{2026}",
    "Tagging\u{2026}",
    "Jiggling electrons\u{2026}",
    "Popping a wheelie\u{2026}",
    "Paying the piper\u{2026}",
    "Cropping\u{2026}",
]

public struct LoadingIndicator: View {
    @State private var index = 0

    public init() {}

    public var body: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text(flavorTexts[index])
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .task {
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(2))
                index = (index + 1) % flavorTexts.count
            }
        }
    }
}
