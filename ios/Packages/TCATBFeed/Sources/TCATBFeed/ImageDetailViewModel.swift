import Foundation
import Observation
import TCATBModels

@MainActor
@Observable
public final class ImageDetailViewModel {
    public private(set) var isLoading = false
    public private(set) var submission: SubmissionResponse?
    public private(set) var error: String?
    public private(set) var isOwner = false
    public private(set) var isDeleting = false
    public private(set) var isDeleted = false
    public private(set) var tags: [TagResponse] = []
    public var selectedTagId: String?
    public private(set) var isTagMode = false
    public private(set) var isCreatingTag = false
    public private(set) var isDeletingTag = false
    public private(set) var isProcessingMask = false
    public private(set) var processedRing: [[Float]]?
    public private(set) var processedMaskWidth: Int = 0
    public private(set) var processedMaskHeight: Int = 0
    public private(set) var pointsAwarded: [ScoringBreakdown]?
    public private(set) var isFlagged = false
    public private(set) var isFlagging = false
    public private(set) var flagCount: Int = 0
    public private(set) var isAdmin = false
    public private(set) var isClearingFlags = false

    public let submissionId: String
    private let apiClient: FeedAPIClient
    private let currentUserId: String?

    public init(submissionId: String, apiClient: FeedAPIClient, currentUserId: String?, isAdmin: Bool = false) {
        self.submissionId = submissionId
        self.apiClient = apiClient
        self.currentUserId = currentUserId
        self.isAdmin = isAdmin
    }

    public var isLoggedIn: Bool {
        currentUserId != nil
    }

    public func load() async {
        isLoading = true
        error = nil

        async let submissionTask: Void = loadSubmission()
        async let tagsTask: Void = loadTags()
        async let flagsTask: Void = loadFlagStatus()

        _ = await (submissionTask, tagsTask, flagsTask)

        isLoading = false
    }

    private func loadSubmission() async {
        do {
            let result = try await apiClient.getSubmission(id: submissionId)
            submission = result
            isOwner = result.userId == currentUserId
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func loadTags() async {
        do {
            tags = try await apiClient.getTags(submissionId: submissionId)
        } catch {
            // Tags are supplementary; silently fail
        }
    }

    private func loadFlagStatus() async {
        guard currentUserId != nil else { return }
        do {
            let result = try await apiClient.getFlagStatus(submissionId: submissionId)
            isFlagged = result.flagged
            flagCount = result.flagCount
        } catch {
            // Flag status is supplementary; silently fail
        }
    }

    public func deleteSubmission() async {
        isDeleting = true
        do {
            try await apiClient.deleteSubmission(id: submissionId)
            isDeleted = true
        } catch {
            self.error = "Failed to delete submission"
        }
        isDeleting = false
    }

    public func clearError() {
        error = nil
    }

    // MARK: - Tag Mode

    public func enterTagMode() {
        isTagMode = true
    }

    public func exitTagMode() {
        isTagMode = false
        processedRing = nil
        processedMaskWidth = 0
        processedMaskHeight = 0
    }

    public func processMask(imageData: Data) async {
        isProcessingMask = true
        do {
            let result = try await apiClient.processMask(imageData: imageData)
            processedRing = result.ring
            processedMaskWidth = result.width
            processedMaskHeight = result.height
        } catch {
            self.error = "Failed to process mask"
        }
        isProcessingMask = false
    }

    public func goBackToDrawing() {
        processedRing = nil
        processedMaskWidth = 0
        processedMaskHeight = 0
    }

    public func createTag(imageData: Data) async {
        isCreatingTag = true
        do {
            let tag = try await apiClient.createTag(
                submissionId: submissionId,
                imageData: imageData,
                ring: processedRing,
                ringWidth: processedMaskWidth > 0 ? processedMaskWidth : nil,
                ringHeight: processedMaskHeight > 0 ? processedMaskHeight : nil
            )
            let breakdown = tag.pointsBreakdown
            tags.insert(tag, at: 0)
            isTagMode = false
            processedRing = nil
            processedMaskWidth = 0
            processedMaskHeight = 0
            if let breakdown, !breakdown.isEmpty {
                pointsAwarded = breakdown
            }
        } catch {
            self.error = "Failed to create tag"
        }
        isCreatingTag = false
    }

    public func deleteTag(_ tagId: String) async {
        isDeletingTag = true
        do {
            try await apiClient.deleteTag(id: tagId)
            tags.removeAll { $0.tagId == tagId }
            if selectedTagId == tagId {
                selectedTagId = nil
            }
        } catch {
            self.error = "Failed to delete tag"
        }
        isDeletingTag = false
    }

    public func isTagOwner(_ tag: TagResponse) -> Bool {
        currentUserId == tag.userId
    }

    public func clearPointsAwarded() {
        pointsAwarded = nil
    }

    // MARK: - Flagging

    public func flagSubmission() async {
        isFlagging = true
        do {
            let result = try await apiClient.createFlag(submissionId: submissionId)
            isFlagged = result.flagged
            flagCount = result.flagCount
        } catch {
            self.error = "Failed to flag submission"
        }
        isFlagging = false
    }

    public func clearFlags() async {
        isClearingFlags = true
        do {
            let result = try await apiClient.clearFlags(submissionId: submissionId)
            flagCount = result.flagCount
            isFlagged = result.flagged
        } catch {
            self.error = "Failed to clear flags"
        }
        isClearingFlags = false
    }
}
