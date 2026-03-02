import Foundation
import Observation
import TCATBModels

@Observable
public final class UserViewModel {
    public private(set) var userDetail: UserDetailResponse?
    public private(set) var submissions: [SubmissionResponse] = []
    public private(set) var isLoading = false
    public private(set) var isLoadingMore = false
    public private(set) var error: String?
    public private(set) var hasMorePages = true
    public private(set) var isBanning = false
    public let currentUserIsAdmin: Bool

    private var nextCursor: String?
    private let userId: String
    private let apiClient: ProfileAPIClient

    public init(userId: String, apiClient: ProfileAPIClient, currentUserIsAdmin: Bool = false) {
        self.userId = userId
        self.apiClient = apiClient
        self.currentUserIsAdmin = currentUserIsAdmin
    }

    public func loadUser() async {
        isLoading = true
        error = nil

        do {
            let detail = try await apiClient.getUserDetail(userId: userId)
            userDetail = detail
        } catch {
            isLoading = false
            self.error = error.localizedDescription
            return
        }

        do {
            let result = try await apiClient.getUserSubmissions(userId: userId, cursor: nil)
            submissions = result.items
            hasMorePages = result.hasMore
            nextCursor = result.nextCursor
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    public func loadMoreSubmissions() async {
        guard !isLoadingMore, hasMorePages else { return }
        isLoadingMore = true

        do {
            let result = try await apiClient.getUserSubmissions(userId: userId, cursor: nextCursor)
            submissions += result.items
            hasMorePages = result.hasMore
            nextCursor = result.nextCursor
        } catch {
            self.error = error.localizedDescription
        }

        isLoadingMore = false
    }

    public func removeSubmission(submissionId: String) {
        submissions.removeAll { $0.submissionId == submissionId }
    }

    public func banUser() async {
        isBanning = true
        do {
            try await apiClient.banUser(userId: userId)
            if var detail = userDetail {
                userDetail = UserDetailResponse(
                    userId: detail.userId,
                    username: detail.username,
                    isAdmin: detail.isAdmin,
                    isBanned: true,
                    submissionCount: detail.submissionCount,
                    firstSeenAt: detail.firstSeenAt,
                    lastSeenAt: detail.lastSeenAt,
                    ownedBikeCount: detail.ownedBikeCount,
                    leaderboardRanks: detail.leaderboardRanks
                )
            }
        } catch {
            self.error = error.localizedDescription
        }
        isBanning = false
    }

    public func unbanUser() async {
        isBanning = true
        do {
            try await apiClient.unbanUser(userId: userId)
            if var detail = userDetail {
                userDetail = UserDetailResponse(
                    userId: detail.userId,
                    username: detail.username,
                    isAdmin: detail.isAdmin,
                    isBanned: false,
                    submissionCount: detail.submissionCount,
                    firstSeenAt: detail.firstSeenAt,
                    lastSeenAt: detail.lastSeenAt,
                    ownedBikeCount: detail.ownedBikeCount,
                    leaderboardRanks: detail.leaderboardRanks
                )
            }
        } catch {
            self.error = error.localizedDescription
        }
        isBanning = false
    }

    public func clearError() {
        error = nil
    }
}
