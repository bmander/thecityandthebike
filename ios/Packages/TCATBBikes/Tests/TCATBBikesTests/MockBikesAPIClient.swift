import Foundation
import TCATBModels
@testable import TCATBBikes

enum TestError: LocalizedError {
    case test

    var errorDescription: String? { "test error" }
}

final class MockBikesAPIClient: BikesAPIClient, @unchecked Sendable {

    // MARK: - getBikes

    var getBikesResult: Result<PaginatedBikes, Error> = .success(
        PaginatedBikes(items: [], total: 0, limit: 20, offset: 0)
    )
    var getBikesCallCount = 0
    var lastGetBikesOffset: Int?
    var lastGetBikesLimit: Int?
    var lastGetBikesSearch: String?

    func getBikes(offset: Int, limit: Int, search: String?) async throws -> PaginatedBikes {
        getBikesCallCount += 1
        lastGetBikesOffset = offset
        lastGetBikesLimit = limit
        lastGetBikesSearch = search
        return try getBikesResult.get()
    }

    // MARK: - getBikeDetail

    var getBikeDetailResult: Result<BikeDetailResponse, Error> = .success(
        BikeDetailResponse(bikeQrId: "bike-1", submissionCount: 0)
    )
    var getBikeDetailCallCount = 0
    var lastGetBikeDetailQrId: String?

    func getBikeDetail(bikeQrId: String) async throws -> BikeDetailResponse {
        getBikeDetailCallCount += 1
        lastGetBikeDetailQrId = bikeQrId
        return try getBikeDetailResult.get()
    }

    // MARK: - getBikeSubmissions

    var getBikeSubmissionsResult: Result<CursorPaginatedSubmissions, Error> = .success(
        CursorPaginatedSubmissions(items: [])
    )
    var getBikeSubmissionsCallCount = 0
    var lastGetBikeSubmissionsQrId: String?
    var lastGetBikeSubmissionsCursor: String?

    func getBikeSubmissions(bikeQrId: String, cursor: String?) async throws -> CursorPaginatedSubmissions {
        getBikeSubmissionsCallCount += 1
        lastGetBikeSubmissionsQrId = bikeQrId
        lastGetBikeSubmissionsCursor = cursor
        return try getBikeSubmissionsResult.get()
    }

    // MARK: - getTagDetail

    var getTagDetailResult: Result<TagDetailResponse, Error> = .success(
        TagDetailResponse(tagId: "tag-1", imageUrl: "", createdAt: "", submissionCount: 0)
    )
    var getTagDetailCallCount = 0

    func getTagDetail(tagId: String) async throws -> TagDetailResponse {
        getTagDetailCallCount += 1
        return try getTagDetailResult.get()
    }

    // MARK: - getTagSubmissions

    var getTagSubmissionsResult: Result<CursorPaginatedSubmissions, Error> = .success(
        CursorPaginatedSubmissions(items: [])
    )
    var getTagSubmissionsCallCount = 0

    func getTagSubmissions(tagId: String, cursor: String?) async throws -> CursorPaginatedSubmissions {
        getTagSubmissionsCallCount += 1
        return try getTagSubmissionsResult.get()
    }
}
