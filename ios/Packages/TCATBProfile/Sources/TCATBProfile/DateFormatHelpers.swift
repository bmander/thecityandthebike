import Foundation

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

private let displayDateFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "MMM d, yyyy"
    return f
}()

/// Format an ISO 8601 date string as "MMM d, yyyy" for display.
/// Returns the original string if parsing fails.
func formatDateTime(_ isoString: String) -> String {
    if let date = iso8601WithFractional.date(from: isoString) {
        return displayDateFormatter.string(from: date)
    }
    if let date = iso8601Plain.date(from: isoString) {
        return displayDateFormatter.string(from: date)
    }
    return isoString
}
