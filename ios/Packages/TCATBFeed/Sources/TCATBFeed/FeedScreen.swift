import SwiftUI

public struct FeedScreen: View {
    @Bindable var viewModel: MainViewModel
    var onImageClick: ((String) -> Void)?
    var onShowScoreRules: (() -> Void)?

    public init(
        viewModel: MainViewModel,
        onImageClick: ((String) -> Void)? = nil,
        onShowScoreRules: (() -> Void)? = nil
    ) {
        self.viewModel = viewModel
        self.onImageClick = onImageClick
        self.onShowScoreRules = onShowScoreRules
    }

    public var body: some View {
        ZStack {
            ScrollView {
                ImageGrid(
                    submissions: viewModel.submissions,
                    onImageClick: onImageClick,
                    onLoadMore: {
                        Task { await viewModel.loadMoreSubmissions() }
                    }
                )
            }
            .refreshable {
                await viewModel.refreshSubmissions()
            }

            if viewModel.isLoading {
                LoadingIndicator()
            }

            if viewModel.isLoadingMore {
                VStack {
                    Spacer()
                    ProgressView()
                        .padding()
                }
            }

            if let error = viewModel.error {
                VStack {
                    Spacer()
                    HStack {
                        Text(error)
                            .foregroundStyle(.white)
                            .font(.callout)
                        Spacer()
                        Button("Dismiss") {
                            viewModel.clearError()
                        }
                        .foregroundStyle(.white)
                    }
                    .padding()
                    .background(.black.opacity(0.8))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .padding()
                }
            }

            if let breakdown = viewModel.pointsAwarded {
                PointsAwardedOverlay(
                    breakdown: breakdown,
                    onDismiss: { viewModel.clearPointsAwarded() },
                    onShowScoreRules: onShowScoreRules
                )
            }
        }
        .task {
            if viewModel.submissions.isEmpty {
                await viewModel.loadSubmissions()
            }
        }
    }
}
