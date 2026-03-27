import SwiftUI

public struct MenuButton: View {
    private let action: () -> Void

    public init(action: @escaping () -> Void) {
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Image(systemName: "line.3.horizontal")
                .foregroundStyle(.primary)
        }
    }
}
