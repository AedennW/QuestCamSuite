# Camera2 / MediaCodec keep rules (reflection via vendor metadata keys)
-keep class android.hardware.camera2.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }

# DataStore preferences
-keep class androidx.datastore.** { *; }

# Keep model classes used by diagnostics reporting
-keep class com.camerasuite.quest.diagnostics.** { *; }

-dontwarn org.jetbrains.annotations.**
