import Foundation
import Observation

@Observable
public final class BikeViewModel {
    public private(set) var bikeDetail: BikeDetailResponse?
    public private(set) var submissions: [SubmissionResponse] = []
    public private(set) var isLoading = false
    public private(set) var isLoadingMore = false
    public private(set) var error: String?

    public let bikeQrId: String
    private var hasMorePages = true
    private var nextCursor: String?
    private let apiClient: BikesAPIClient

    public init(bikeQrId: String, apiClient: BikesAPIClient) {
        self.bikeQrId = bikeQrId
        self.apiClient = apiClient
    }

    public func loadBike() async {
        guard !isLoading else { return }
        isLoading = true
        error = nil

        do {
            let detail = try await apiClient.getBikeDetail(bikeQrId: bikeQrId)
            bikeDetail = detail
        } catch {
            self.error = error.localizedDescription
            isLoading = false
            return
        }

        do {
            let subs = try await apiClient.getBikeSubmissions(bikeQrId: bikeQrId, cursor: nil)
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
            let subs = try await apiClient.getBikeSubmissions(bikeQrId: bikeQrId, cursor: nextCursor)
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
