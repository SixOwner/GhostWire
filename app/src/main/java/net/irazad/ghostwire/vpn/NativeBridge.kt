package net.irazad.ghostwire.vpn

import android.content.Context
import android.util.Log

object NativeBridge {
    private const val TAG = "GhostWireNative"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("ghostwire_core")
            isNativeLoaded = true
        } catch (t: Throwable) {
            Log.w(TAG, "Native library libghostwire_core.so not yet linked: ${t.message}. Using resilient runtime fallback engine.")
            isNativeLoaded = false
        }
    }

    fun isLoaded(): Boolean = isNativeLoaded

    // Native JNI Declarations
    external fun initNativeCore(context: Context): Boolean
    external fun getCloakedFactoryProfiles(): String
    external fun parseConfigurationUri(uri: String): String
    external fun measureProfileLatency(profileJson: String): Int
    external fun measureAllProfiles(profilesJson: String): String
    external fun executeRadarScan(resolversJson: String): String
    external fun discoverOptimalMtu(targetHost: String): Int
    external fun startTunnel(tunFd: Int, profileJson: String): Boolean
    external fun stopTunnel()
}
