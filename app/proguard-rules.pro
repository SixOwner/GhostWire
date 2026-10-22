# Keep entire GhostWire application suite
-keep class net.irazad.ghostwire.** { *; }
-keepclasseswithmembers class net.irazad.ghostwire.** { *; }

# Keep JNI entry points and NativeBridge
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class net.irazad.ghostwire.vpn.NativeBridge { *; }
-keep class net.irazad.ghostwire.vpn.GhostWireVpnService { *; }
-keep class net.irazad.ghostwire.vpn.GhostWireVpnController { *; }
-keep class net.irazad.ghostwire.data.models.** { *; }

# Keep Activity and Application classes
-keep public class net.irazad.ghostwire.MainActivity { *; }
-keep public class net.irazad.ghostwire.GhostWireApp { *; }

# Compose & DataStore runtime integrity
-keep class androidx.compose.** { *; }
-keep class androidx.datastore.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.**
-dontobfuscate

