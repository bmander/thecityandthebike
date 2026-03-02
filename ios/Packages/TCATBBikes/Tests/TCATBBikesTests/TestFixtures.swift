import Foundation
import TCATBModels

enum TestFixtures {

    static func bikeListItem(
        bikeQrId: String = "bike-1",
        provider: String? = nil,
        submissionCount: Int = 3,
        owner: UserSummary? = nil
    ) -> BikeListItem {
        BikeListItem(
            bikeQrId: bikeQrId,
            provider: provider,
            submissionCount: submissionCount,
            owner: owner
        )
    }

    static func paginatedBikes(
        items: [BikeListItem] = [],
        total: Int? = nil,
        limit: Int = 20,
        offset: Int = 0
    ) -> PaginatedBikes {
        PaginatedBikes(
            items: items,
            total: total ?? items.count,
            limit: limit,
            offset: offset
        )
    }

    static func bikeDetail(
        bikeQrId: String = "bike-1",
        provider: String? = nil,
        submissionCount: Int = 5,
        owners: [BikeOwner] = []
    ) -> BikeDetailResponse {
        BikeDetailResponse(
            bikeQrId: bikeQrId,
            provider: provider,
            submissionCount: submissionCount,
            owners: owners
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
}
