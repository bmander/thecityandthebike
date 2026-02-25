# TCATB Android App

The Android app is built with Jetpack Compose (compileSdk 35, targetSdk 35, minSdk 26).

## Prerequisites
- Android SDK (typically installed via Android Studio)
- Java 17+ (Android Studio's bundled JDK works)
- A connected Android device with USB debugging enabled

## Building and Running

1. Set up environment variables:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   export ANDROID_HOME=~/Library/Android/sdk
   export PATH="$ANDROID_HOME/platform-tools:$PATH"
   ```

2. Build the debug APK:
   ```bash
   cd android
   ./gradlew assembleStagingDebug
   ```

3. Install on a connected device:
   ```bash
   adb install app/build/outputs/apk/staging/debug/app-staging-debug.apk
   ```

4. Launch the app:
   ```bash
   adb shell am start -n com.thecityandthebike/.MainActivity
   ```
