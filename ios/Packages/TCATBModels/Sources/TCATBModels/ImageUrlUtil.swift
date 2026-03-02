import Foundation

/// Convert an image path to a full URL. If the path already starts with "http",
/// it is returned as-is. Otherwise, it is prefixed with the given base URL.
public func imageUrl(from path: String, baseURL: String) -> URL? {
    if path.hasPrefix("http") {
        return URL(string: path)
    }
    return URL(string: baseURL + path)
}
