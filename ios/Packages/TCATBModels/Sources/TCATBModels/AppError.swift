import Foundation

public enum AppError: Error, Sendable, Equatable {
    case network(String)
    case auth(code: Int, message: String)
    case validation(field: String, message: String)
    case server(code: Int, message: String)
    case rateLimit(retryAfterSeconds: Int?)
    case unknown(String)

    public var displayMessage: String {
        switch self {
        case .network:
            return "Network error. Check your connection and try again."
        case .auth(_, let message):
            return message
        case .validation(_, let message):
            return message
        case .server(_, let message):
            return message
        case .rateLimit:
            return "Too many attempts. Please try again later."
        case .unknown:
            return "An unexpected error occurred."
        }
    }
}
