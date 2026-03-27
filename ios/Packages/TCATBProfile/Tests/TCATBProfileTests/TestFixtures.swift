import Foundation
import TCATBModels

enum TestFixtures {

    static func userDetail(
        userId: String = "user-1",
        username: String = "testuser",
        isAdmin: Bool = false,
        isBanned: Bool = false,
        submissionCount: Int = 5,
        ownedBikeCount: Int = 0,
        leaderboardRanks: [LeaderboardRank] = []
    ) -> UserDetailResponse {
        UserDetailResponse(
            userId: userId,
            username: username,
            isAdmin: isAdmin,
            isBanned: isBanned,
            submissionCount: submissionCount,
            ownedBikeCount: ownedBikeCount,
            leaderboardRanks: leaderboardRanks
        )
    }

    static func submission(
        submissionId: String = "sub-1",
        userId: String = "user-1",
        bikeQrId: String = "bike-1",
        capturedDate: String? = "2026-03-01T12:00:00Z",
        uploadedAt: String? = nil
    ) -> SubmissionResponse {
        SubmissionResponse(
            submissionId: submissionId,
            userId: userId,
            bikeQrId: bikeQrId,
            capturedDate: capturedDate,
            uploadedAt: uploadedAt
        )
    }

    static func paginatedSubmissions(
        items: [SubmissionResponse] = [],
        nextCursor: String? = nil,
        hasMore: Bool = false
    ) -> CursorPaginatedSubmissions {
        CursorPaginatedSubmissions(items: items, nextCursor: nextCursor, hasMore: hasMore)
    }

    static func leaderboardEntry(
        rank: Int = 1,
        userId: String = "user-1",
        username: String = "testuser",
        score: Int = 100
    ) -> LeaderboardEntry {
        LeaderboardEntry(rank: rank, userId: userId, username: username, score: score)
    }
}
