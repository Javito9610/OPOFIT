# Reglas ProGuard / R8 para OpoFit en release.
#
# El comentario en build.gradle.kts decía "el APK release sin reglas completas
# crasheaba al abrir". El motivo casi siempre es Retrofit + Gson + Firebase
# borrando las data classes que se serializan/deserializan por reflection.
# Estas reglas cubren todas esas librerías.
#
# Cuando estés listo para release de Play Store:
#  1. En build.gradle.kts: `isMinifyEnabled = true`, `isShrinkResources = true`.
#  2. `./gradlew :app:assembleRelease`
#  3. Probar el APK en un dispositivo real (login + plan + GPS + share + push).
#  4. Si crashea, mirar logcat el ClassNotFoundException o NoSuchFieldException
#     y añadir la regla correspondiente.

# === Stacktraces legibles en Play Console ===
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# === Atributos para reflection y librerías ===
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# === Retrofit / OkHttp ===
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# === Gson — data classes serializadas/deserializadas ===
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class sun.misc.Unsafe { *; }
-dontwarn sun.misc.**

# === Modelos OpoFit que viajan por la red ===
-keep class com.opofit.miapp.data.models.** { <fields>; }
-keep class com.opofit.miapp.data.responsemodels.** { *; }
-keep class com.opofit.miapp.data.requestmodels.** { *; }
-keep class com.opofit.miapp.gps.model.** { *; }

# === Kotlin / Coroutines ===
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.flow.** { *; }
-dontwarn kotlinx.coroutines.**

# === Compose ===
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**

# === Firebase / Google Play Services ===
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# === Health Connect (alpha — alta carga de reflection) ===
-keep class androidx.health.connect.** { *; }
-keep class androidx.health.** { *; }
-dontwarn androidx.health.**

# === Maps Compose ===
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.maps.android.**

# === Misc ===
-dontwarn javax.lang.model.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
