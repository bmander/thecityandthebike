import SwiftUI
import TCATBModels

public struct TagList: View {
    let tags: [TagResponse]
    let isTagOwner: (TagResponse) -> Bool
    let onDeleteTag: (String) -> Void

    public init(
        tags: [TagResponse],
        isTagOwner: @escaping (TagResponse) -> Bool,
        onDeleteTag: @escaping (String) -> Void
    ) {
        self.tags = tags
        self.isTagOwner = isTagOwner
        self.onDeleteTag = onDeleteTag
    }

    public var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(tags) { tag in
                    TagItem(
                        tag: tag,
                        isDeletable: isTagOwner(tag),
                        onDelete: { onDeleteTag(tag.tagId) }
                    )
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

struct TagItem: View {
    @Environment(\.imageBaseURL) private var imageBaseURL
    let tag: TagResponse
    let isDeletable: Bool
    let onDelete: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            AsyncImage(url: imageUrl(from: tag.imageUrl, baseURL: imageBaseURL)) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Color(.secondarySystemGroupedBackground)
            }
            .frame(width: 80, height: 80)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            if isDeletable {
                Button(action: onDelete) {
                    Image(systemName: "xmark")
                        .font(.caption2)
                        .foregroundStyle(.red)
                        .frame(width: 24, height: 24)
                }
            }
        }
    }
}
