import SwiftUI

public struct AdminFlagActions: View {
    let flagCount: Int
    let isClearingFlags: Bool
    let onClearFlags: () -> Void

    public init(flagCount: Int, isClearingFlags: Bool, onClearFlags: @escaping () -> Void) {
        self.flagCount = flagCount
        self.isClearingFlags = isClearingFlags
        self.onClearFlags = onClearFlags
    }

    public var body: some View {
        HStack {
            Text("\(flagCount) \(flagCount == 1 ? "flag" : "flags")")
                .font(.body)
                .foregroundStyle(.red)

            Spacer()

            Button(action: onClearFlags) {
                if isClearingFlags {
                    ProgressView()
                        .controlSize(.small)
                } else {
                    Text("Clear flags")
                        .foregroundStyle(.red)
                }
            }
            .buttonStyle(.bordered)
            .tint(.red)
            .disabled(isClearingFlags)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}
