// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBCamera",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "TCATBCamera", targets: ["TCATBCamera"]),
    ],
    targets: [
        .target(name: "TCATBCamera"),
    ]
)
