import SwiftUI

public struct OwnerActions: View {
    let isDeleting: Bool
    var onDownload: () -> Void
    var onShowDeleteDialog: () -> Void

    public init(
        isDeleting: Bool,
        onDownload: @escaping () -> Void,
        onShowDeleteDialog: @escaping () -> Void
    ) {
        self.isDeleting = isDeleting
        self.onDownload = onDownload
        self.onShowDeleteDialog = onShowDeleteDialog
    }

    public var body: some View {
        if isDeleting {
            ProgressView()
                .controlSize(.small)
        } else {
            HStack(spacing: 8) {
                Button(action: onDownload) {
                    Image(systemName: "arrow.down.circle")
                }
                .buttonStyle(.bordered)

                Button(action: onShowDeleteDialog) {
                    Image(systemName: "trash")
                }
                .buttonStyle(.borderedProminent)
                .tint(.red)
            }
        }
    }
}
