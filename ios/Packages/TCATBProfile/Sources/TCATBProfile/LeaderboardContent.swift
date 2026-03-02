import SwiftUI

public struct LeaderboardContent: View {
    @Bindable var viewModel: LeaderboardViewModel
    var onUserClick: (String) -> Void

    public init(viewModel: LeaderboardViewModel, onUserClick: @escaping (String) -> Void) {
        self.viewModel = viewModel
        self.onUserClick = onUserClick
    }

    public var body: some View {
        VStack(spacing: 0) {
            Picker("Period", selection: $viewModel.selectedPeriod) {
                ForEach(LeaderboardPeriod.allCases, id: \.self) { period in
                    Text(period.displayName).tag(period)
                }
            }
            .pickerStyle(.segmented)
            .padding()

            Group {
                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = viewModel.error {
                    VStack(spacing: 16) {
                        Text(error)
                            .foregroundStyle(.red)
                        Button("Dismiss") {
                            viewModel.clearError()
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.entries.isEmpty {
                    Text("No submissions yet")
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(viewModel.entries) { entry in
                        LeaderboardRow(entry: entry)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                onUserClick(entry.userId)
                            }
                            .listRowInsets(EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16))
                    }
                    .listStyle(.plain)
                }
            }
        }
        .task {
            await viewModel.loadLeaderboard()
        }
        .onChange(of: viewModel.selectedPeriod) {
            viewModel.periodDidChange()
        }
    }
}

private struct LeaderboardRow: View {
    let entry: LeaderboardEntry

    private var isTopThree: Bool { entry.rank <= 3 }

    var body: some View {
        HStack {
            HStack(spacing: 12) {
                Text("#\(entry.rank)")
                    .font(isTopThree ? .title3 : .body)
                    .fontWeight(isTopThree ? .bold : .regular)
                    .foregroundColor(isTopThree ? .accentColor : .primary)

                Text(entry.username)
                    .font(isTopThree ? .title3 : .body)
                    .fontWeight(isTopThree ? .bold : .regular)
                    .foregroundColor(isTopThree ? .accentColor : .primary)
            }

            Spacer()

            Text("\(entry.score)")
                .font(isTopThree ? .title3 : .body)
                .fontWeight(isTopThree ? .bold : .regular)
                .foregroundColor(isTopThree ? .accentColor : .primary)
        }
    }
}
