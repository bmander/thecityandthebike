import Foundation
import TCATBNetworking
import TCATBProfile
import TCATBModels

extension APIClient: @retroactive ProfileAPIClient {
    // getUserDetail(userId:) — satisfied by APIEndpoints.swift directly

    public func getUserSubmissions(userId: String, cursor: String?) async throws -> CursorPaginatedSubmissions {
        try await getUserSubmissions(userId: userId, limit: 20, cursor: cursor)
    }

    // getLeaderboard(period:) — satisfied by APIEndpoints.swift directly

    public func banUser(userId: String) async throws {
        _ = try await banUser(userId: userId, reason: nil)
    }

    public func unbanUser(userId: String) async throws {
        _ = try await unbanUser(userId: userId) as BanResponse
    }

    public func deleteAccount() async throws {
        _ = try await deleteAccount() as MessageResponse
    }
}
