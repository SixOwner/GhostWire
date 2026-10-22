package net.irazad.ghostwire.data.models

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

data class VpnTelemetry(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val uploadSpeedBps: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val totalBytesUploaded: Long = 0L,
    val totalBytesDownloaded: Long = 0L,
    val sessionDurationSeconds: Long = 0L,
    val latencyMs: Int = 0,
    val packetLossPercent: Int = 0,
    val activeProtocol: String = "WireReality",
    val activeNodeName: String = "Ghost Core - Node Alpha [Blackout Ready]"
)
