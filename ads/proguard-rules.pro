# Proguard rules for smdey-ads library
-keep class com.smdey.ads.** { *; }
-keepclassmembers class com.smdey.ads.** { *; }

# Keep Google Mobile Ads (GMA Next-Gen SDK & UMP)
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-keep class com.google.android.ump.** { *; }
