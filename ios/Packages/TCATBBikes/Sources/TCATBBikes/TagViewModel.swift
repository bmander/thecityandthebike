import Foundation
import Observation

@Observable
public final class TagViewModel {
    public private(set) var tagDetail: TagDetailResponse?
    public private(set) var submissions: [SubmissionResponse] = []
    public private(set) var isLoading = false
    public private(set) var isLoadingMore = false
    public private(set) var error: String?

    public let tagId: String
    private var hasMorePages = true
    private var nextCursor: String?
    private let apiClient: BikesAPIClient

    public init(tagId: String, apiClient: BikesAPIClient) {
        self.tagId = tagId
        self.apiClient = apiClient
    }

    public func loadTag() async {
        guard !isLoading else { return }
        isLoading = true
        error = nil

        do {
            let detail = try await apiClient.getTagDetail(tagId: tagId)
            tagDetail = detail
        } catch {
            self.error = error.localizedDescription
            isLoading = false
            return
        }

        do {
            let subs = try await apiClient.getTagSubmissions(tagId: tagId, cursor: nil)
            submissions = subs.items
            hasMorePages = subs.hasMore
            nextCursor = subs.nextCursor
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    public func loadMoreSubmissions() async {
        guard !isLoadingMore, !isLoading, hasMorePages else { return }
        isLoadingMore = true

        do {
            let subs = try await apiClient.getTagSubmissions(tagId: tagId, cursor: nextCursor)
            submissions += subs.items
            hasMorePages = subs.hasMore
            nextCursor = subs.nextCursor
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
}
