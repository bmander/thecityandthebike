import SwiftUI

public enum TagModePhase {
    case draw
    case confirm
}

public struct TagModeControls: View {
    let mode: TagModePhase
    var isProcessing: Bool = false
    var hasStrokes: Bool = true
    var onDiscard: (() -> Void)?
    var onNext: (() -> Void)?
    var onBack: (() -> Void)?
    var onConfirm: (() -> Void)?

    public init(
        mode: TagModePhase,
        isProcessing: Bool = false,
        hasStrokes: Bool = true,
        onDiscard: (() -> Void)? = nil,
        onNext: (() -> Void)? = nil,
        onBack: (() -> Void)? = nil,
        onConfirm: (() -> Void)? = nil
    ) {
        self.mode = mode
        self.isProcessing = isProcessing
        self.hasStrokes = hasStrokes
        self.onDiscard = onDiscard
        self.onNext = onNext
        self.onBack = onBack
        self.onConfirm = onConfirm
    }

    public var body: some View {
        HStack {
            Spacer()
            switch mode {
            case .draw:
                Button("Discard") { onDiscard?() }
                    .buttonStyle(.bordered)
                Spacer()
                Button("Next") { onNext?() }
                    .buttonStyle(.borderedProminent)
                    .disabled(!hasStrokes || isProcessing)
                    .overlay {
                        if isProcessing {
                            ProgressView()
                        }
                    }
            case .confirm:
                Button("Back") { onBack?() }
                    .buttonStyle(.bordered)
                Spacer()
                Button("Confirm tag") { onConfirm?() }
                    .buttonStyle(.borderedProminent)
                    .disabled(isProcessing)
                    .overlay {
                        if isProcessing {
                            ProgressView()
                        }
                    }
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}
