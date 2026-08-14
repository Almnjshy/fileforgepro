# FileForge Pro ProGuard rules

# Keep application class
-keep class com.fileforge.pro.app.FileForgeApp { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * { @androidx.room.* <methods>; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-keep class androidx.compose.** { *; }

# Commons Compress (used by Archive engine)
-keep class org.apache.commons.compress.** { *; }

# Apache commons-net (FTP)
-keep class org.apache.commons.net.** { *; }

# Sardine (WebDAV)
-keep class com.googlecode.sardine.** { *; }

# Coil
-keep class coil.** { *; }

# PNG/JPEG/MP4 metadata
-keep class androidx.exifinterface.** { *; }
