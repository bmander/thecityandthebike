// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "TCATBNetworking",
    platforms: [.iOS(.v17), .macOS(.v13)],
    products: [
        .library(name: "TCATBNetworking", targets: ["TCATBNetworking"]),
    ],
    dependencies: [
        .package(name: "TCATBModels", path: "../TCATBModels"),
    ],
    targets: [
        .target(name: "TCATBNetworking", dependencies: ["TCATBModels"]),
        .testTarget(
            name: "TCATBNetworkingTests",
            dependencies: ["TCATBNetworking", "TCATBModels"]
        ),
    ]
)
