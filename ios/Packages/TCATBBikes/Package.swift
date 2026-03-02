// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBBikes",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "TCATBBikes", targets: ["TCATBBikes"]),
    ],
    dependencies: [
        .package(name: "TCATBModels", path: "../TCATBModels"),
        .package(name: "TCATBSharedUI", path: "../TCATBSharedUI"),
    ],
    targets: [
        .target(name: "TCATBBikes", dependencies: ["TCATBModels", "TCATBSharedUI"]),
        .testTarget(name: "TCATBBikesTests", dependencies: ["TCATBBikes", "TCATBModels"]),
    ]
)
