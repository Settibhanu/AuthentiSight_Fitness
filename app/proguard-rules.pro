# Snapdragon Fitness App — ProGuard Rules

# Keep SNPE JNI classes
-keep class com.fitness.snapapp.ai.qairt.** { *; }
-keepclassmembers class com.fitness.snapapp.ai.qairt.SNPEHelper {
    native <methods>;
}

# Keep Room entities and DAOs
-keep class com.fitness.snapapp.data.room.entity.** { *; }
-keep class com.fitness.snapapp.data.room.dao.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Filament
-keep class com.google.android.filament.** { *; }
-dontwarn com.google.android.filament.**

# Keep CameraX
-keep class androidx.camera.** { *; }

# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# General Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**
