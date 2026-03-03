// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBAuth",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "TCATBAuth", targets: ["TCATBAuth"]),
    ],
    dependencies: [
        .package(name: "TCATBModels", path: "../TCATBModels"),
    ],
    targets: [
        .target(name: "TCATBAuth", dependencies: ["TCATBModels"]),
        .testTarget(name: "TCATBAuthTests", dependencies: ["TCATBAuth", "TCATBModels"]),
    ]
)
