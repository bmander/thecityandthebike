import SwiftUI
import TCATBModels

public struct PointsAwardedOverlay: View {
    let breakdown: [ScoringBreakdown]
    let onDismiss: () -> Void
    var onShowScoreRules: (() -> Void)?

    @State private var scale: CGFloat = 0.5
    @State private var opacity: Double = 0
    @State private var dismissTask: Task<Void, Never>?

    public init(
        breakdown: [ScoringBreakdown],
        onDismiss: @escaping () -> Void,
        onShowScoreRules: (() -> Void)? = nil
    ) {
        self.breakdown = breakdown
        self.onDismiss = onDismiss
        self.onShowScoreRules = onShowScoreRules
    }

    private var total: Int {
        breakdown.reduce(0) { $0 + $1.points }
    }

    public var body: some View {
        ZStack {
            VStack(spacing: 0) {
                ForEach(Array(breakdown.enumerated()), id: \.element.eventType) { _, item in
                    HStack {
                        Text(item.label)
                            .font(.body)
                            .foregroundStyle(.white)
                        Spacer()
                        Text("+\(item.points)")
                            .font(.body)
                            .fontWeight(.bold)
                            .foregroundStyle(.white)
                    }
                }

                Text("+\(total)")
                    .font(.system(size: 64, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.top, 8)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(Color.black.opacity(0.75))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(alignment: .topTrailing) {
                HStack(spacing: 8) {
                    if onShowScoreRules != nil {
                        dismissButton(label: "?") {
                            onDismiss()
                            onShowScoreRules?()
                        }
                        .offset(x: -4, y: -16)
                    }
                    dismissButton(label: "\u{00D7}") {
                        animateDismiss()
                    }
                    .offset(x: 16, y: -16)
                }
            }
            .padding(.horizontal, 32)
            .scaleEffect(scale)
            .opacity(opacity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onAppear {
            withAnimation(.easeOut(duration: 0.2)) {
                opacity = 1
            }
            withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                scale = 1.0
            }
            dismissTask = Task {
                try? await Task.sleep(for: .seconds(10))
                guard !Task.isCancelled else { return }
                animateDismiss()
            }
        }
        .onDisappear {
            dismissTask?.cancel()
        }
    }

    private func dismissButton(label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.body)
                .fontWeight(.bold)
                .foregroundStyle(.black)
                .frame(width: 32, height: 32)
                .background(Color.white.opacity(0.85))
                .clipShape(Circle())
                .overlay(Circle().stroke(Color.gray, lineWidth: 1))
        }
    }

    private func animateDismiss() {
        dismissTask?.cancel()
        withAnimation(.easeIn(duration: 0.3)) {
            opacity = 0
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            onDismiss()
        }
    }
}
