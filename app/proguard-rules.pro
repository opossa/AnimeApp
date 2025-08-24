
-dontwarn
-dontpreverify
-dontoptimize

-keep class com.example.animeapp.activities.** { *; }

-keep class com.google.gson.Gson { *; }
-keep class com.example.animeapp.models.HostListModel { *; }

-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.bumptech.glide.** { *; }
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**