import Foundation
import TCATBNetworking
import TCATBBikes
import TCATBModels

extension APIClient: @retroactive BikesAPIClient {
    public func getBikes(offset: Int, limit: Int, search: String?) async throws -> PaginatedBikes {
        try await getBikes(limit: limit, offset: offset)
    }

    // getBikeDetail(bikeQrId:) — satisfied by APIEndpoints.swift directly

    public func getBikeSubmissions(bikeQrId: String, cursor: String?) async throws -> CursorPaginatedSubmissions {
        try await getBikeSubmissions(bikeQrId: bikeQrId, limit: 20, cursor: cursor)
    }

    // getTagDetail(tagId:) — satisfied by APIEndpoints.swift directly

    public func getTagSubmissions(tagId: String, cursor: String?) async throws -> CursorPaginatedSubmissions {
        try await getTagSubmissions(tagId: tagId, limit: 20, cursor: cursor)
    }
}
