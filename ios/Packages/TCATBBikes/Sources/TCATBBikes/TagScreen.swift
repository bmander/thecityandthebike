import SwiftUI

public struct TagScreen: View {
    let viewModel: TagViewModel
    let onImageTapped: (String) -> Void
    let onUserTapped: (String) -> Void
    let onBack: () -> Void

    public init(
        viewModel: TagViewModel,
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
                        Task { await viewModel.loadTag() }
                    }
                }
            } else {
                tagContent
            }
        }
        .navigationTitle("Tag")
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
            if viewModel.tagDetail == nil {
                await viewModel.loadTag()
            }
        }
    }

    private var tagContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                if let detail = viewModel.tagDetail {
                    tagInfoSection(detail)
                }

                submissionsGallery
            }
        }
    }

    private func tagInfoSection(_ detail: TagDetailResponse) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            AsyncImage(url: URL(string: detail.imageUrl)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(1, contentMode: .fit)
                case .failure:
                    Image(systemName: "photo")
                        .font(.largeTitle)
                        .frame(maxWidth: .infinity)
                        .aspectRatio(1, contentMode: .fit)
                        .background(.quaternary)
                default:
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .aspectRatio(1, contentMode: .fit)
                }
            }

            Text("\(detail.submissionCount) photo\(detail.submissionCount != 1 ? "s" : "")")
                .font(.title3)
                .fontWeight(.medium)
                .padding(.horizontal)

            if let firstBy = detail.firstCapturedBy {
                Button {
                    onUserTapped(firstBy.id)
                } label: {
                    Label {
                        Text("First captured by \(firstBy.name)\(formattedDate(detail.firstCapturedAt))")
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
        return " on \(displayDateFormatter.string(from: date))"
    }
}
