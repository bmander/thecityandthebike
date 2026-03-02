import SwiftUI

public struct BikeScreen: View {
    let viewModel: BikeViewModel
    let onImageTapped: (String) -> Void
    let onUserTapped: (String) -> Void
    let onBack: () -> Void

    public init(
        viewModel: BikeViewModel,
        onImageTapped: @escaping (String) -> Void,
        onUserTapped: @escaping (String) -> Void,
        onBack: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.onImageTapped = onImageTapped
        self.onUserTapped = onUserTapped
        self.onBack = onBack
    }

    public var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.error {
                ContentUnavailableView {
                    Label("Error", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(error)
                } actions: {
                    Button("Retry") {
                        Task { await viewModel.loadBike() }
                    }
                }
            } else {
                bikeContent
            }
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                BikeIdBadge(
                    provider: viewModel.bikeDetail?.provider,
                    bikeQrId: viewModel.bikeQrId
                )
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                }
            }
        }
        .task {
            if viewModel.bikeDetail == nil {
                await viewModel.loadBike()
            }
        }
    }

    private var bikeContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                if let detail = viewModel.bikeDetail {
                    bikeInfoSection(detail)
                }

                submissionsGallery
            }
        }
    }

    private func bikeInfoSection(_ detail: BikeDetailResponse) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("\(detail.submissionCount) photo\(detail.submissionCount != 1 ? "s" : "")")
                .font(.title3)
                .fontWeight(.medium)
                .padding(.horizontal)
                .padding(.top)

            if !detail.owners.isEmpty {
                ForEach(detail.owners, id: \.user.id) { owner in
                    Button {
                        onUserTapped(owner.user.id)
                    } label: {
                        Label {
                            Text("\(owner.user.name) (\(owner.submissionCount) photo\(owner.submissionCount != 1 ? "s" : ""))")
                                .font(.body)
                        } icon: {
                            Image(systemName: "person.fill")
                        }
                    }
                    .buttonStyle(.plain)
                    .padding(.horizontal)
                }
            }

            if let firstBy = detail.firstCapturedBy {
                Button {
                    onUserTapped(firstBy.id)
                } label: {
                    Label {
                        Text("First captured by \(firstBy.name)\(formattedDate(detail.firstSeenAt))")
                            .font(.body)
                    } icon: {
                        Image(systemName: "person.fill")
                    }
                }
                .buttonStyle(.plain)
                .padding(.horizontal)
            }

            Divider()
                .padding(.top, 8)
        }
    }

    private var submissionsGallery: some View {
        let dateGroups = groupSubmissionsByDate(viewModel.submissions)
        return ForEach(dateGroups) { group in
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(group.dateLabel)
                        .font(.headline)
                    if let yearLabel = group.yearLabel {
                        Text(yearLabel)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.horizontal)
                .padding(.top, 12)

                LazyVGrid(columns: [
                    GridItem(.flexible(), spacing: 2),
                    GridItem(.flexible(), spacing: 2),
                    GridItem(.flexible(), spacing: 2)
                ], spacing: 2) {
                    ForEach(group.submissions) { submission in
                        SubmissionThumbnail(submission: submission) {
                            onImageTapped(submission.submissionId)
                        }
                        .onAppear {
                            if submission.id == viewModel.submissions.last?.id {
                                Task { await viewModel.loadMoreSubmissions() }
                            }
                        }
                    }
                }
            }
        }
    }

    private func formattedDate(_ dateString: String?) -> String {
        guard let dateString, let date = parseDate(dateString) else { return "" }
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy"
        return " on \(formatter.string(from: date))"
    }
}
