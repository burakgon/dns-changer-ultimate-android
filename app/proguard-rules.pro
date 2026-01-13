# Add project specific ProGuard rules here.

# Keep RevenueCat
-keep class com.revenuecat.purchases.** { *; }

# Keep AdMob
-keep class com.google.android.gms.ads.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
