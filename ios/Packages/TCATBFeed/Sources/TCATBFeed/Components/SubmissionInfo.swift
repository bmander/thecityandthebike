import SwiftUI
import TCATBModels

public struct SubmissionInfo: View {
    let submission: SubmissionResponse
    let formattedDate: String?
    var onUserClick: ((String) -> Void)?
    var onBikeClick: ((String) -> Void)?

    public init(
        submission: SubmissionResponse,
        formattedDate: String?,
        onUserClick: ((String) -> Void)? = nil,
        onBikeClick: ((String) -> Void)? = nil
    ) {
        self.submission = submission
        self.formattedDate = formattedDate
        self.onUserClick = onUserClick
        self.onBikeClick = onBikeClick
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let username = submission.username {
                InfoRow(
                    systemImage: "person",
                    label: "User",
                    navigable: onUserClick != nil,
                    action: { onUserClick?(submission.userId) }
                ) {
                    Text(username)
                        .font(.headline)
                }
            }

            InfoRow(
                systemImage: "bicycle",
                label: "Bike",
                navigable: onBikeClick != nil,
                action: { onBikeClick?(submission.bikeQrId) }
            ) {
                BikeIdBadge(provider: submission.provider, bikeQrId: submission.bikeQrId)
            }

            if let date = formattedDate {
                InfoRow(systemImage: "calendar", label: "Date") {
                    Text(date)
                        .font(.headline)
                }
            }
        }
        .padding(.horizontal, 32)
    }
}

struct InfoRow<Content: View>: View {
    let systemImage: String
    let label: String
    var navigable: Bool = false
    var action: (() -> Void)?
    @ViewBuilder let content: () -> Content

    var body: some View {
        let row = HStack {
            Image(systemName: systemImage)
                .frame(width: 22)
                .foregroundStyle(.secondary)
            content()
            if navigable {
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(.secondary)
            }
        }

        if let action, navigable {
            Button(action: action) { row }
                .buttonStyle(.plain)
        } else {
            row
        }
    }
}
