// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBProfile",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "TCATBProfile", targets: ["TCATBProfile"]),
    ],
    dependencies: [
        .package(name: "TCATBModels", path: "../TCATBModels"),
        .package(name: "TCATBSharedUI", path: "../TCATBSharedUI"),
    ],
    targets: [
        .target(name: "TCATBProfile", dependencies: ["TCATBModels", "TCATBSharedUI"]),
    ]
)
