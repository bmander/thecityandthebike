import Foundation
import UIKit

private let capturedDateFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    f.timeZone = TimeZone(identifier: "America/Los_Angeles")
    return f
}()

/// State of the upload flow.
public enum UploadState: Sendable, Equatable {
    case idle
    case uploading(progress: Double)
    case success(pointsAwarded: Int, pointsBreakdown: [CameraScoringBreakdown])
    case error(message: String)
}

/// Manages photo upload with progress tracking.
///
/// Prepares the image (crop, resize, strip metadata), then uploads via
/// the injected `CameraAPIClient`.
@Observable
public final class UploadManager: @unchecked Sendable {
    public private(set) var state: UploadState = .idle

    private let apiClient: CameraAPIClient
    private var lastImage: UIImage?
    private var lastBikeQrId: String?
    private var lastSide: String?

    public init(apiClient: CameraAPIClient) {
        self.apiClient = apiClient
    }

    /// Uploads the given image as a submission for the specified bike.
    ///
    /// - Parameters:
    ///   - image: The captured photo.
    ///   - bikeQrId: The parsed bike QR identifier.
    ///   - side: Which fender side, if known.
    @MainActor
    public func uploadAndCreateSubmission(image: UIImage, bikeQrId: String, side: String? = nil) {
        lastImage = image
        lastBikeQrId = bikeQrId
        lastSide = side

        state = .uploading(progress: 0)

        Task {
            await performUpload(image: image, bikeQrId: bikeQrId, side: side)
        }
    }

    /// Retries the last failed upload.
    @MainActor
    public func retryUpload() {
        guard let image = lastImage, let bikeQrId = lastBikeQrId else { return }
        uploadAndCreateSubmission(image: image, bikeQrId: bikeQrId, side: lastSide)
    }

    /// Resets state to idle after a successful upload is acknowledged.
    @MainActor
    public func clearSuccess() {
        if case .success = state {
            state = .idle
        }
    }

    /// Resets state to idle and clears stored upload context.
    @MainActor
    public func clearError() {
        state = .idle
        lastImage = nil
        lastBikeQrId = nil
        lastSide = nil
    }

    @MainActor
    private func performUpload(image: UIImage, bikeQrId: String, side: String?) async {
        state = .uploading(progress: 0.1)

        guard let imageData = ImagePreparer.prepareForUpload(image) else {
            state = .error(message: "Could not prepare image for upload")
            return
        }

        state = .uploading(progress: 0.3)

        let capturedDate = capturedDateFormatter.string(from: Date())

        do {
            state = .uploading(progress: 0.5)

            let response = try await apiClient.createSubmission(
                imageData: imageData,
                bikeQrId: bikeQrId,
                capturedDate: capturedDate,
                side: side
            )

            state = .success(
                pointsAwarded: response.pointsAwarded ?? 0,
                pointsBreakdown: response.pointsBreakdown ?? []
            )
        } catch {
            state = .error(message: error.localizedDescription)
        }
    }
}
