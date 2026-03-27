import Foundation

public enum ScoreRules {
    public struct Rule: Sendable, Equatable {
        public let id: String
        public let label: String
        public let points: Int

        public init(id: String, label: String, points: Int) {
            self.id = id
            self.label = label
            self.points = points
        }
    }

    public static let photoRules: [Rule] = [
        Rule(id: "add_image", label: "Took a photo", points: 2),
        Rule(id: "first_bike_today", label: "First photo of this bike today", points: 5),
        Rule(id: "first_bike_for_user", label: "Your first photo of this bike", points: 5),
        Rule(id: "first_bike_ever", label: "First photo of this bike ever", points: 10),
    ]

    public static let tagRules: [Rule] = [
        Rule(id: "tag_1", label: "1st tag on a submission", points: 2),
        Rule(id: "tag_2", label: "2nd tag", points: 1),
        Rule(id: "tag_3", label: "3rd tag", points: 1),
        Rule(id: "tag_4_plus", label: "4th+ tags", points: 0),
    ]
}
