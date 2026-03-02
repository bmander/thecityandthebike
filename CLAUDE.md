# Claude Code Guidelines

## Virtual Environment

If a virtual environment doesn't exist, create one:

```bash
cd api && python -m venv venv && source venv/bin/activate && pip install -r requirements.txt -r requirements-dev.txt
```

## Testing

All new behavior must be covered by a test case. Do not consider a task complete until tests are written and passing.

### Python Tests

Always run tests with the `-x` flag to stop on the first error, and `-q` for minimal output to keep context clean:

```bash
cd api && source venv/bin/activate && pytest -x -q 2>&1 | tail -10
```

This prevents overwhelming output when there are multiple failures from the same root cause.

### Android Builds

The Android SDK is installed locally. Before running Android builds or tests, always recreate `android/local.properties` (it's gitignored and may be missing or stale, especially in worktrees):

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > android/local.properties
```

Environment variables must be set in the **same Bash invocation** as the build command, since shell state does not persist between tool calls. Build and install on a connected device:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && export ANDROID_HOME=~/Library/Android/sdk && export PATH="$ANDROID_HOME/platform-tools:$PATH" && cd android && ./gradlew assembleStagingDebug && adb install app/build/outputs/apk/staging/debug/app-staging-debug.apk
```

### Android Tests

Run instrumented tests (requires connected device/emulator):

```bash
cd android && ./gradlew connectedAndroidTest
```

Run a specific test class:

```bash
cd android && ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.thecityandthebike.ui.components.CameraFABTest
```

#### Minified Instrumented Tests

To catch R8/ProGuard stripping issues before production, run instrumented tests against a minified build:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && export ANDROID_HOME=~/Library/Android/sdk && cd android && ./gradlew -PtestBuildType=debugMinified connectedStagingDebugMinifiedAndroidTest
```

This uses the `debugMinified` build type, which applies R8 minification with debug signing so instrumented tests can detect classes or methods stripped by R8.

#### Java 21 Compatibility

Android Studio bundles Java 21, which requires:
- Android Gradle Plugin: 8.3.0 or later
- Gradle: 8.4 or later
- KSP instead of kapt for annotation processing (Hilt)

If builds fail with `IllegalAccessError: superclass access check failed` related to kapt, migrate from kapt to KSP.

#### Android 16 Compatibility

Testing on Android 16 devices requires recent library versions due to `InputManager.getInstance()` API changes:
- Compose BOM: 2025.05.01 or later
- Espresso: 3.7.0 or later
- AndroidX Test Runner: 1.6.2 or later

If tests fail with `NoSuchMethodException: android.hardware.input.InputManager.getInstance`, update these dependencies.

### iOS Swift Package Tests

The system `swift` from Command Line Tools has a dyld linker issue. Always use the Xcode toolchain via `DEVELOPER_DIR`:

Packages without iOS-only SwiftUI (e.g., TCATBSharedUI, TCATBAuth, TCATBModels) can use `swift test`:

```bash
cd ios/Packages/TCATBSharedUI && DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun swift test -q 2>&1 | tail -10
```

Packages with iOS-only SwiftUI APIs (e.g., TCATBProfile) must use `xcodebuild` with an iOS simulator:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild test -scheme TCATBProfile -destination 'platform=iOS Simulator,id=1180B51D-CC0B-42C8-AA42-BFBBA864EB38' 2>&1 | grep -E '(Test Suite|Test Case|passed|failed|error:|✔|✘)' | tail -30
```

To find available simulator IDs:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun simctl list devices available | grep -i "iphone"
```

### iOS Linting & Formatting

SwiftLint runs automatically on every Xcode build via a build phase. SwiftFormat is CLI-only (no build phase) to avoid modifying files during builds.

SwiftLint requires the Xcode toolchain (same `DEVELOPER_DIR` as Swift package tests):

```bash
# Lint all iOS code
cd ios && DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer swiftlint lint 2>&1 | tail -20

# Auto-fix SwiftLint violations
cd ios && DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer swiftlint lint --fix

# Check formatting (dry run)
cd ios && swiftformat --lint . 2>&1 | tail -20

# Apply formatting
cd ios && swiftformat .
```

#### Platform Requirements

Packages depending on TCATBModels need `.macOS(.v13)` in their Package.swift platforms for `swift test` to work on macOS. Packages using `@Observable` need `.macOS(.v14)`.

## Incidental Findings

If during exploration or implementation you notice a particularly urgent or elegant refactor opportunity, or a security flaw, pause and ask whether to file a GitHub issue for it before continuing with the main task.

## Background Commands

Do not pipe background shell commands through `tail`, `head`, or other utilities. The background shell environment has a limited PATH and these commands may not be found, causing the entire command to fail with exit code 127.
