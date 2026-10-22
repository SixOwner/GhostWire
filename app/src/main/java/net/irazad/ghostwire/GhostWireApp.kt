package net.irazad.ghostwire

import android.app.Application
import net.irazad.ghostwire.vpn.NativeBridge

class GhostWireApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NativeBridge.initNativeCore(this)
    }
}
