# Cactus native runtime & models
-keep class com.cactus.** { *; }
-dontwarn com.cactus.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# CameraX
-keep class androidx.camera.core.** { *; }
-dontwarn androidx.camera.core.**
