import SwiftUI

// MARK: - Date grouping

struct DateGroup: Identifiable {
    let id: String
    let dateLabel: String
    let yearLabel: String?
    let submissions: [SubmissionResponse]

    init(dateLabel: String, yearLabel: String?, submissions: [SubmissionResponse]) {
        self.id = dateLabel + (yearLabel ?? "")
        self.dateLabel = dateLabel
        self.yearLabel = yearLabel
        self.submissions = submissions
    }
}

func groupSubmissionsByDate(_ submissions: [SubmissionResponse]) -> [DateGroup] {
    let calendar = Calendar.current
    let now = Date()
    let currentYear = calendar.component(.year, from: now)

    let monthDayFormatter = DateFormatter()
    monthDayFormatter.dateFormat = "MMM d"

    var groups: [(key: String, yearLabel: String?, submissions: [SubmissionResponse])] = []
    var currentGroup: (key: String, yearLabel: String?, submissions: [SubmissionResponse])?

    for submission in submissions {
        let dateString = submission.capturedDate ?? submission.uploadedAt
        guard let dateString, let date = parseDate(dateString) else { continue }

        let label = monthDayFormatter.string(from: date)
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

func parseDate(_ string: String) -> Date? {
    let isoFormatter = ISO8601DateFormatter()
    isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    if let date = isoFormatter.date(from: string) { return date }

    isoFormatter.formatOptions = [.withInternetDateTime]
    if let date = isoFormatter.date(from: string) { return date }

    let dateOnly = DateFormatter()
    dateOnly.dateFormat = "yyyy-MM-dd"
    return dateOnly.date(from: String(string.prefix(10)))
}

// MARK: - Submission thumbnail

struct SubmissionThumbnail: View {
    let submission: SubmissionResponse
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            AsyncImage(url: thumbnailURL) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(1, contentMode: .fill)
                case .failure:
                    Image(systemName: "photo")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity)
                        .aspectRatio(1, contentMode: .fill)
                        .background(.quaternary)
                default:
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .aspectRatio(1, contentMode: .fill)
                }
            }
            .clipped()
        }
        .buttonStyle(.plain)
    }

    private var thumbnailURL: URL? {
        if let thumb = submission.imageUrlThumbnail {
            return URL(string: thumb)
        }
        if let url = submission.imageUrl {
            return URL(string: url)
        }
        return nil
    }
}
