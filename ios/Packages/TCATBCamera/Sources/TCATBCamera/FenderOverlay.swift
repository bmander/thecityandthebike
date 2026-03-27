import SwiftUI

/// A SwiftUI overlay shape that shows a bike fender guide on the camera preview.
///
/// This draws a stylized fender outline to help the user position the bike
/// fender correctly in the frame. The shape can be mirrored for left/right sides.
public struct FenderOverlay: Shape {
    /// Whether to mirror the shape horizontally for the left fender.
    public var mirrored: Bool

    public init(mirrored: Bool = false) {
        self.mirrored = mirrored
    }

    public func path(in rect: CGRect) -> Path {
        var path = Path()

        // Fender arc outline - a stylized curved shape representing a bike fender.
        // The shape is drawn relative to the rect so it scales with the view.
        let width = rect.width
        let height = rect.height
        let centerX = width / 2
        let centerY = height / 2

        // Main fender curve - an arc from bottom-left to top-right
        let radius = min(width, height) * 0.4
        let startAngle = Angle.degrees(200)
        let endAngle = Angle.degrees(340)

        path.addArc(
            center: CGPoint(x: centerX, y: centerY + radius * 0.1),
            radius: radius,
            startAngle: startAngle,
            endAngle: endAngle,
            clockwise: false
        )

        // Inner arc (slightly smaller radius) to form the fender thickness
        let innerRadius = radius * 0.85
        path.addArc(
            center: CGPoint(x: centerX, y: centerY + radius * 0.1),
            radius: innerRadius,
            startAngle: endAngle,
            endAngle: startAngle,
            clockwise: true
        )

        path.closeSubpath()

        // Mounting bracket indicator - small rectangle near the top
        let bracketWidth: CGFloat = width * 0.06
        let bracketHeight: CGFloat = height * 0.08
        let bracketX = centerX - bracketWidth / 2
        let bracketY = centerY - radius * 0.7
        path.addRoundedRect(
            in: CGRect(x: bracketX, y: bracketY, width: bracketWidth, height: bracketHeight),
            cornerSize: CGSize(width: 2, height: 2)
        )

        if mirrored {
            let mirror = CGAffineTransform(translationX: width, y: 0)
                .scaledBy(x: -1, y: 1)
            return path.applying(mirror)
        }

        return path
    }
}

/// View modifier that adds the fender overlay guide to a camera preview.
public struct FenderOverlayModifier: ViewModifier {
    let side: FenderSide

    public func body(content: Content) -> some View {
        content.overlay {
            FenderOverlay(mirrored: side == .left)
                .stroke(Color.white.opacity(0.6), lineWidth: 2)
                .allowsHitTesting(false)
        }
    }
}

extension View {
    /// Adds a fender overlay guide to the view.
    public func fenderOverlay(side: FenderSide) -> some View {
        modifier(FenderOverlayModifier(side: side))
    }
}
