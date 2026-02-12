# Claude Code Guidelines

## Virtual Environment

If a virtual environment doesn't exist, create one:

```bash
cd api && python -m venv venv && source venv/bin/activate && pip install -r requirements.txt -r requirements-dev.txt
```

## Testing

All new behavior must be covered by a test case. Do not consider a task complete until tests are written and passing.

### Python Tests

Always run tests with the `-x` flag to stop on the first error:

```bash
cd api && source venv/bin/activate && pytest -x
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

## Incidental Findings

If during exploration or implementation you notice a particularly urgent or elegant refactor opportunity, or a security flaw, pause and ask whether to file a GitHub issue for it before continuing with the main task.

## Background Commands

Do not pipe background shell commands through `tail`, `head`, or other utilities. The background shell environment has a limited PATH and these commands may not be found, causing the entire command to fail with exit code 127.
