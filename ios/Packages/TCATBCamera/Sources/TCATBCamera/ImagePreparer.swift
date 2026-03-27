import CoreGraphics
import ImageIO
import UIKit
import UniformTypeIdentifiers

/// Prepares captured images for upload by stripping EXIF metadata,
/// cropping to square, and compressing as JPEG.
public enum ImagePreparer {
    /// Maximum dimension (width or height) for uploaded images.
    private static let maxDimension: CGFloat = 1600

    /// JPEG compression quality for uploads.
    private static let compressionQuality: CGFloat = 0.85

    /// Prepares an image for upload: crops to square, resizes if needed,
    /// strips EXIF metadata, and returns JPEG data.
    ///
    /// - Parameter image: The source image.
    /// - Returns: JPEG data ready for upload, or nil on failure.
    public static func prepareForUpload(_ image: UIImage) -> Data? {
        let squared = cropToSquare(image)
        let resized = resizeIfNeeded(squared)
        return stripMetadataAndCompress(resized)
    }

    /// Prepares an image file at the given URL for upload.
    ///
    /// - Parameter url: File URL of the source image.
    /// - Returns: JPEG data ready for upload, or nil on failure.
    public static func prepareForUpload(fileAt url: URL) -> Data? {
        guard let image = UIImage(contentsOfFile: url.path) else { return nil }
        return prepareForUpload(image)
    }

    /// Crops the image to a centered square.
    public static func cropToSquare(_ image: UIImage) -> UIImage {
        guard let cgImage = image.cgImage else { return image }
        let side = min(cgImage.width, cgImage.height)
        let x = (cgImage.width - side) / 2
        let y = (cgImage.height - side) / 2
        let cropRect = CGRect(x: x, y: y, width: side, height: side)
        guard let cropped = cgImage.cropping(to: cropRect) else { return image }
        return UIImage(cgImage: cropped, scale: image.scale, orientation: image.imageOrientation)
    }

    /// Resizes the image if either dimension exceeds `maxDimension`.
    static func resizeIfNeeded(_ image: UIImage) -> UIImage {
        let size = image.size
        guard size.width > maxDimension || size.height > maxDimension else { return image }

        let scale = maxDimension / max(size.width, size.height)
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)

        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }

    /// Compresses the image as JPEG and strips all EXIF/GPS metadata.
    static func stripMetadataAndCompress(_ image: UIImage) -> Data? {
        guard let jpegData = image.jpegData(compressionQuality: compressionQuality) else {
            return nil
        }

        guard let source = CGImageSourceCreateWithData(jpegData as CFData, nil),
              let utType = CGImageSourceGetType(source) else {
            return jpegData
        }

        let mutableData = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(mutableData, utType, 1, nil) else {
            return jpegData
        }

        // Copy image without metadata
        let cleanProperties: [CFString: Any] = [
            kCGImagePropertyExifDictionary: NSNull(),
            kCGImagePropertyGPSDictionary: NSNull(),
            kCGImagePropertyTIFFDictionary: NSNull(),
            kCGImagePropertyIPTCDictionary: NSNull(),
        ]

        CGImageDestinationAddImageFromSource(destination, source, 0, cleanProperties as CFDictionary)

        guard CGImageDestinationFinalize(destination) else {
            return jpegData
        }

        return mutableData as Data
    }
}
