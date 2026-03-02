import SwiftUI

// MARK: - Cached formatters

private let iso8601WithFractional: ISO8601DateFormatter = {
    let f = ISO8601DateFormatter()
    f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return f
}()

private let iso8601Plain: ISO8601DateFormatter = {
    let f = ISO8601DateFormatter()
    f.formatOptions = [.withInternetDateTime]
    return f
}()

private let dateOnlyFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    return f
}()

let monthDayFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "MMM d"
    return f
}()

let displayDateFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "MMM d, yyyy"
    return f
}()

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
    if let date = iso8601WithFractional.date(from: string) { return date }
    if let date = iso8601Plain.date(from: string) { return date }
    return dateOnlyFormatter.date(from: String(string.prefix(10)))
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
