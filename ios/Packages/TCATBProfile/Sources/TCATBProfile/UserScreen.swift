import SwiftUI
import TCATBModels
import TCATBSharedUI

public struct UserScreen: View {
    @Bindable var viewModel: UserViewModel
    var imageBaseURL: String
    var onBack: () -> Void
    var onImageClick: (String) -> Void

    @State private var showBanConfirmation = false

    public init(
        viewModel: UserViewModel,
        imageBaseURL: String,
        onBack: @escaping () -> Void,
        onImageClick: @escaping (String) -> Void
    ) {
        self.viewModel = viewModel
        self.imageBaseURL = imageBaseURL
        self.onBack = onBack
        self.onImageClick = onImageClick
    }

    public var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.error {
                Text(error)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                userContent
            }
        }
        .navigationTitle(viewModel.userDetail?.username ?? "User")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                }
            }
        }
        .task {
            await viewModel.loadUser()
        }
    }

    @ViewBuilder
    private var userContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                if let detail = viewModel.userDetail {
                    userHeader(detail: detail)
                }

                submissionsGallery
            }
        }
    }

    @ViewBuilder
    private func userHeader(detail: UserDetailResponse) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            if detail.isAdmin {
                Text("Admin")
                    .font(.caption)
                    .foregroundStyle(.tint)
            }

            Text("\(detail.submissionCount) photo\(detail.submissionCount != 1 ? "s" : "")")
                .font(.title3)

            Text("\(detail.ownedBikeCount) bike\(detail.ownedBikeCount != 1 ? "s" : "") owned")
                .font(.title3)

            let ranksText = detail.leaderboardRanks.map { rank in
                let label = rank.period.prefix(1).uppercased() + rank.period.dropFirst()
                let value = rank.rank.map { "#\($0)" } ?? "--"
                return "\(label): \(value)"
            }.joined(separator: " | ")
            Text(ranksText)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            if let firstSeen = detail.firstSeenAt {
                Text("First seen: \(DateFormatting.formatDisplayDate(firstSeen) ?? firstSeen)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            if let lastSeen = detail.lastSeenAt {
                Text("Last seen: \(DateFormatting.formatDisplayDate(lastSeen) ?? lastSeen)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            if viewModel.currentUserIsAdmin && !detail.isAdmin {
                banButton(detail: detail)
                    .padding(.top, 8)
            }
        }
        .padding()
    }

    @ViewBuilder
    private func banButton(detail: UserDetailResponse) -> some View {
        Button {
            showBanConfirmation = true
        } label: {
            if viewModel.isBanning {
                ProgressView()
                    .frame(maxWidth: .infinity)
            } else {
                Text(detail.isBanned ? "Unban User" : "Ban User")
                    .frame(maxWidth: .infinity)
            }
        }
        .buttonStyle(.borderedProminent)
        .tint(detail.isBanned ? .accentColor : .red)
        .disabled(viewModel.isBanning)
        .confirmationDialog(
            detail.isBanned ? "Unban User" : "Ban User",
            isPresented: $showBanConfirmation,
            titleVisibility: .visible
        ) {
            Button(detail.isBanned ? "Unban" : "Ban", role: detail.isBanned ? nil : .destructive) {
                Task {
                    if detail.isBanned {
                        await viewModel.unbanUser()
                    } else {
                        await viewModel.banUser()
                    }
                }
            }
        } message: {
            if detail.isBanned {
                Text("Are you sure you want to unban \(detail.username)? They will be able to post content and log in again.")
            } else {
                Text("Are you sure you want to ban \(detail.username)? They will no longer be able to post content or log in.")
            }
        }
    }

    private var submissionsGallery: some View {
        SubmissionsGallery(
            submissions: viewModel.submissions,
            imageBaseURL: imageBaseURL,
            onImageTapped: onImageClick,
            onLastAppeared: { Task { await viewModel.loadMoreSubmissions() } }
        )
    }
}

