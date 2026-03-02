// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TCATBProfile",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "TCATBProfile", targets: ["TCATBProfile"]),
    ],
    targets: [
        .target(name: "TCATBProfile"),
    ]
)
