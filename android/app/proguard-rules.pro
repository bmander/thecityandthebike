# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# API service interface - preserve generic type signatures for Retrofit
-keep interface com.thecityandthebike.data.api.ApiService { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.thecityandthebike.**$$serializer { *; }
-keepclassmembers class com.thecityandthebike.** {
    *** Companion;
}
-keepclasseswithmembers class com.thecityandthebike.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.thecityandthebike.data.model.dto.** { *; }
