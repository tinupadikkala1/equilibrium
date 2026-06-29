# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging obfuscated stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name.
-renamesourcefileattribute SourceFile

# Room database keep rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.limits.LimitLimitAnnotation

# AdMob SDK keep rules
-keep public class com.google.android.gms.ads.** { public *; }
-keep public class com.google.ads.** { public *; }
-dontwarn com.google.android.gms.ads.**

# Jetpack Compose keep rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn androidx.compose.**
