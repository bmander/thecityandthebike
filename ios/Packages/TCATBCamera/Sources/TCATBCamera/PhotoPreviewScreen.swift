import SwiftUI

/// Photo review screen shown after capture, before upload.
///
/// Displays the captured photo in a square preview with confirm and retake buttons.
public struct PhotoPreviewScreen: View {
    let image: UIImage
    let onConfirm: () -> Void
    let onRetake: () -> Void

    public init(
        image: UIImage,
        onConfirm: @escaping () -> Void,
        onRetake: @escaping () -> Void
    ) {
        self.image = image
        self.onConfirm = onConfirm
        self.onRetake = onRetake
    }

    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity)
                .aspectRatio(1, contentMode: .fit)
                .clipped()
                .accessibilityLabel("Photo preview")

            VStack {
                Spacer()

                HStack {
                    Spacer()

                    // Retake button
                    Button(action: onRetake) {
                        Circle()
                            .fill(Color(white: 0.2))
                            .frame(width: 72, height: 72)
                            .overlay {
                                Image(systemName: "xmark")
                                    .foregroundStyle(.white)
                                    .font(.title2)
                            }
                    }
                    .accessibilityLabel("Retake photo")

                    Spacer()

                    // Confirm button
                    Button(action: onConfirm) {
                        Circle()
                            .fill(.white)
                            .frame(width: 72, height: 72)
                            .overlay {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(.black)
                                    .font(.title2)
                            }
                    }
                    .accessibilityLabel("Confirm photo")

                    Spacer()
                }
                .padding(.bottom, 48)
            }
        }
    }
}
