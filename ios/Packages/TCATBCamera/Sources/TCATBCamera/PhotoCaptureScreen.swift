import AVFoundation
import SwiftUI

/// Camera screen for capturing bike fender photos.
///
/// Displays a square camera preview with a fender overlay guide,
/// flash toggle, left/right side toggle, and a capture button.
public struct PhotoCaptureScreen: View {
    @Binding var side: FenderSide
    let onPhotoCaptured: (UIImage) -> Void
    let onBack: () -> Void

    @State private var flashMode: FlashMode = .auto
    @State private var isCapturing = false
    @State private var cameraSession = CameraSessionManager()

    public init(
        side: Binding<FenderSide>,
        onPhotoCaptured: @escaping (UIImage) -> Void,
        onBack: @escaping () -> Void
    ) {
        self._side = side
        self.onPhotoCaptured = onPhotoCaptured
        self.onBack = onBack
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            switch cameraSession.permissionStatus {
            case .notDetermined:
                Color.clear.task {
                    await cameraSession.requestPermission()
                }
            case .denied, .restricted:
                permissionDeniedView
            case .authorized:
                cameraContentView
            @unknown default:
                permissionDeniedView
            }
        }
    }

    private var permissionDeniedView: some View {
        ZStack {
            Text("Camera permission is required to take photos")
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

    private var cameraContentView: some View {
        ZStack {
            VStack(spacing: 0) {
                // Flash toggle
                HStack {
                    Spacer()
                    Button(action: { flashMode = flashMode.next }) {
                        Image(systemName: flashMode.systemImageName)
                            .foregroundStyle(.white)
                            .font(.title2)
                            .padding(8)
                    }
                    .accessibilityLabel(flashMode.label)
                    .padding(.trailing, 8)
                }

                // Square camera preview with fender overlay
                CameraView(session: cameraSession.session)
                    .aspectRatio(1, contentMode: .fit)
                    .clipped()
                    .fenderOverlay(side: side)
                    .accessibilityIdentifier("camera_preview")

                Spacer()
            }

            // Side toggle buttons
            VStack {
                Spacer()

                HStack(spacing: 24) {
                    sideButton(forSide: .left)
                    sideButton(forSide: .right)
                }
                .padding(.bottom, 140)

                // Capture button
                Button(action: capturePhoto) {
                    Circle()
                        .fill(.white)
                        .frame(width: 72, height: 72)
                }
                .disabled(isCapturing)
                .accessibilityLabel("Capture photo")
                .padding(.bottom, 48)
            }

            // Back button
            VStack {
                HStack {
                    backButton
                    Spacer()
                }
                Spacer()
            }
        }
        .task {
            await cameraSession.startSession()
        }
        .onDisappear {
            cameraSession.stopSession()
        }
        .onChange(of: flashMode) { _, newValue in
            cameraSession.setFlashMode(newValue)
        }
    }

    private func sideButton(forSide buttonSide: FenderSide) -> some View {
        Button {
            side = buttonSide
        } label: {
            RoundedRectangle(cornerRadius: 8)
                .fill(side == buttonSide ? .white : .gray)
                .frame(width: 48, height: 48)
                .overlay {
                    Image(systemName: "bicycle")
                        .scaleEffect(x: buttonSide == .left ? -1 : 1, y: 1)
                        .foregroundStyle(.black)
                        .opacity(side == buttonSide ? 1.0 : 0.4)
                }
        }
        .accessibilityLabel("\(buttonSide.rawValue.capitalized) side")
        .accessibilityIdentifier("side_\(buttonSide.rawValue)")
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

    private func capturePhoto() {
        isCapturing = true
        cameraSession.capturePhoto { image in
            if let image {
                let squared = ImagePreparer.cropToSquare(image)
                onPhotoCaptured(squared)
            }
            isCapturing = false
        }
    }
}

/// Manages the AVCaptureSession lifecycle for photo capture.
@Observable
final class CameraSessionManager: @unchecked Sendable {
    let session = AVCaptureSession()
    private(set) var permissionStatus: AVAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
    private let photoOutput = AVCapturePhotoOutput()
    private var flashMode: FlashMode = .auto
    private var delegate: PhotoCaptureDelegate?

    func requestPermission() async {
        let granted = await AVCaptureDevice.requestAccess(for: .video)
        await MainActor.run {
            permissionStatus = granted ? .authorized : .denied
        }
    }

    func startSession() async {
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
                if session.canAddOutput(photoOutput) {
                    session.addOutput(photoOutput)
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

    func setFlashMode(_ mode: FlashMode) {
        flashMode = mode
    }

    func capturePhoto(completion: @escaping @MainActor (UIImage?) -> Void) {
        let settings = AVCapturePhotoSettings()

        switch flashMode {
        case .auto:
            settings.flashMode = .auto
        case .on:
            settings.flashMode = .on
        case .off:
            settings.flashMode = .off
        }

        let captureDelegate = PhotoCaptureDelegate { image in
            Task { @MainActor in
                completion(image)
            }
        }
        self.delegate = captureDelegate
        photoOutput.capturePhoto(with: settings, delegate: captureDelegate)
    }
}

/// Delegate that receives the captured photo and converts it to UIImage.
private final class PhotoCaptureDelegate: NSObject, AVCapturePhotoCaptureDelegate, @unchecked Sendable {
    private let completion: (UIImage?) -> Void

    init(completion: @escaping (UIImage?) -> Void) {
        self.completion = completion
        super.init()
    }

    func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photo: AVCapturePhoto,
        error: Error?
    ) {
        if let error {
            print("PhotoCaptureDelegate: capture failed - \(error.localizedDescription)")
            completion(nil)
            return
        }
        guard let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data) else {
            completion(nil)
            return
        }
        completion(image)
    }
}
