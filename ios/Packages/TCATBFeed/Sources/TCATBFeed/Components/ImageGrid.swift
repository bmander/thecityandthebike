import SwiftUI
import TCATBModels

public struct ImageGrid: View {
    @Environment(\.imageBaseURL) private var imageBaseURL
    let submissions: [SubmissionResponse]
    var onImageClick: ((String) -> Void)?
    var onLoadMore: (() -> Void)?

    private let columns = [
        GridItem(.adaptive(minimum: 120), spacing: 2)
    ]

    public init(
        submissions: [SubmissionResponse],
        onImageClick: ((String) -> Void)? = nil,
        onLoadMore: (() -> Void)? = nil
    ) {
        self.submissions = submissions
        self.onImageClick = onImageClick
        self.onLoadMore = onLoadMore
    }

    public var body: some View {
        LazyVGrid(columns: columns, spacing: 2) {
            ForEach(submissions) { submission in
                if let thumbnailUrl = submission.imageUrlThumbnail,
                   let url = imageUrl(from: thumbnailUrl, baseURL: imageBaseURL) {
                    GridCell(
                        url: url,
                        isFlagged: (submission.flagCount ?? 0) > 0,
                        onTap: {
                            onImageClick?(submission.submissionId)
                        }
                    )
                    .onAppear {
                        if submission.submissionId == submissions.suffix(6).first?.submissionId {
                            onLoadMore?()
                        }
                    }
                }
            }
        }
    }
}

private struct GridCell: View {
    let url: URL
    let isFlagged: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack {
                Color(.secondarySystemGroupedBackground)

                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        Color(.secondarySystemGroupedBackground)
                    case .empty:
                        ProgressView()
                    @unknown default:
                        Color(.secondarySystemGroupedBackground)
                    }
                }

                if isFlagged {
                    Color.black.opacity(0.3)
                    Image(systemName: "xmark")
                        .font(.title)
                        .foregroundStyle(.red)
                }
            }
            .aspectRatio(1, contentMode: .fill)
            .clipped()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isFlagged ? "Flagged photo" : "Photo")
    }
}
