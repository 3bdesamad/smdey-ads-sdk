# Proguard rules for smdey-ads library
-keep class com.smdey.ads.** { *; }
-keepclassmembers class com.smdey.ads.** { *; }

# Keep Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
