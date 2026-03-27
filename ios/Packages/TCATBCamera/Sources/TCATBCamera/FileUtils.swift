import Foundation
import UIKit

/// Utilities for temporary file management and image I/O.
public enum FileUtils {
    /// Returns a URL for a new temporary JPEG file in the app's caches directory.
    public static func temporaryImageURL() -> URL {
        let directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("images", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let filename = "photo_\(Int(Date().timeIntervalSince1970 * 1000)).jpg"
        return directory.appendingPathComponent(filename)
    }

    /// Saves a `UIImage` as JPEG data to the given file URL.
    ///
    /// - Parameters:
    ///   - image: The image to save.
    ///   - url: The destination file URL.
    ///   - compressionQuality: JPEG compression quality (0.0-1.0). Defaults to 0.9.
    /// - Throws: If the data cannot be written.
    public static func saveImage(_ image: UIImage, to url: URL, compressionQuality: CGFloat = 0.9) throws {
        guard let data = image.jpegData(compressionQuality: compressionQuality) else {
            throw FileUtilsError.jpegConversionFailed
        }
        try data.write(to: url)
    }

    /// Loads a `UIImage` from a file URL.
    ///
    /// - Parameter url: The source file URL.
    /// - Returns: The loaded image, or nil if the file cannot be read.
    public static func loadImage(from url: URL) -> UIImage? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data)
    }

    /// Deletes the file at the given URL, ignoring errors.
    public static func deleteFile(at url: URL) {
        try? FileManager.default.removeItem(at: url)
    }

    /// Cleans up old temporary images from the caches/images directory.
    /// Removes files older than the given time interval.
    public static func cleanupOldTemporaryImages(olderThan interval: TimeInterval = 3600) {
        let directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("images", isDirectory: true)
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: directory,
            includingPropertiesForKeys: [.creationDateKey]
        ) else { return }

        let cutoff = Date().addingTimeInterval(-interval)
        for file in files {
            guard let attributes = try? FileManager.default.attributesOfItem(atPath: file.path),
                  let creationDate = attributes[.creationDate] as? Date,
                  creationDate < cutoff else { continue }
            try? FileManager.default.removeItem(at: file)
        }
    }
}

public enum FileUtilsError: Error, LocalizedError {
    case jpegConversionFailed

    public var errorDescription: String? {
        switch self {
        case .jpegConversionFailed:
            return "Failed to convert image to JPEG data"
        }
    }
}
