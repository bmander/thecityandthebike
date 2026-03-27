import Foundation
import Observation
import TCATBModels

@MainActor
@Observable
public final class MainViewModel {
    public private(set) var isLoading = false
    public private(set) var isRefreshing = false
    public private(set) var submissions: [SubmissionResponse] = []
    public private(set) var error: String?
    public private(set) var isLoadingMore = false
    public private(set) var hasMorePages = true
    public private(set) var nextCursor: String?
    public private(set) var pointsAwarded: [ScoringBreakdown]?

    private let apiClient: FeedAPIClient

    public init(apiClient: FeedAPIClient) {
        self.apiClient = apiClient
    }

    public func loadSubmissions() async {
        await fetchSubmissions(isRefresh: false)
    }

    public func refreshSubmissions() async {
        guard !isRefreshing else { return }
        await fetchSubmissions(isRefresh: true)
    }

    private func fetchSubmissions(isRefresh: Bool) async {
        isLoading = !isRefresh
        isRefreshing = isRefresh
        error = nil

        do {
            let result = try await apiClient.getSubmissions(cursor: nil)
            submissions = result.items
            hasMorePages = result.hasMore
            nextCursor = result.nextCursor
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
        isRefreshing = false
    }

    public func loadMoreSubmissions() async {
        guard !isLoadingMore, hasMorePages else { return }

        isLoadingMore = true

        do {
            let result = try await apiClient.getSubmissions(cursor: nextCursor)
            submissions += result.items
            hasMorePages = result.hasMore
            nextCursor = result.nextCursor
        } catch {
            self.error = error.localizedDescription
        }

        isLoadingMore = false
    }

    public func removeSubmission(_ submissionId: String) {
        submissions.removeAll { $0.submissionId == submissionId }
    }

    public func clearError() {
        error = nil
    }

    public func clearPointsAwarded() {
        pointsAwarded = nil
    }

    public func setPointsAwarded(_ breakdown: [ScoringBreakdown]) {
        pointsAwarded = breakdown
    }
}
