package net.irazad.ghostwire

import android.app.Application
import android.util.Log
import net.irazad.ghostwire.vpn.NativeBridge

class GhostWireApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (NativeBridge.isLoaded()) {
                NativeBridge.initNativeCore(this)
            } else {
                Log.w("GhostWireApp", "Native core not loaded; fallback engine active.")
            }
        } catch (t: Throwable) {
            Log.e("GhostWireApp", "Native core init failed: ${t.message}", t)
        }
    }
}
