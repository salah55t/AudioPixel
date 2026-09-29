# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# App
-keep class com.audiopixel.app.** { *; }
