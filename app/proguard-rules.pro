# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# kotlinx.serialization keep rules for Gemini SDK
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class **$serializer {
    *** INSTANCE;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    *** CreatingSerializer;
}

# Keep DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences

# Keep Android KeyStore and AES Security components
-keep class com.fahim.geminiApiComposeStarter.data.security.** { *; }
-keep class com.fahim.geminiApiComposeStarter.data.local.** { *; }
