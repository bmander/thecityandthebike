import Foundation
import TCATBModels
@testable import TCATBProfile

enum TestError: LocalizedError {
    case test

    var errorDescription: String? { "test error" }
}

final class MockProfileAPIClient: ProfileAPIClient, @unchecked Sendable {

    // MARK: - getUserDetail

    var getUserDetailResult: Result<UserDetailResponse, Error> = .success(
        UserDetailResponse(
            userId: "user-1", username: "testuser",
            submissionCount: 0, ownedBikeCount: 0, leaderboardRanks: []
        )
    )
    var getUserDetailCallCount = 0
    var lastGetUserDetailUserId: String?

    func getUserDetail(userId: String) async throws -> UserDetailResponse {
        getUserDetailCallCount += 1
        lastGetUserDetailUserId = userId
        return try getUserDetailResult.get()
    }

    // MARK: - getUserSubmissions

    var getUserSubmissionsResult: Result<CursorPaginatedSubmissions, Error> = .success(
        CursorPaginatedSubmissions(items: [])
    )
    var getUserSubmissionsCallCount = 0
    var lastGetUserSubmissionsCursor: String?

    func getUserSubmissions(userId: String, cursor: String?) async throws -> CursorPaginatedSubmissions {
        getUserSubmissionsCallCount += 1
        lastGetUserSubmissionsCursor = cursor
        return try getUserSubmissionsResult.get()
    }

    // MARK: - getLeaderboard

    var getLeaderboardResult: Result<LeaderboardResponse, Error> = .success(
        LeaderboardResponse(period: "weekly", entries: [])
    )
    var getLeaderboardCallCount = 0
    var lastGetLeaderboardPeriod: String?

    func getLeaderboard(period: String) async throws -> LeaderboardResponse {
        getLeaderboardCallCount += 1
        lastGetLeaderboardPeriod = period
        return try getLeaderboardResult.get()
    }

    // MARK: - banUser

    var banUserResult: Result<Void, Error> = .success(())
    var banUserCallCount = 0

    func banUser(userId: String) async throws {
        banUserCallCount += 1
        try banUserResult.get()
    }

    // MARK: - unbanUser

    var unbanUserResult: Result<Void, Error> = .success(())
    var unbanUserCallCount = 0

    func unbanUser(userId: String) async throws {
        unbanUserCallCount += 1
        try unbanUserResult.get()
    }

    // MARK: - deleteAccount

    var deleteAccountResult: Result<Void, Error> = .success(())
    var deleteAccountCallCount = 0

    func deleteAccount() async throws {
        deleteAccountCallCount += 1
        try deleteAccountResult.get()
    }
}
