// INTEGRATION: Move to TCATBNetworking
// This protocol defines the API surface needed by the bikes/tags feature.
// During integration, replace with the real API client from TCATBNetworking.

import Foundation

public protocol BikesAPIClient: Sendable {
    /// Fetch paginated list of bikes with optional search query.
    func getBikes(offset: Int, limit: Int, search: String?) async throws -> PaginatedBikes

    /// Fetch detail for a single bike.
    func getBikeDetail(bikeQrId: String) async throws -> BikeDetailResponse

    /// Fetch cursor-paginated submissions for a bike.
    func getBikeSubmissions(bikeQrId: String, cursor: String?) async throws -> CursorPaginatedSubmissions

    /// Fetch detail for a single tag.
    func getTagDetail(tagId: String) async throws -> TagDetailResponse

    /// Fetch cursor-paginated submissions for a tag.
    func getTagSubmissions(tagId: String, cursor: String?) async throws -> CursorPaginatedSubmissions
}
