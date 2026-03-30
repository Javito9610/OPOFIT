# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============ RETROFIT ============
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ============ GSON ============
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-dontwarn sun.misc.**

# ============ DATA MODELS (Gson serialization/deserialization — keep fields only) ============
-keep class com.opofit.miapp.data.models.** { <fields>; }
-keep class com.opofit.miapp.data.responsemodels.** { <fields>; }

# ============ COROUTINES ============
-dontwarn kotlinx.coroutines.**

# ============ GOOGLE PLAY SERVICES ============
-dontwarn com.google.android.gms.**

# ============ FIREBASE ============
-dontwarn com.google.firebase.**
