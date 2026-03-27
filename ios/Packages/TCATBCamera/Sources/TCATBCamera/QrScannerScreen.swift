import AVFoundation
import SwiftUI

/// Camera screen that scans QR codes from bike fenders using the Vision framework.
///
/// Displays a full-screen camera preview with a semi-transparent overlay
/// and a cutout viewfinder. When a QR code from a recognized bike-share
/// provider is detected, calls `onQrCodeScanned` with the raw URL string.
public struct QrScannerScreen: View {
    let onQrCodeScanned: (String) -> Void
    let onBack: () -> Void

    @State private var scannerSession = QrScannerSessionManager()
    @State private var errorMessage: String?
    @State private var errorDismissTask: Task<Void, Never>?

    public init(
        onQrCodeScanned: @escaping (String) -> Void,
        onBack: @escaping () -> Void
    ) {
        self.onQrCodeScanned = onQrCodeScanned
        self.onBack = onBack
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            switch scannerSession.permissionStatus {
            case .notDetermined:
                Color.clear.task {
                    await scannerSession.requestPermission()
                }
            case .denied, .restricted:
                permissionDeniedView
            case .authorized:
                scannerContentView
            @unknown default:
                permissionDeniedView
            }
        }
    }

    private var permissionDeniedView: some View {
        ZStack {
            Text("Camera permission is required to scan QR codes")
                .foregroundStyle(.primary)
                .padding(32)

            VStack {
                HStack {
                    backButton
                    Spacer()
                }
                Spacer()
            }
        }
    }

    private var scannerContentView: some View {
        ZStack {
            CameraView(session: scannerSession.session)
                .ignoresSafeArea()

            // Semi-transparent overlay with cutout viewfinder
            ViewfinderOverlay()
                .fill(Color.black.opacity(0.5), style: FillStyle(eoFill: true))
                .ignoresSafeArea()
                .allowsHitTesting(false)

            VStack {
                HStack {
                    backButton
                    Spacer()
                }
                Spacer()

                Text("Point camera at QR code on bike")
                    .foregroundStyle(.white)
                    .font(.title3)
                    .fontWeight(.medium)
                    .padding(.bottom, 120)
            }

            // Error snackbar
            if let errorMessage {
                VStack {
                    Spacer()
                    Text(errorMessage)
                        .foregroundStyle(.white)
                        .padding()
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 8))
                        .padding()
                }
            }
        }
        .task {
            await scannerSession.startSession { qrValue in
                if isRecognizedProvider(qrValue) {
                    onQrCodeScanned(qrValue)
                } else {
                    showError("Unrecognized bike provider")
                }
            }
        }
        .onDisappear {
            scannerSession.stopSession()
            errorDismissTask?.cancel()
        }
    }

    private var backButton: some View {
        Button(action: onBack) {
            Image(systemName: "chevron.left")
                .foregroundStyle(.white)
                .font(.title2)
                .padding(16)
        }
        .accessibilityLabel("Back")
    }

    private func showError(_ message: String) {
        errorMessage = message
        scannerSession.resetScanned()

        errorDismissTask?.cancel()
        errorDismissTask = Task {
            try? await Task.sleep(for: .seconds(3))
            guard !Task.isCancelled else { return }
            errorMessage = nil
        }
    }
}

/// Shape that draws a semi-transparent overlay with a rounded-rect cutout in the center.
private struct ViewfinderOverlay: Shape {
    func path(in rect: CGRect) -> Path {
        let cutoutSize = min(rect.width, rect.height) * 0.6
        let cutoutRect = CGRect(
            x: (rect.width - cutoutSize) / 2,
            y: (rect.height - cutoutSize) / 2,
            width: cutoutSize,
            height: cutoutSize
        )

        var path = Path(rect)
        path.addRoundedRect(in: cutoutRect, cornerSize: CGSize(width: 16, height: 16))
        return path
    }
}

/// Manages the AVCaptureSession for QR code scanning.
@Observable
final class QrScannerSessionManager: @unchecked Sendable {
    let session = AVCaptureSession()
    private(set) var permissionStatus: AVAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
    private var analyzer: BarcodeAnalyzer?
    private var hasScanned = false
    private let lock = NSLock()

    func requestPermission() async {
        let granted = await AVCaptureDevice.requestAccess(for: .video)
        await MainActor.run {
            permissionStatus = granted ? .authorized : .denied
        }
    }

    func startSession(onQrDetected: @MainActor @escaping (String) -> Void) async {
        guard permissionStatus == .authorized, !session.isRunning else { return }

        await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async { [self] in
                session.beginConfiguration()

                guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                      let input = try? AVCaptureDeviceInput(device: camera) else {
                    session.commitConfiguration()
                    continuation.resume()
                    return
                }

                if session.canAddInput(input) {
                    session.addInput(input)
                }

                let videoOutput = AVCaptureVideoDataOutput()

                let analysisQueue = DispatchQueue(label: "com.thecityandthebike.qrscanner", qos: .userInitiated)

                let barcodeAnalyzer = BarcodeAnalyzer { [weak self] qrValue in
                    guard let self else { return }
                    lock.lock()
                    guard !hasScanned else {
                        lock.unlock()
                        return
                    }
                    hasScanned = true
                    lock.unlock()

                    DispatchQueue.main.async {
                        onQrDetected(qrValue)
                    }
                }
                self.analyzer = barcodeAnalyzer

                videoOutput.setSampleBufferDelegate(barcodeAnalyzer, queue: analysisQueue)

                if session.canAddOutput(videoOutput) {
                    session.addOutput(videoOutput)
                }

                session.commitConfiguration()
                session.startRunning()
                continuation.resume()
            }
        }
    }

    func stopSession() {
        DispatchQueue.global(qos: .userInitiated).async { [self] in
            session.stopRunning()
        }
    }

    func resetScanned() {
        lock.lock()
        hasScanned = false
        lock.unlock()
    }
}
