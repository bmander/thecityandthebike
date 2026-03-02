import Foundation
import TCATBNetworking
import TCATBFeed
import TCATBModels

extension APIClient: @retroactive FeedAPIClient {
    public func getSubmissions(cursor: String?) async throws -> CursorPaginatedSubmissions {
        try await getSubmissions(limit: 20, cursor: cursor)
    }

    public func getSubmission(id: String) async throws -> SubmissionResponse {
        try await getSubmission(submissionId: id)
    }

    public func deleteSubmission(id: String) async throws {
        _ = try await deleteSubmission(submissionId: id)
    }

    public func deleteTag(id: String) async throws {
        _ = try await deleteTag(tagId: id)
    }

    public func createTag(
        submissionId: String,
        imageData: Data,
        ring: [[Float]]?,
        ringWidth: Int?,
        ringHeight: Int?
    ) async throws -> TagResponse {
        try await createTag(
            submissionId: submissionId,
            imageData: imageData,
            imageFileName: "tag.jpg",
            imageMimeType: "image/jpeg",
            ring: ring.map { String(data: try JSONEncoder().encode($0), encoding: .utf8)! },
            ringWidth: ringWidth.map { String($0) },
            ringHeight: ringHeight.map { String($0) }
        )
    }

    public func processMask(imageData: Data) async throws -> ProcessedMaskResult {
        fatalError("processMask not yet implemented")
    }
}
