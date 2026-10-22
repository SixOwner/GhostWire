# Keep JNI entry points and NativeBridge
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class net.irazad.ghostwire.vpn.NativeBridge { *; }
-keep class net.irazad.ghostwire.vpn.GhostWireVpnService { *; }
-keep class net.irazad.ghostwire.data.models.** { *; }

# Keep Activity and Application classes
-keep public class net.irazad.ghostwire.MainActivity { *; }
-keep public class net.irazad.ghostwire.GhostWireApp { *; }

# Coroutines and DataStore rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.**

