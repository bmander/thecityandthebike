// INTEGRATION: Move to TCATBNetworking

import Foundation

public protocol ProfileAPIClient: Sendable {
    func getUserDetail(userId: String) async throws -> UserDetailResponse
    func getUserSubmissions(userId: String, cursor: String?) async throws -> CursorPaginatedSubmissions
    func getLeaderboard(period: String) async throws -> LeaderboardResponse
    func banUser(userId: String) async throws
    func unbanUser(userId: String) async throws
    func deleteAccount() async throws
}
