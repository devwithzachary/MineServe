# Proguard / R8 rules for MineServe

# Preserve JNI boundary methods and classes
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.devwithzachary.mineserve.engine.PtyNative { *; }

# Preserve data models and serialization (JSON / serialization reflection)
-keep class com.devwithzachary.mineserve.model.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# OkHttp & Okio rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines rules
-dontwarn kotlinx.coroutines.**

# Compose runtime rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
