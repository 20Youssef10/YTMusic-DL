# yt-dlp / YoutubeDL-Boom
-keep class com.yausername.** { *; }
-keep class com.facebook.soloader.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Hilt
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.example.ytdlpdownloader.**$$serializer { *; }
-keepclassmembers class com.example.ytdlpdownloader.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.ytdlpdownloader.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coil
-keep class coil.** { *; }
