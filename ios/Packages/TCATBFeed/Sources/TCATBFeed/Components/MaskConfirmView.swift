import SwiftUI

public struct MaskConfirmView: View {
    let imageURL: URL?
    let thumbnailURL: URL?
    let ring: [[Float]]
    let maskWidth: Int
    let maskHeight: Int

    public init(
        imageURL: URL?,
        thumbnailURL: URL?,
        ring: [[Float]],
        maskWidth: Int,
        maskHeight: Int
    ) {
        self.imageURL = imageURL
        self.thumbnailURL = thumbnailURL
        self.ring = ring
        self.maskWidth = maskWidth
        self.maskHeight = maskHeight
    }

    public var body: some View {
        ZStack {
            Color(.secondarySystemGroupedBackground)

            AsyncImage(url: imageURL ?? thumbnailURL) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Color.clear
            }

            if ring.count >= 3 {
                GeometryReader { geometry in
                    let scaleX = geometry.size.width / CGFloat(maskWidth)
                    let scaleY = geometry.size.height / CGFloat(maskHeight)

                    Canvas { context, _ in
                        let outlineColor = Color(red: 0, green: 0.9, blue: 0.46)

                        var path = Path()
                        for i in ring.indices {
                            let start = ring[i]
                            let end = ring[(i + 1) % ring.count]
                            path.move(to: CGPoint(
                                x: CGFloat(start[0]) * scaleX,
                                y: CGFloat(start[1]) * scaleY
                            ))
                            path.addLine(to: CGPoint(
                                x: CGFloat(end[0]) * scaleX,
                                y: CGFloat(end[1]) * scaleY
                            ))
                        }

                        context.stroke(
                            path,
                            with: .color(outlineColor),
                            style: StrokeStyle(lineWidth: 3, lineCap: .round)
                        )
                    }
                }
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .clipped()
    }
}
