// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBFeed",
    platforms: [.iOS(.v17), .macOS(.v13)],
    products: [
        .library(name: "TCATBFeed", targets: ["TCATBFeed"]),
    ],
    dependencies: [
        .package(name: "TCATBModels", path: "../TCATBModels"),
    ],
    targets: [
        .target(name: "TCATBFeed", dependencies: ["TCATBModels"]),
    ]
)
