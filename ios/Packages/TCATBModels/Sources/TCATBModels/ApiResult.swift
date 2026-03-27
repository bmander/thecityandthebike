import Foundation

public enum ApiResult<T: Sendable>: Sendable {
    case success(T)
    case failure(AppError)

    public var value: T? {
        if case .success(let data) = self { return data }
        return nil
    }

    public var error: AppError? {
        if case .failure(let error) = self { return error }
        return nil
    }

    public func map<U: Sendable>(_ transform: (T) -> U) -> ApiResult<U> {
        switch self {
        case .success(let data):
            return .success(transform(data))
        case .failure(let error):
            return .failure(error)
        }
    }
}
