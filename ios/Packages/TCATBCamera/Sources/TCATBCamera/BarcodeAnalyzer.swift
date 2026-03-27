import AVFoundation
import Vision

/// Processes camera sample buffers to detect QR codes using the Vision framework.
///
/// Uses `VNDetectBarcodesRequest` for reliable QR code detection from
/// `AVCaptureVideoDataOutput` frames.
public final class BarcodeAnalyzer: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate, @unchecked Sendable {
    private let onBarcodeDetected: @Sendable (String) -> Void
    private var isProcessing = false
    private let lock = NSLock()

    /// Creates a new barcode analyzer.
    ///
    /// - Parameter onBarcodeDetected: Called on detection with the QR code's string payload.
    ///   Called on an arbitrary queue; dispatch to main if needed.
    public init(onBarcodeDetected: @escaping @Sendable (String) -> Void) {
        self.onBarcodeDetected = onBarcodeDetected
        super.init()
    }

    public func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        lock.lock()
        guard !isProcessing else {
            lock.unlock()
            return
        }
        isProcessing = true
        lock.unlock()

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            resetProcessing()
            return
        }

        let request = VNDetectBarcodesRequest { [weak self] request, error in
            defer { self?.resetProcessing() }

            if let error {
                print("BarcodeAnalyzer: detection failed - \(error.localizedDescription)")
                return
            }

            guard let results = request.results as? [VNBarcodeObservation] else { return }

            for observation in results {
                if observation.symbology == .qr, let payload = observation.payloadStringValue {
                    self?.onBarcodeDetected(payload)
                    return
                }
            }
        }

        request.symbologies = [.qr]

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:])
        do {
            try handler.perform([request])
        } catch {
            print("BarcodeAnalyzer: request failed - \(error.localizedDescription)")
            resetProcessing()
        }
    }

    private func resetProcessing() {
        lock.lock()
        isProcessing = false
        lock.unlock()
    }
}
