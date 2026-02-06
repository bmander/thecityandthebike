# Claude Code Guidelines

## Virtual Environment

If a virtual environment doesn't exist, create one:

```bash
cd api && python -m venv venv && source venv/bin/activate && pip install -r requirements.txt -r requirements-dev.txt
```

## Testing

### Python Tests

Always run tests with the `-x` flag to stop on the first error:

```bash
cd api && source venv/bin/activate && pytest -x
```

This prevents overwhelming output when there are multiple failures from the same root cause.

### Android Builds

Before running Android builds or tests, set the required environment variables:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME=~/Library/Android/sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

Build and install on a connected device:

```bash
cd android && ./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk
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
