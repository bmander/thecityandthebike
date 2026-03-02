// INTEGRATION: Move to TCATBNetworking
// This protocol defines the API surface needed by the feed module.
// When integrating, the real APIClient in TCATBNetworking should conform to this protocol.

import Foundation
import TCATBModels

public protocol FeedAPIClient: Sendable {
    func getSubmissions(cursor: String?) async throws -> CursorPaginatedSubmissions
    func getSubmission(id: String) async throws -> SubmissionResponse
    func deleteSubmission(id: String) async throws

    func getTags(submissionId: String) async throws -> [TagResponse]
    func createTag(submissionId: String, imageData: Data, ring: [[Float]]?, ringWidth: Int?, ringHeight: Int?) async throws -> TagResponse
    func deleteTag(id: String) async throws
    func processMask(imageData: Data) async throws -> ProcessedMaskResult

    func getFlagStatus(submissionId: String) async throws -> FlagStatusResponse
    func createFlag(submissionId: String) async throws -> FlagStatusResponse
    func clearFlags(submissionId: String) async throws -> FlagStatusResponse
}
