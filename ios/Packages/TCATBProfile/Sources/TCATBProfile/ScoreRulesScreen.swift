import SwiftUI
import TCATBModels

public struct ScoreRulesScreen: View {
    var onBack: () -> Void

    public init(onBack: @escaping () -> Void) {
        self.onBack = onBack
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text("Photo Points")
                    .font(.title2)
                    .padding(.bottom, 8)

                ForEach(ScoreRules.photoRules, id: \.id) { rule in
                    ScoreRuleRow(label: rule.label, points: rule.points)
                }

                Text("A single photo can trigger several bonuses at once \u{2014} the points add up!")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.top, 8)

                Text("Tag Points")
                    .font(.title2)
                    .padding(.top, 24)
                    .padding(.bottom, 8)

                ForEach(ScoreRules.tagRules, id: \.id) { rule in
                    ScoreRuleRow(label: rule.label, points: rule.points)
                }
            }
            .padding()
        }
        .navigationTitle("Score Rules")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                }
            }
        }
    }
}

private struct ScoreRuleRow: View {
    let label: String
    let points: Int

    var body: some View {
        HStack {
            Text(label)
                .font(.body)

            Spacer()

            Text("+\(points)")
                .font(.body)
                .fontWeight(.bold)
        }
        .padding(.vertical, 4)
    }
}
