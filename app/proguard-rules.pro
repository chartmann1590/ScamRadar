# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.charles.scamradar.app.data.model.** { *; }
-keep class com.charles.scamradar.app.data.db.** { *; }
-keep class com.charles.scamradar.app.recovery.** { *; }
-keep class com.charles.scamradar.app.engagement.** { *; }
-keep class com.charles.scamradar.app.family.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
