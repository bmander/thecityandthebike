import Foundation

public enum APIEnvironment {
    case staging
    case production

    public var baseURL: URL {
        switch self {
        case .staging:
            return URL(string: "https://tcatb-api-staging-821600862601.us-central1.run.app")!
        case .production:
            return URL(string: "https://tcatb-api-production-gj6zdcsumq-uc.a.run.app")!
        }
    }
}
