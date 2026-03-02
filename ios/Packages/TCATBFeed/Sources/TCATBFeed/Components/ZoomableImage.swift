import SwiftUI

public struct ZoomableImage<Overlay: View>: View {
    let imageURL: URL?
    let thumbnailURL: URL?
    let contentDescription: String
    @ViewBuilder let overlay: () -> Overlay

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    public init(
        imageURL: URL?,
        thumbnailURL: URL?,
        contentDescription: String,
        @ViewBuilder overlay: @escaping () -> Overlay = { EmptyView() }
    ) {
        self.imageURL = imageURL
        self.thumbnailURL = thumbnailURL
        self.contentDescription = contentDescription
        self.overlay = overlay
    }

    public var body: some View {
        ZStack {
            Color(.secondarySystemGroupedBackground)

            if let thumbnailURL {
                AsyncImage(url: thumbnailURL) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.clear
                }
            }

            AsyncImage(url: imageURL ?? thumbnailURL) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure:
                    Color(.systemRed).opacity(0.1)
                case .empty:
                    Color.clear
                @unknown default:
                    Color.clear
                }
            }

            overlay()
        }
        .aspectRatio(1, contentMode: .fit)
        .clipped()
        .scaleEffect(scale)
        .offset(offset)
        .gesture(
            MagnifyGesture()
                .simultaneously(with: DragGesture())
                .onChanged { value in
                    if let magnification = value.first?.magnification {
                        let newScale = lastScale * magnification
                        scale = min(max(newScale, 1.0), 5.0)
                    }
                    if let translation = value.second?.translation, scale > 1.0 {
                        offset = CGSize(
                            width: lastOffset.width + translation.width,
                            height: lastOffset.height + translation.height
                        )
                    }
                }
                .onEnded { _ in
                    withAnimation(.spring()) {
                        scale = 1.0
                        offset = .zero
                    }
                    lastScale = 1.0
                    lastOffset = .zero
                }
        )
        .accessibilityLabel(contentDescription)
    }
}
