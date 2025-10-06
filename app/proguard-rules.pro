# Основные настройки оптимизации
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-keepattributes *Annotation*
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-repackageclasses ''

# Сохраняем нативные методы
-keepclasseswithmembernames class * {
    native <methods>;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keep class * extends java.lang.annotation.Annotation { *; }
-keep @javax.inject.Singleton class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Наш код
-keep class com.freehands.assistant.utils.audio.** { *; }
-keep class com.freehands.assistant.native.** { *; }
-keep class com.freehands.assistant.ui.audio.** { *; }

# Поддержка Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Поддержка R8
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Сохраняем ViewBinding
-keepclassmembers class * {
    @androidx.viewbinding.BindView *;
    @butterknife.BindView *;
}

# Избегаем предупреждений
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn kotlinx.coroutines.**
-dontwarn org.jetbrains.annotations.**

# Оптимизация ресурсов
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
