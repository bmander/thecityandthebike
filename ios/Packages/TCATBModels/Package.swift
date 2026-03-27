// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBModels",
    platforms: [.iOS(.v17), .macOS(.v13)],
    products: [
        .library(name: "TCATBModels", targets: ["TCATBModels"]),
    ],
    targets: [
        .target(name: "TCATBModels"),
        .testTarget(name: "TCATBModelsTests", dependencies: ["TCATBModels"]),
    ]
)
