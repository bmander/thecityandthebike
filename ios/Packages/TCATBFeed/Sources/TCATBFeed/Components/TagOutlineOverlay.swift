import SwiftUI
import TCATBModels

public struct TagOutlineOverlay: View {
    let tags: [TagResponse]
    let selectedTagId: String?
    var onTagTapped: (String?) -> Void
    var isTagDeletable: (TagResponse) -> Bool
    var onDeleteTag: (String) -> Void
    var onTagClick: ((String) -> Void)?

    public init(
        tags: [TagResponse],
        selectedTagId: String? = nil,
        onTagTapped: @escaping (String?) -> Void = { _ in },
        isTagDeletable: @escaping (TagResponse) -> Bool = { _ in false },
        onDeleteTag: @escaping (String) -> Void = { _ in },
        onTagClick: ((String) -> Void)? = nil
    ) {
        self.tags = tags
        self.selectedTagId = selectedTagId
        self.onTagTapped = onTagTapped
        self.isTagDeletable = isTagDeletable
        self.onDeleteTag = onDeleteTag
        self.onTagClick = onTagClick
    }

    public var body: some View {
        GeometryReader { geometry in
            let size = geometry.size

            Canvas { context, canvasSize in
                guard canvasSize.width > 0, canvasSize.height > 0 else { return }

                let outlineColor = Color(red: 0, green: 0.9, blue: 0.46) // #00E676

                for tag in tags {
                    guard let ring = tag.ring,
                          let ringWidth = tag.ringWidth,
                          let ringHeight = tag.ringHeight,
                          ring.count >= 3,
                          ringWidth > 0,
                          ringHeight > 0
                    else { continue }

                    let scaleX = canvasSize.width / CGFloat(ringWidth)
                    let scaleY = canvasSize.height / CGFloat(ringHeight)
                    let strokeWidth: CGFloat = tag.tagId == selectedTagId ? 8 : 3

                    var path = Path()
                    for i in ring.indices {
                        let start = ring[i]
                        let end = ring[(i + 1) % ring.count]
                        path.move(to: CGPoint(x: CGFloat(start[0]) * scaleX, y: CGFloat(start[1]) * scaleY))
                        path.addLine(to: CGPoint(x: CGFloat(end[0]) * scaleX, y: CGFloat(end[1]) * scaleY))
                    }

                    context.stroke(path, with: .color(outlineColor), style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { location in
                guard size.width > 0, size.height > 0 else { return }

                let tappedTag = tags.first { tag in
                    guard let ring = tag.ring,
                          let ringWidth = tag.ringWidth,
                          let ringHeight = tag.ringHeight,
                          ring.count >= 3,
                          ringWidth > 0,
                          ringHeight > 0
                    else { return false }

                    let scaleX = size.width / CGFloat(ringWidth)
                    let scaleY = size.height / CGFloat(ringHeight)
                    let scaledRing = ring.map { [CGFloat($0[0]) * scaleX, CGFloat($0[1]) * scaleY] }
                    return isPointInPolygon(x: location.x, y: location.y, polygon: scaledRing)
                }

                if tappedTag?.tagId == selectedTagId, selectedTagId != nil {
                    onTagTapped(nil)
                } else {
                    onTagTapped(tappedTag?.tagId)
                }
            }

            // Delete button for selected tag
            if let selectedTagId,
               let selectedTag = tags.first(where: { $0.tagId == selectedTagId }),
               isTagDeletable(selectedTag),
               let ring = selectedTag.ring,
               let ringWidth = selectedTag.ringWidth,
               let ringHeight = selectedTag.ringHeight,
               ring.count >= 3, ringWidth > 0, ringHeight > 0 {
                let scaleX = size.width / CGFloat(ringWidth)
                let scaleY = size.height / CGFloat(ringHeight)
                let maxX = ring.map { CGFloat($0[0]) * scaleX }.max() ?? 0
                let minY = ring.map { CGFloat($0[1]) * scaleY }.min() ?? 0

                Button {
                    onDeleteTag(selectedTagId)
                } label: {
                    Image(systemName: "trash.fill")
                        .font(.caption2)
                        .foregroundStyle(.white)
                        .frame(width: 21, height: 21)
                        .background(Color.red)
                        .clipShape(Circle())
                }
                .position(x: maxX, y: minY)
            }

            // Navigate button for selected tag
            if let selectedTagId,
               let selectedTag = tags.first(where: { $0.tagId == selectedTagId }),
               let onTagClick,
               let ring = selectedTag.ring,
               let ringWidth = selectedTag.ringWidth,
               let ringHeight = selectedTag.ringHeight,
               ring.count >= 3, ringWidth > 0, ringHeight > 0 {
                let scaleX = size.width / CGFloat(ringWidth)
                let scaleY = size.height / CGFloat(ringHeight)
                let maxX = ring.map { CGFloat($0[0]) * scaleX }.max() ?? 0
                let maxY = ring.map { CGFloat($0[1]) * scaleY }.max() ?? 0

                Button {
                    onTagClick(selectedTagId)
                } label: {
                    Image(systemName: "arrow.right")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .frame(width: 21, height: 21)
                        .background(Color(.secondarySystemGroupedBackground))
                        .clipShape(Circle())
                }
                .position(x: maxX, y: maxY)
            }
        }
    }
}

func isPointInPolygon(x: CGFloat, y: CGFloat, polygon: [[CGFloat]]) -> Bool {
    var inside = false
    var j = polygon.count - 1
    for i in polygon.indices {
        let xi = polygon[i][0]
        let yi = polygon[i][1]
        let xj = polygon[j][0]
        let yj = polygon[j][1]
        if ((yi > y) != (yj > y)) &&
            (x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
            inside = !inside
        }
        j = i
    }
    return inside
}
