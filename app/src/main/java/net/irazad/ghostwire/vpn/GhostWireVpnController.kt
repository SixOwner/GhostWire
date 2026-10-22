package net.irazad.ghostwire.vpn

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.irazad.ghostwire.data.models.ServerProfile
import net.irazad.ghostwire.data.models.VpnStatus
import net.irazad.ghostwire.data.models.VpnTelemetry

object GhostWireVpnController {

    private val _telemetry = MutableStateFlow(VpnTelemetry())
    val telemetry: StateFlow<VpnTelemetry> = _telemetry.asStateFlow()

    fun updateStatus(status: VpnStatus) {
        _telemetry.value = _telemetry.value.copy(status = status)
    }

    fun updateTelemetry(telemetry: VpnTelemetry) {
        _telemetry.value = telemetry
    }

    fun connect(context: Context, profile: ServerProfile) {
        val intent = Intent(context, GhostWireVpnService::class.java).apply {
            action = GhostWireVpnService.ACTION_CONNECT
            putExtra(GhostWireVpnService.EXTRA_PROFILE_JSON, profile.toJson().toString())
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun disconnect(context: Context) {
        val intent = Intent(context, GhostWireVpnService::class.java).apply {
            action = GhostWireVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }
}
