import SwiftUI

public struct MeScreen: View {
    @Bindable var viewModel: MeViewModel
    @Bindable var deleteAccountViewModel: DeleteAccountViewModel
    var onBack: () -> Void
    var onImageClick: (String) -> Void
    var onLogout: () -> Void
    var onDeleted: () -> Void

    @State private var showDeleteConfirmation = false

    public init(
        viewModel: MeViewModel,
        deleteAccountViewModel: DeleteAccountViewModel,
        onBack: @escaping () -> Void,
        onImageClick: @escaping (String) -> Void,
        onLogout: @escaping () -> Void,
        onDeleted: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.deleteAccountViewModel = deleteAccountViewModel
        self.onBack = onBack
        self.onImageClick = onImageClick
        self.onLogout = onLogout
        self.onDeleted = onDeleted
    }

    public var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.error ?? deleteAccountViewModel.error {
                VStack(spacing: 16) {
                    Text(error)
                        .foregroundStyle(.red)
                    Button("Dismiss") {
                        viewModel.clearError()
                        deleteAccountViewModel.clearError()
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                meContent
            }
        }
        .navigationTitle("Me")
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
        .onChange(of: deleteAccountViewModel.isDeleted) { _, isDeleted in
            if isDeleted {
                onDeleted()
            }
        }
    }

    @ViewBuilder
    private var meContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                if let detail = viewModel.userDetail {
                    meHeader(detail: detail)
                }

                accountActions

                submissionsGrid
            }
        }
    }

    @ViewBuilder
    private func meHeader(detail: UserDetailResponse) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            if detail.isAdmin {
                Text("Admin")
                    .font(.caption)
                    .foregroundStyle(.tint)
            }

            Text("\(detail.submissionCount) photo\(detail.submissionCount != 1 ? "s" : "")")
                .font(.title3)

            if let firstSeen = detail.firstSeenAt {
                Text("First seen: \(formatDateTime(firstSeen))")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            if let lastSeen = detail.lastSeenAt {
                Text("Last seen: \(formatDateTime(lastSeen))")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
    }

    @ViewBuilder
    private var accountActions: some View {
        VStack(spacing: 12) {
            Button("Log Out", action: onLogout)
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                showDeleteConfirmation = true
            } label: {
                if deleteAccountViewModel.isDeleting {
                    ProgressView()
                } else {
                    Text("Delete My Account")
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(.red)
            .disabled(deleteAccountViewModel.isDeleting)
            .confirmationDialog(
                "Are you sure?",
                isPresented: $showDeleteConfirmation,
                titleVisibility: .visible
            ) {
                Button("Delete", role: .destructive) {
                    Task {
                        await deleteAccountViewModel.deleteAccount()
                    }
                }
            } message: {
                Text("This will permanently delete your account and all your data. This cannot be undone.")
            }
        }
        .padding(.horizontal)
    }

    @ViewBuilder
    private var submissionsGrid: some View {
        let columns = [
            GridItem(.flexible(), spacing: 2),
            GridItem(.flexible(), spacing: 2),
            GridItem(.flexible(), spacing: 2),
        ]

        LazyVGrid(columns: columns, spacing: 2) {
            ForEach(viewModel.submissions) { submission in
                if let imageUrl = submission.imageUrlThumbnail ?? submission.imageUrl,
                   let url = URL(string: imageUrl) {
                    Button {
                        onImageClick(submission.submissionId)
                    } label: {
                        AsyncImage(url: url) { image in
                            image
                                .resizable()
                                .aspectRatio(1, contentMode: .fill)
                                .clipped()
                        } placeholder: {
                            Color.gray.opacity(0.2)
                                .aspectRatio(1, contentMode: .fill)
                        }
                    }
                }
            }
        }
        .padding(.top, 8)
        .onAppear {
            Task {
                await viewModel.loadMoreSubmissions()
            }
        }
    }
}

private func formatDateTime(_ isoString: String) -> String {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    if let date = formatter.date(from: isoString) {
        let displayFormatter = DateFormatter()
        displayFormatter.dateFormat = "MMM d, yyyy"
        return displayFormatter.string(from: date)
    }
    formatter.formatOptions = [.withInternetDateTime]
    if let date = formatter.date(from: isoString) {
        let displayFormatter = DateFormatter()
        displayFormatter.dateFormat = "MMM d, yyyy"
        return displayFormatter.string(from: date)
    }
    return isoString
}
