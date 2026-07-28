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

# Firebase / ML Kit component registrars are instantiated reflectively (no-arg constructor) by
# ComponentDiscoveryService at startup. Without this rule R8 strips the no-arg constructor even
# though it keeps the class, so every registrar (Crashlytics, Auth, Messaging, AppCheck,
# Installations, MLKit, ...) fails with NoSuchMethodException and the app crashes on every
# cold start in ScamRadarApp.onCreate ("FirebaseCrashlytics component is not present").
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}
