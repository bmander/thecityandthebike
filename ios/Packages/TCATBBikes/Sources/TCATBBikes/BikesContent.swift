import SwiftUI
import TCATBModels
import TCATBSharedUI

public struct BikesContent: View {
    @Bindable var viewModel: BikesListViewModel
    let onBikeTapped: (String) -> Void

    public init(viewModel: BikesListViewModel, onBikeTapped: @escaping (String) -> Void) {
        self.viewModel = viewModel
        self.onBikeTapped = onBikeTapped
    }

    public var body: some View {
        Group {
            if viewModel.isLoading && viewModel.bikes.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.error, viewModel.bikes.isEmpty {
                ContentUnavailableView {
                    Label("Error", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(error)
                } actions: {
                    Button("Retry") {
                        Task { await viewModel.loadBikes() }
                    }
                }
            } else if viewModel.bikes.isEmpty {
                ContentUnavailableView("No bikes yet", systemImage: "bicycle")
            } else {
                bikeList
            }
        }
        .searchable(text: $viewModel.searchText, prompt: "Search bikes")
        .onChange(of: viewModel.searchText) {
            viewModel.searchTextDidChange()
        }
        .refreshable {
            await viewModel.loadBikes()
        }
        .task {
            if viewModel.bikes.isEmpty {
                await viewModel.loadBikes()
            }
        }
    }

    private var bikeList: some View {
        List {
            Section {
                ForEach(viewModel.bikes) { bike in
                    BikeRow(bike: bike)
                        .contentShape(Rectangle())
                        .onTapGesture { onBikeTapped(bike.bikeQrId) }
                        .onAppear {
                            if bike.id == viewModel.bikes.last?.id {
                                Task { await viewModel.loadMoreBikes() }
                            }
                        }
                }
            } header: {
                BikeListHeader()
            }

            if viewModel.isLoadingMore {
                HStack {
                    Spacer()
                    ProgressView()
                        .controlSize(.small)
                    Spacer()
                }
                .listRowSeparator(.hidden)
            }
        }
        .listStyle(.plain)
    }
}

private struct BikeListHeader: View {
    var body: some View {
        HStack {
            Text("Bike")
            Spacer()
            Text("Owner")
            Spacer()
            Text("# Captures")
        }
        .font(.caption)
        .foregroundStyle(.secondary)
        .textCase(nil)
    }
}

private struct BikeRow: View {
    let bike: BikeListItem

    var body: some View {
        HStack {
            BikeIdBadge(provider: bike.provider, bikeQrId: bike.bikeQrId)

            if let owner = bike.owner {
                Text(owner.name)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Text("\(bike.submissionCount)")
                .font(.body)
                .monospacedDigit()
        }
        .padding(.vertical, 4)
    }
}

