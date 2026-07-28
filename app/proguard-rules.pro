# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Android & Material classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Room Database, Entities, and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# Keep Data Models & Application Classes
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-keep class com.example.MainApplication { *; }
-keep class com.example.MainActivity { *; }

# Keep Moshi JSON Serializer
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <init>(...);
}

# Keep Retrofit & OkHttp
-keep class retrofit2.** { *; }
-keepclasseswithmembernames class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Keep Firebase Auth, Firestore, AppCheck, etc.
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Keep App Launcher Icons & Resources
-keepclassmembers class **.R$* {
    public static <fields>;
}

