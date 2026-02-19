# Additional ProGuard rules for the debugMinified build type.
# These let R8 shrink the app while keeping enough for instrumented
# tests to run against the minified APK.

# Keep all library/framework classes with all their members.
# R8 strips framework classes (kotlin, compose, dagger, okio, etc.)
# that the test APK depends on at runtime.
-keep class !com.thecityandthebike.** { *; }
-keep interface !com.thecityandthebike.** { *; }
-keep enum !com.thecityandthebike.** { *; }

# Keep Compose stability fields ($stable) that R8 strips from app
# classes. The test APK's compiled Composables reference these fields.
# Also keep public methods that test code calls but app code may not.
-keepclassmembers class com.thecityandthebike.** {
    static final int $stable;
    public <methods>;
}

# Suppress warnings for transitive dependencies not in classpath.
-dontwarn com.google.api.client.http.**
-dontwarn com.google.api.client.http.javanet.**
-dontwarn org.joda.time.**
