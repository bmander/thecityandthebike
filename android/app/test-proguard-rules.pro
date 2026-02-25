# Don't minify the test APK — we only care about R8 on the app APK
-dontobfuscate
-dontshrink
-dontoptimize

# Suppress warnings for test dependency classes not available on Android
-dontwarn com.sun.jna.**
-dontwarn java.lang.instrument.**
-dontwarn edu.umd.cs.findbugs.**
-dontwarn javax.lang.model.**
-dontwarn org.apiguardian.api.**
-dontwarn org.junit.jupiter.**
-dontwarn org.slf4j.**
