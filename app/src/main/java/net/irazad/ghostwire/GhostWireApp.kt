package net.irazad.ghostwire

import android.app.Application
import android.util.Log
import net.irazad.ghostwire.vpn.NativeBridge

class GhostWireApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Guard against uncaught crashes in background threads
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GhostWireApp", "Uncaught crash on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

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
