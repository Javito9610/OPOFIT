-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

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

-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-dontwarn sun.misc.**

-keep class com.opofit.miapp.data.models.** { <fields>; }
-keep class com.opofit.miapp.data.responsemodels.** { <fields>; }

-dontwarn kotlinx.coroutines.**

-dontwarn com.google.android.gms.**

-dontwarn com.google.firebase.**
