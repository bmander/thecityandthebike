// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBSharedUI",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "TCATBSharedUI", targets: ["TCATBSharedUI"]),
    ],
    dependencies: [
        .package(name: "TCATBModels", path: "../TCATBModels"),
    ],
    targets: [
        .target(name: "TCATBSharedUI", dependencies: ["TCATBModels"]),
    ]
)
