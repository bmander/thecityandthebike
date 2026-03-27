import AVFoundation
import SwiftUI
import UIKit

/// A `UIViewRepresentable` that displays an AVFoundation camera preview.
///
/// This wraps an `AVCaptureVideoPreviewLayer` in a UIView for use in SwiftUI.
/// The parent is responsible for managing the `AVCaptureSession` lifecycle.
public struct CameraView: UIViewRepresentable {
    let session: AVCaptureSession

    public init(session: AVCaptureSession) {
        self.session = session
    }

    public func makeUIView(context: Context) -> CameraPreviewUIView {
        let view = CameraPreviewUIView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    public func updateUIView(_ uiView: CameraPreviewUIView, context: Context) {
        uiView.previewLayer.session = session
    }
}

/// UIView subclass that hosts an `AVCaptureVideoPreviewLayer` as its layer.
public final class CameraPreviewUIView: UIView {
    override public class var layerClass: AnyClass {
        AVCaptureVideoPreviewLayer.self
    }

    var previewLayer: AVCaptureVideoPreviewLayer {
        // swiftlint:disable:next force_cast
        layer as! AVCaptureVideoPreviewLayer
    }
}
