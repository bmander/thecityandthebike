import SwiftUI
import TCATBModels
#if canImport(UIKit)
import UIKit
#endif

public struct ImageDetailScreen: View {
    @Bindable var viewModel: ImageDetailViewModel
    var onHome: () -> Void
    var onBikeClick: ((String) -> Void)?
    var onUserClick: ((String) -> Void)?
    var onTagClick: ((String) -> Void)?
    var onShowScoreRules: (() -> Void)?

    @State private var showDeleteDialog = false
    @State private var showDeleteTagDialog: String?
    @State private var showFlagDialog = false

    public init(
        viewModel: ImageDetailViewModel,
        onHome: @escaping () -> Void,
        onBikeClick: ((String) -> Void)? = nil,
        onUserClick: ((String) -> Void)? = nil,
        onTagClick: ((String) -> Void)? = nil,
        onShowScoreRules: (() -> Void)? = nil
    ) {
        self.viewModel = viewModel
        self.onHome = onHome
        self.onBikeClick = onBikeClick
        self.onUserClick = onUserClick
        self.onTagClick = onTagClick
        self.onShowScoreRules = onShowScoreRules
    }

    public var body: some View {
        ZStack {
            ScrollView {
                VStack(spacing: 0) {
                    if let submission = viewModel.submission {
                        imageSection(submission: submission)
                        actionBar(submission: submission)
                        adminSection
                        Spacer().frame(height: 16)
                        SubmissionInfo(
                            submission: submission,
                            formattedDate: formatDate(submission.capturedDate),
                            onUserClick: onUserClick,
                            onBikeClick: onBikeClick
                        )
                    } else if viewModel.isLoading {
                        LoadingIndicator()
                            .frame(maxWidth: .infinity, minHeight: 300)
                    }
                }
            }

            if let breakdown = viewModel.pointsAwarded {
                PointsAwardedOverlay(
                    breakdown: breakdown,
                    onDismiss: { viewModel.clearPointsAwarded() },
                    onShowScoreRules: onShowScoreRules
                )
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onHome) {
                    Image(systemName: "bicycle")
                        .font(.title2)
                }
            }
        }
        .alert("Delete Photo", isPresented: $showDeleteDialog) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                Task { await viewModel.deleteSubmission() }
            }
        } message: {
            Text("Are you sure you want to delete this photo? This action cannot be undone.")
        }
        .alert("Delete Tag", isPresented: .init(
            get: { showDeleteTagDialog != nil },
            set: { if !$0 { showDeleteTagDialog = nil } }
        )) {
            Button("Cancel", role: .cancel) { showDeleteTagDialog = nil }
            Button("Delete", role: .destructive) {
                if let tagId = showDeleteTagDialog {
                    Task { await viewModel.deleteTag(tagId) }
                    showDeleteTagDialog = nil
                }
            }
        } message: {
            Text("Are you sure you want to delete this tag?")
        }
        .alert("Flag Photo", isPresented: $showFlagDialog) {
            Button("Cancel", role: .cancel) {}
            Button("Flag", role: .destructive) {
                Task { await viewModel.flagSubmission() }
            }
        } message: {
            Text("Are you sure you want to flag this photo as inappropriate?")
        }
        .task {
            await viewModel.load()
        }
        .onChange(of: viewModel.isDeleted) { _, deleted in
            if deleted { onHome() }
        }
    }

    @ViewBuilder
    private func imageSection(submission: SubmissionResponse) -> some View {
        if viewModel.isTagMode, let ring = viewModel.processedRing {
            // Confirm phase: show image with polygon outline overlay
            MaskConfirmView(
                imageURL: submission.imageUrl.flatMap { URL(string: $0) },
                thumbnailURL: submission.imageUrlThumbnail.flatMap { URL(string: $0) },
                ring: ring,
                maskWidth: viewModel.processedMaskWidth,
                maskHeight: viewModel.processedMaskHeight
            )
            TagModeControls(
                mode: .confirm,
                isProcessing: viewModel.isCreatingTag,
                onBack: { viewModel.goBackToDrawing() },
                onConfirm: {
                    Task {
                        let imageURL = submission.imageUrl.flatMap { URL(string: $0) }
                        let imageData = await exportCompositedFromRing(
                            imageURL: imageURL,
                            ring: ring,
                            maskWidth: viewModel.processedMaskWidth,
                            maskHeight: viewModel.processedMaskHeight
                        )
                        if let imageData {
                            await viewModel.createTag(imageData: imageData)
                        }
                    }
                }
            )
        } else if viewModel.isTagMode {
            // Drawing phase: mask painter + Discard/Next buttons
            MaskPainter(
                imageURL: submission.imageUrl.flatMap { URL(string: $0) },
                thumbnailURL: submission.imageUrlThumbnail.flatMap { URL(string: $0) },
                onExitTagMode: { viewModel.exitTagMode() },
                isProcessingMask: viewModel.isProcessingMask,
                onMaskExported: { maskData, _, _ in
                    Task { await viewModel.processMask(imageData: maskData) }
                }
            )
        } else {
            // Normal view: zoomable image with tag overlays
            ZStack(alignment: .topLeading) {
                ZoomableImage(
                    imageURL: submission.imageUrl.flatMap { URL(string: $0) },
                    thumbnailURL: submission.imageUrlThumbnail.flatMap { URL(string: $0) },
                    contentDescription: "Submission photo"
                ) {
                    TagOutlineOverlay(
                        tags: viewModel.tags,
                        selectedTagId: viewModel.selectedTagId,
                        onTagTapped: { viewModel.selectedTagId = $0 },
                        isTagDeletable: { viewModel.isTagOwner($0) },
                        onDeleteTag: { showDeleteTagDialog = $0 },
                        onTagClick: onTagClick
                    )
                }

                if viewModel.isLoggedIn {
                    Button {
                        showFlagDialog = true
                    } label: {
                        Image(systemName: viewModel.isFlagged ? "flag.fill" : "flag")
                            .padding(8)
                            .background(
                                viewModel.isFlagged
                                    ? Color.red.opacity(0.5)
                                    : Color(.systemBackground).opacity(0.5)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .disabled(viewModel.isFlagging)
                    .padding(8)
                }
            }
        }
    }

    @ViewBuilder
    private func actionBar(submission: SubmissionResponse) -> some View {
        let showTagButton = viewModel.isLoggedIn && !viewModel.isTagMode
        let showOwnerActions = viewModel.isOwner || viewModel.isAdmin

        if showTagButton || showOwnerActions {
            HStack {
                if showTagButton {
                    Button {
                        viewModel.enterTagMode()
                    } label: {
                        Label("Add tags", systemImage: "tag")
                    }
                    .buttonStyle(.bordered)
                }
                Spacer()
                if showOwnerActions {
                    OwnerActions(
                        isDeleting: viewModel.isDeleting,
                        onDownload: {
                            // Download action handled by app shell
                        },
                        onShowDeleteDialog: { showDeleteDialog = true }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
    }

    @ViewBuilder
    private var adminSection: some View {
        if viewModel.isAdmin && viewModel.flagCount > 0 {
            AdminFlagActions(
                flagCount: viewModel.flagCount,
                isClearingFlags: viewModel.isClearingFlags,
                onClearFlags: {
                    Task { await viewModel.clearFlags() }
                }
            )
        }
    }

    private func formatDate(_ dateString: String?) -> String? {
        guard let dateString else { return nil }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        guard let date = formatter.date(from: dateString) else { return dateString }
        let displayFormatter = DateFormatter()
        displayFormatter.dateFormat = "MMM d, yyyy"
        return displayFormatter.string(from: date)
    }
}

/// Exports a composited image: original pixels where the ring polygon covers,
/// transparent elsewhere. Matches the Android `exportCompositedFromRing` function.
private func exportCompositedFromRing(
    imageURL: URL?,
    ring: [[Float]],
    maskWidth: Int,
    maskHeight: Int
) async -> Data? {
    #if canImport(UIKit)
    guard ring.count >= 3, let imageURL else { return nil }

    // Download the original image
    guard let (data, _) = try? await URLSession.shared.data(from: imageURL),
          let originalImage = UIImage(data: data),
          let cgOriginal = originalImage.cgImage
    else { return nil }

    let maxCompositeDim: CGFloat = 1024
    let origWidth = CGFloat(cgOriginal.width)
    let origHeight = CGFloat(cgOriginal.height)
    let compositeScale = max(origWidth, origHeight) > maxCompositeDim
        ? maxCompositeDim / max(origWidth, origHeight)
        : 1.0
    let outputWidth = Int(origWidth * compositeScale)
    let outputHeight = Int(origHeight * compositeScale)

    guard outputWidth > 0, outputHeight > 0 else { return nil }

    let scaleX = CGFloat(outputWidth) / CGFloat(maskWidth)
    let scaleY = CGFloat(outputHeight) / CGFloat(maskHeight)

    let colorSpace = CGColorSpaceCreateDeviceRGB()
    guard let ctx = CGContext(
        data: nil,
        width: outputWidth,
        height: outputHeight,
        bitsPerComponent: 8,
        bytesPerRow: outputWidth * 4,
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ) else { return nil }

    // Core Graphics has origin at bottom-left, flip Y
    ctx.translateBy(x: 0, y: CGFloat(outputHeight))
    ctx.scaleBy(x: 1, y: -1)

    // Draw filled polygon mask
    ctx.beginPath()
    ctx.move(to: CGPoint(x: CGFloat(ring[0][0]) * scaleX, y: CGFloat(ring[0][1]) * scaleY))
    for i in 1..<ring.count {
        ctx.addLine(to: CGPoint(x: CGFloat(ring[i][0]) * scaleX, y: CGFloat(ring[i][1]) * scaleY))
    }
    ctx.closePath()
    ctx.setFillColor(CGColor(red: 1, green: 1, blue: 1, alpha: 1))
    ctx.fillPath()

    // Composite with SRC_IN: keep original pixels only where mask is opaque
    ctx.setBlendMode(.sourceIn)
    if let scaledImage = cgOriginal.copy() {
        ctx.draw(scaledImage, in: CGRect(x: 0, y: 0, width: outputWidth, height: outputHeight))
    }

    guard let composited = ctx.makeImage() else { return nil }

    // Crop to bounding box of the ring polygon
    var minX = CGFloat.greatestFiniteMagnitude
    var minY = CGFloat.greatestFiniteMagnitude
    var maxX = -CGFloat.greatestFiniteMagnitude
    var maxY = -CGFloat.greatestFiniteMagnitude
    for point in ring {
        let px = CGFloat(point[0]) * scaleX
        let py = CGFloat(point[1]) * scaleY
        minX = min(minX, px)
        minY = min(minY, py)
        maxX = max(maxX, px)
        maxY = max(maxY, py)
    }
    let cropLeft = max(0, Int(minX))
    let cropTop = max(0, Int(minY))
    let cropRight = min(outputWidth, Int(maxX + 1))
    let cropBottom = min(outputHeight, Int(maxY + 1))
    let cropWidth = cropRight - cropLeft
    let cropHeight = cropBottom - cropTop

    let finalImage: CGImage
    if cropWidth > 0, cropHeight > 0,
       let cropped = composited.cropping(to: CGRect(x: cropLeft, y: cropTop, width: cropWidth, height: cropHeight)) {
        finalImage = cropped
    } else {
        finalImage = composited
    }

    return UIImage(cgImage: finalImage).pngData()
    #else
    return nil
    #endif
}
