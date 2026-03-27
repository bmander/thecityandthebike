import Foundation

/// Cached date formatters for efficient reuse. DateFormatter allocation is
/// expensive and should not happen inside view body computations.
public enum DateFormatting {
    private static let iso8601WithFractional: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private static let iso8601Plain: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    private static let dateOnly: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    private static let displayDate: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy"
        return f
    }()

    private static let displayMonthDay: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f
    }()

    /// Parse a date string in ISO 8601 or "yyyy-MM-dd" format.
    public static func parseDate(_ string: String) -> Date? {
        if let date = iso8601WithFractional.date(from: string) { return date }
        if let date = iso8601Plain.date(from: string) { return date }
        return dateOnly.date(from: String(string.prefix(10)))
    }

    /// Format a date string as "MMM d, yyyy" for display.
    public static func formatDisplayDate(_ string: String) -> String? {
        guard let date = parseDate(string) else { return nil }
        return displayDate.string(from: date)
    }

    /// Format a Date as "MMM d, yyyy".
    public static func formatDisplayDate(_ date: Date) -> String {
        displayDate.string(from: date)
    }

    /// Format a Date as "MMM d".
    public static func formatMonthDay(_ date: Date) -> String {
        displayMonthDay.string(from: date)
    }

    /// Format a Date as "yyyy-MM-dd".
    public static func formatDateOnly(_ date: Date) -> String {
        dateOnly.string(from: date)
    }
}
