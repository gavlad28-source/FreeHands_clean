# ======================== FREEHANDS PROGUARD RULES ========================

# Keep app entry points
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# ======================== KOTLIN ========================
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ======================== COROUTINES ========================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ======================== ANDROIDX & ROOM ========================
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class *
-keepclassmembers class * {
  @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ======================== HILT/DAGGER ========================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }

# ======================== COMPOSE ========================
-keep class androidx.compose.** { *; }
-keep @androidx.compose.runtime.Composable public class * { *; }
-keepclassmembers class androidx.compose.** { *; }

# ======================== VOSK ASR ========================
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn org.vosk.**
-dontwarn com.sun.jna.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ======================== PORCUPINE WAKE WORD ========================
-keep class ai.picovoice.porcupine.** { *; }
-dontwarn ai.picovoice.porcupine.**

# ======================== WEBRTC VAD ========================
-keep class com.konovalov.vad.** { *; }
-dontwarn com.konovalov.vad.**

# ======================== ANDROID SECURITY ========================
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }
-dontwarn androidx.security.**

# ======================== SERIALIZATION ========================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ======================== PARCELABLE ========================
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ======================== ENUMS ========================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ======================== REFLECTION ========================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod

# Keep class members for reflection
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ======================== LOGGING (Remove in release) ========================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# But keep warnings and errors
-keep class android.util.Log {
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Remove Timber logs in release
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ======================== FIREBASE (if added later) ========================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ======================== CUSTOM APP CLASSES ========================
# Keep SecurityManager methods
-keep class com.freehands.assistant.SecurityManager {
    public *;
}

# Keep CommandExecutor
-keep class com.freehands.assistant.CommandExecutor {
    public *;
}

# Keep VoskManager
-keep class com.freehands.assistant.VoskManager {
    public *;
}

# Keep all service classes
-keep class * extends android.app.Service {
    public *;
}

# ======================== OPTIMIZATION ========================
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization options
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ======================== WARNINGS TO IGNORE ========================
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ======================== CRASHLYTICS (if added) ========================
# Keep file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
