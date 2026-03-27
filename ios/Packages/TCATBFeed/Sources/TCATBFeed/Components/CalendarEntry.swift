import SwiftUI

public struct CalendarPhoto: Identifiable {
    public let id: String
    public let submissionId: String
    public let imageURL: URL

    public init(submissionId: String, imageURL: URL) {
        self.id = submissionId
        self.submissionId = submissionId
        self.imageURL = imageURL
    }
}

public struct CalendarEntry: View {
    let dateLabel: String
    let yearLabel: String?
    let photos: [CalendarPhoto]
    var onImageClick: (String) -> Void

    public init(
        dateLabel: String,
        yearLabel: String? = nil,
        photos: [CalendarPhoto],
        onImageClick: @escaping (String) -> Void
    ) {
        self.dateLabel = dateLabel
        self.yearLabel = yearLabel
        self.photos = photos
        self.onImageClick = onImageClick
    }

    public var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading) {
                Text(dateLabel)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if let yearLabel {
                    Text(yearLabel)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 56, alignment: .leading)
            .padding(.top, 4)

            let rows = photos.chunked(into: 3)
            VStack(spacing: 2) {
                ForEach(Array(rows.enumerated()), id: \.offset) { _, rowPhotos in
                    HStack(spacing: 2) {
                        ForEach(rowPhotos) { photo in
                            Button {
                                onImageClick(photo.submissionId)
                            } label: {
                                AsyncImage(url: photo.imageURL) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Color(.secondarySystemGroupedBackground)
                                }
                                .aspectRatio(1, contentMode: .fill)
                                .clipped()
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Photo")
                        }
                        // Fill remaining space in incomplete rows
                        ForEach(0..<(3 - rowPhotos.count), id: \.self) { _ in
                            Color.clear.aspectRatio(1, contentMode: .fill)
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}

extension Array {
    func chunked(into size: Int) -> [[Element]] {
        stride(from: 0, to: count, by: size).map {
            Array(self[$0..<Swift.min($0 + size, count)])
        }
    }
}
