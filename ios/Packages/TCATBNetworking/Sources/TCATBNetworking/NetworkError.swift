import Foundation

public enum NetworkError: Error, Equatable {
    case unauthorized
    case forbidden
    case notFound
    case validationError(String)
    case rateLimited(retryAfter: Int?)
    case serverError(statusCode: Int, message: String)
    case networkFailure(String)
    case decodingError(String)
    case unknown(String)

    public static func fromHTTPStatus(_ statusCode: Int, body: Data?) -> NetworkError {
        let message = body.flatMap { parseErrorMessage(from: $0) } ?? "Request failed"
        switch statusCode {
        case 401:
            return .unauthorized
        case 403:
            return .forbidden
        case 404:
            return .notFound
        case 422:
            return .validationError(message)
        case 429:
            return .rateLimited(retryAfter: nil)
        case 500...599:
            return .serverError(statusCode: statusCode, message: message)
        default:
            return .unknown("HTTP \(statusCode): \(message)")
        }
    }

    private static func parseErrorMessage(from data: Data) -> String? {
        struct ErrorDetail: Decodable {
            let msg: String
        }
        struct ErrorResponse: Decodable {
            let detail: ErrorDetail?
        }
        if let errorResponse = try? JSONDecoder().decode(ErrorResponse.self, from: data) {
            return errorResponse.detail?.msg
        }
        if let stringDetail = try? JSONDecoder().decode([String: String].self, from: data),
           let detail = stringDetail["detail"] {
            return detail
        }
        return nil
    }

    public var displayMessage: String {
        switch self {
        case .unauthorized:
            return "Your session has expired. Please log in again."
        case .forbidden:
            return "You don't have permission to perform this action."
        case .notFound:
            return "The requested resource was not found."
        case .validationError(let message):
            return message
        case .rateLimited:
            return "Too many attempts. Please try again later."
        case .serverError(_, let message):
            return message
        case .networkFailure:
            return "Network error. Check your connection and try again."
        case .decodingError:
            return "An unexpected error occurred."
        case .unknown:
            return "An unexpected error occurred."
        }
    }
}
