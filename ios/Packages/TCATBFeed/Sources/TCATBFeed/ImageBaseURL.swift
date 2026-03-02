import SwiftUI

private struct ImageBaseURLKey: EnvironmentKey {
    static let defaultValue: String = ""
}

public extension EnvironmentValues {
    var imageBaseURL: String {
        get { self[ImageBaseURLKey.self] }
        set { self[ImageBaseURLKey.self] = newValue }
    }
}
