// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBAuth",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "TCATBAuth", targets: ["TCATBAuth"]),
    ],
    targets: [
        .target(name: "TCATBAuth"),
        .testTarget(name: "TCATBAuthTests", dependencies: ["TCATBAuth"]),
    ]
)
