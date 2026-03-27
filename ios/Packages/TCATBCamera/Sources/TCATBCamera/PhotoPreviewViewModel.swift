import SwiftUI

/// View model for the photo preview and upload flow.
///
/// Coordinates between the photo preview screen and the upload manager,
/// tracking the current photo, bike QR ID, and fender side.
@Observable
public final class PhotoPreviewViewModel {
    public var capturedImage: UIImage?
    public var bikeQrId: String?
    public var side: FenderSide = .right

    public let uploadManager: UploadManager

    public init(uploadManager: UploadManager) {
        self.uploadManager = uploadManager
    }

    /// The current upload state, forwarded from the upload manager.
    public var uploadState: UploadState {
        uploadManager.state
    }

    /// Starts uploading the captured photo.
    @MainActor
    public func upload() {
        guard let image = capturedImage, let qrId = bikeQrId else { return }
        uploadManager.uploadAndCreateSubmission(
            image: image,
            bikeQrId: qrId,
            side: side.rawValue
        )
    }

    /// Retries the last upload.
    @MainActor
    public func retryUpload() {
        uploadManager.retryUpload()
    }

    /// Clears a successful upload state.
    @MainActor
    public func clearSuccess() {
        uploadManager.clearSuccess()
    }

    /// Resets the entire flow for a new capture.
    @MainActor
    public func reset() {
        capturedImage = nil
        bikeQrId = nil
        side = .right
        uploadManager.clearError()
    }
}
