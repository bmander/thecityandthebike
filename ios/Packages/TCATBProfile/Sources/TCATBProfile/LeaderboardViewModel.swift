import Foundation
import Observation

@Observable
public final class LeaderboardViewModel {
    public private(set) var isLoading = false
    public var selectedPeriod: LeaderboardPeriod = .weekly

    /// Call this from the view's `.onChange(of: viewModel.selectedPeriod)` modifier
    public func periodDidChange() {
        Task { await loadLeaderboard() }
    }
    public private(set) var entries: [LeaderboardEntry] = []
    public private(set) var error: String?

    private let apiClient: ProfileAPIClient

    public init(apiClient: ProfileAPIClient) {
        self.apiClient = apiClient
    }

    public func loadLeaderboard() async {
        isLoading = true
        error = nil

        do {
            let response = try await apiClient.getLeaderboard(period: selectedPeriod.rawValue)
            entries = response.entries
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    public func clearError() {
        error = nil
    }
}
