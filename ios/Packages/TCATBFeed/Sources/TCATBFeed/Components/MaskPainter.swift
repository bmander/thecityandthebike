import SwiftUI
import CoreGraphics
#if canImport(UIKit)
import UIKit
#endif

private let strokeWidth: CGFloat = 40
private let maxMaskDim: Int = 512

struct StrokePoint {
    let x: CGFloat
    let y: CGFloat
}

struct StrokePath {
    let points: [StrokePoint]
}

public struct MaskPainter: View {
    let imageURL: URL?
    let thumbnailURL: URL?
    var onExitTagMode: () -> Void
    var isProcessingMask: Bool
    /// Called with (maskPNGData, maskWidth, maskHeight) when the user taps Next.
    var onMaskExported: ((Data, Int, Int) -> Void)?

    @State private var strokes: [StrokePath] = []
    @State private var currentStroke: [StrokePoint] = []
    @State private var canvasSize: CGSize = .zero

    private var hasStrokes: Bool { !strokes.isEmpty }

    public init(
        imageURL: URL?,
        thumbnailURL: URL?,
        onExitTagMode: @escaping () -> Void,
        isProcessingMask: Bool = false,
        onMaskExported: ((Data, Int, Int) -> Void)? = nil
    ) {
        self.imageURL = imageURL
        self.thumbnailURL = thumbnailURL
        self.onExitTagMode = onExitTagMode
        self.isProcessingMask = isProcessingMask
        self.onMaskExported = onMaskExported
    }

    public var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Color(.secondarySystemGroupedBackground)

                AsyncImage(url: imageURL ?? thumbnailURL) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.clear
                }

                // Semi-transparent overlay with strokes erasing through
                Canvas { context, size in
                    // Draw the semi-transparent overlay
                    context.fill(
                        Path(CGRect(origin: .zero, size: size)),
                        with: .color(.black.opacity(0.5))
                    )

                    // Erase stroke paths using blending
                    context.blendMode = .destinationOut
                    let allPaths = strokes + (currentStroke.count >= 2 ? [StrokePath(points: currentStroke)] : [])
                    for stroke in allPaths {
                        if stroke.points.count >= 2 {
                            var path = Path()
                            path.move(to: CGPoint(x: stroke.points[0].x, y: stroke.points[0].y))
                            for i in 1..<stroke.points.count {
                                path.addLine(to: CGPoint(x: stroke.points[i].x, y: stroke.points[i].y))
                            }
                            context.stroke(
                                path,
                                with: .color(.black),
                                style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round, lineJoin: .round)
                            )
                        }
                    }
                }
                .drawingGroup(opaque: false)
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            let point = StrokePoint(x: value.location.x, y: value.location.y)
                            if currentStroke.isEmpty {
                                currentStroke = [point]
                            } else {
                                currentStroke.append(point)
                            }
                        }
                        .onEnded { _ in
                            if !currentStroke.isEmpty {
                                strokes.append(StrokePath(points: currentStroke))
                                currentStroke = []
                            }
                        }
                )
            }
            .aspectRatio(1, contentMode: .fit)
            .clipped()
            .background(
                GeometryReader { geometry in
                    Color.clear
                        .onAppear { canvasSize = geometry.size }
                        .onChange(of: geometry.size) { _, newSize in canvasSize = newSize }
                }
            )

            TagModeControls(
                mode: .draw,
                isProcessing: isProcessingMask,
                hasStrokes: hasStrokes,
                onDiscard: onExitTagMode,
                onNext: {
                    guard canvasSize.width > 0, canvasSize.height > 0 else { return }
                    guard let maskData = exportMaskPNG() else { return }
                    onMaskExported?(maskData.data, maskData.width, maskData.height)
                }
            )
        }
    }

    /// Renders the strokes as a black-and-white PNG at a capped resolution.
    private func exportMaskPNG() -> (data: Data, width: Int, height: Int)? {
        guard !strokes.isEmpty, canvasSize.width > 0, canvasSize.height > 0 else { return nil }

        let aspectWidth = canvasSize.width
        let aspectHeight = canvasSize.height
        let maxDim = max(aspectWidth, aspectHeight)
        let scale: CGFloat = maxDim > CGFloat(maxMaskDim) ? CGFloat(maxMaskDim) / maxDim : 1.0
        let targetWidth = Int(aspectWidth * scale)
        let targetHeight = Int(aspectHeight * scale)

        guard targetWidth > 0, targetHeight > 0 else { return nil }

        let scaleX = CGFloat(targetWidth) / canvasSize.width
        let scaleY = CGFloat(targetHeight) / canvasSize.height
        let scaledStrokeWidth = strokeWidth * scaleX

        let colorSpace = CGColorSpaceCreateDeviceRGB()
        guard let ctx = CGContext(
            data: nil,
            width: targetWidth,
            height: targetHeight,
            bitsPerComponent: 8,
            bytesPerRow: targetWidth * 4,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else { return nil }

        // Black background
        ctx.setFillColor(CGColor(red: 0, green: 0, blue: 0, alpha: 1))
        ctx.fill(CGRect(x: 0, y: 0, width: targetWidth, height: targetHeight))

        // White strokes
        ctx.setStrokeColor(CGColor(red: 1, green: 1, blue: 1, alpha: 1))
        ctx.setLineWidth(scaledStrokeWidth)
        ctx.setLineCap(.round)
        ctx.setLineJoin(.round)

        // Flip Y since Core Graphics has origin at bottom-left
        ctx.translateBy(x: 0, y: CGFloat(targetHeight))
        ctx.scaleBy(x: 1, y: -1)

        for stroke in strokes {
            guard stroke.points.count >= 2 else { continue }
            ctx.beginPath()
            ctx.move(to: CGPoint(x: stroke.points[0].x * scaleX, y: stroke.points[0].y * scaleY))
            for i in 1..<stroke.points.count {
                ctx.addLine(to: CGPoint(x: stroke.points[i].x * scaleX, y: stroke.points[i].y * scaleY))
            }
            ctx.strokePath()
        }

        guard let cgImage = ctx.makeImage() else { return nil }

        #if canImport(UIKit)
        let uiImage = UIImage(cgImage: cgImage)
        guard let pngData = uiImage.pngData() else { return nil }
        return (pngData, targetWidth, targetHeight)
        #else
        return nil
        #endif
    }
}
