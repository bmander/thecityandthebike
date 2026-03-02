// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBBikes",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "TCATBBikes", targets: ["TCATBBikes"]),
    ],
    dependencies: [
        .package(name: "TCATBModels", path: "../TCATBModels"),
    ],
    targets: [
        .target(name: "TCATBBikes", dependencies: ["TCATBModels"]),
    ]
)
