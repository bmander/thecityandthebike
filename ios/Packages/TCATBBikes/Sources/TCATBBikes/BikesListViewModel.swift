import Foundation
import Observation
import TCATBModels

@Observable
public final class BikesListViewModel {
    public private(set) var bikes: [BikeListItem] = []
    public private(set) var isLoading = false
    public private(set) var isLoadingMore = false
    public private(set) var error: String?
    public var searchText = ""

    /// Call this from the view's `.onChange(of: viewModel.searchText)` modifier
    public func searchTextDidChange() {
        let query = searchText
        searchTask?.cancel()
        searchTask = Task {
            try? await Task.sleep(for: .milliseconds(300))
            guard !Task.isCancelled else { return }
            await search(query: query)
        }
    }

    private var hasMorePages = true
    private var currentOffset = 0
    private let limit = 20
    private let apiClient: BikesAPIClient
    private var searchTask: Task<Void, Never>?

    public init(apiClient: BikesAPIClient) {
        self.apiClient = apiClient
    }

    public func loadBikes() async {
        guard !isLoading else { return }
        isLoading = true
        error = nil
        currentOffset = 0

        do {
            let result = try await apiClient.getBikes(
                offset: 0,
                limit: limit,
                search: searchText.isEmpty ? nil : searchText
            )
            bikes = result.items
            currentOffset = result.items.count
            hasMorePages = result.items.count + result.offset < result.total
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    public func loadMoreBikes() async {
        guard !isLoadingMore, !isLoading, hasMorePages else { return }
        isLoadingMore = true

        do {
            let result = try await apiClient.getBikes(
                offset: currentOffset,
                limit: limit,
                search: searchText.isEmpty ? nil : searchText
            )
            bikes += result.items
            currentOffset = bikes.count
            hasMorePages = bikes.count < result.total
        } catch {
            self.error = error.localizedDescription
        }
        isLoadingMore = false
    }

    public func clearError() {
        error = nil
    }

    private func search(query: String) async {
        isLoading = true
        error = nil
        currentOffset = 0

        do {
            let result = try await apiClient.getBikes(
                offset: 0,
                limit: limit,
                search: query.isEmpty ? nil : query
            )
            bikes = result.items
            currentOffset = result.items.count
            hasMorePages = result.items.count + result.offset < result.total
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}
