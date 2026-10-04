# Reown AppKit (WalletConnect) ships native code through JNA and uniffi.
-keepattributes *Annotation*
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** {
    native <methods>;
    *;
}
-keep class uniffi.** { *; }
-dontwarn uniffi.**
-dontwarn com.sun.jna.**
# kotlinx.serialization: keep the generated serializers of our wire models.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class app.orbz.android.**$$serializer { *; }
-keepclassmembers class app.orbz.android.** { *** Companion; }
-keepclasseswithmembers class app.orbz.android.** { kotlinx.serialization.KSerializer serializer(...); }
# OkHttp
-dontwarn okhttp3.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
