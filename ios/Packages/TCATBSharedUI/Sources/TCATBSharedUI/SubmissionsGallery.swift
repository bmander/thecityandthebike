import SwiftUI
import TCATBModels

// MARK: - Date grouping

public struct DateGroup: Identifiable {
    public let id: String
    public let dateLabel: String
    public let yearLabel: String?
    public let submissions: [SubmissionResponse]

    public init(dateLabel: String, yearLabel: String?, submissions: [SubmissionResponse]) {
        self.id = dateLabel + (yearLabel ?? "")
        self.dateLabel = dateLabel
        self.yearLabel = yearLabel
        self.submissions = submissions
    }
}

public func groupSubmissionsByDate(_ submissions: [SubmissionResponse]) -> [DateGroup] {
    let calendar = Calendar.current
    let now = Date()
    let currentYear = calendar.component(.year, from: now)

    var groups: [(key: String, yearLabel: String?, submissions: [SubmissionResponse])] = []
    var currentGroup: (key: String, yearLabel: String?, submissions: [SubmissionResponse])?

    for submission in submissions {
        let dateString = submission.capturedDate ?? submission.uploadedAt
        guard let dateString, let date = DateFormatting.parseDate(dateString) else { continue }

        let label = DateFormatting.formatMonthDay(date)
        let year = calendar.component(.year, from: date)
        let yearLabel: String? = year != currentYear ? String(year) : nil
        let groupKey = label + (yearLabel ?? "")

        if let current = currentGroup, current.key == groupKey {
            currentGroup?.submissions.append(submission)
        } else {
            if let current = currentGroup {
                groups.append(current)
            }
            currentGroup = (key: groupKey, yearLabel: yearLabel, submissions: [submission])
        }
    }
    if let current = currentGroup {
        groups.append(current)
    }

    return groups.map { group in
        let dateLabel = group.key.replacingOccurrences(of: group.yearLabel ?? "", with: "")
        return DateGroup(dateLabel: dateLabel, yearLabel: group.yearLabel, submissions: group.submissions)
    }
}

public func formattedDate(_ dateString: String?) -> String {
    guard let dateString, let date = DateFormatting.parseDate(dateString) else { return "" }
    return " on \(DateFormatting.formatDisplayDate(date))"
}

// MARK: - Submissions gallery

public struct SubmissionsGallery: View {
    let submissions: [SubmissionResponse]
    let imageBaseURL: String
    let onImageTapped: (String) -> Void
    let onLastAppeared: () -> Void

    public init(
        submissions: [SubmissionResponse],
        imageBaseURL: String,
        onImageTapped: @escaping (String) -> Void,
        onLastAppeared: @escaping () -> Void
    ) {
        self.submissions = submissions
        self.imageBaseURL = imageBaseURL
        self.onImageTapped = onImageTapped
        self.onLastAppeared = onLastAppeared
    }

    public var body: some View {
        let dateGroups = groupSubmissionsByDate(submissions)
        ForEach(dateGroups) { group in
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(group.dateLabel)
                        .font(.headline)
                    if let yearLabel = group.yearLabel {
                        Text(yearLabel)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.horizontal)
                .padding(.top, 12)

                LazyVGrid(columns: [
                    GridItem(.flexible(), spacing: 2),
                    GridItem(.flexible(), spacing: 2),
                    GridItem(.flexible(), spacing: 2)
                ], spacing: 2) {
                    ForEach(group.submissions) { submission in
                        SubmissionThumbnail(submission: submission, imageBaseURL: imageBaseURL) {
                            onImageTapped(submission.submissionId)
                        }
                        .onAppear {
                            if submission.id == submissions.last?.id {
                                onLastAppeared()
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Submission thumbnail

public struct SubmissionThumbnail: View {
    let submission: SubmissionResponse
    let imageBaseURL: String
    let onTap: () -> Void

    public init(submission: SubmissionResponse, imageBaseURL: String, onTap: @escaping () -> Void) {
        self.submission = submission
        self.imageBaseURL = imageBaseURL
        self.onTap = onTap
    }

    public var body: some View {
        Button(action: onTap) {
            Color.clear
                .aspectRatio(1, contentMode: .fit)
                .overlay {
                    AsyncImage(url: thumbnailURL) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                        case .failure:
                            Image(systemName: "photo")
                                .font(.title3)
                                .foregroundStyle(.secondary)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .background(.quaternary)
                        default:
                            ProgressView()
                        }
                    }
                }
                .clipped()
        }
        .buttonStyle(.plain)
    }

    private var thumbnailURL: URL? {
        if let thumb = submission.imageUrlThumbnail {
            return imageUrl(from: thumb, baseURL: imageBaseURL)
        }
        if let url = submission.imageUrl {
            return imageUrl(from: url, baseURL: imageBaseURL)
        }
        return nil
    }
}
