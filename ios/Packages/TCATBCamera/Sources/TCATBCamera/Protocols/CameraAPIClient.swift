import Foundation

// INTEGRATION: Move to TCATBNetworking

/// Protocol defining the API calls needed by the camera and upload flow.
///
/// Conforming types handle authentication and base URL configuration.
/// During integration, this protocol should be implemented by the main
/// API client in TCATBNetworking.
public protocol CameraAPIClient: Sendable {
    /// Upload an image file and create a submission.
    ///
    /// - Parameters:
    ///   - imageData: JPEG image data to upload.
    ///   - bikeQrId: The parsed bike QR identifier.
    ///   - capturedDate: ISO date string (yyyy-MM-dd) when the photo was taken.
    ///   - side: Which fender side was photographed ("left" or "right"), if known.
    /// - Returns: The submission response from the server.
    /// - Throws: Network or server errors.
    func createSubmission(
        imageData: Data,
        bikeQrId: String,
        capturedDate: String,
        side: String?
    ) async throws -> CameraSubmissionResponse
}
