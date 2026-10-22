package net.irazad.ghostwire.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.irazad.ghostwire.R
import net.irazad.ghostwire.data.models.ServerProfile
import net.irazad.ghostwire.data.models.VpnStatus
import net.irazad.ghostwire.data.models.VpnTelemetry
import net.irazad.ghostwire.ui.components.*
import net.irazad.ghostwire.ui.theme.ElectricCyan
import net.irazad.ghostwire.ui.theme.DeepViolet
import net.irazad.ghostwire.ui.theme.StatusGreen

@Composable
fun NexusScreen(
    telemetry: VpnTelemetry,
    activeProfile: ServerProfile?,
    onToggleConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header & Security Posture
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.app_version_display),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TacticalStatusBadge(
                title = if (telemetry.status == VpnStatus.CONNECTED) {
                    stringResource(R.string.auto_diagnosis_blackout)
                } else {
                    stringResource(R.string.auto_diagnosis_optimal)
                },
                accentColor = if (telemetry.status == VpnStatus.CONNECTED) StatusGreen else ElectricCyan
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Prominent Biometric Connect Ring (One-Tap Action)
        BiometricConnectRing(
            status = telemetry.status,
            onToggle = onToggleConnection
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Selected Node Cloaked Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.selected_node_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeProfile?.name ?: telemetry.activeNodeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                TacticalStatusBadge(
                    title = activeProfile?.protocol ?: telemetry.activeProtocol,
                    accentColor = ElectricCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Real-Time Throughput Waveform
        LiveThroughputChart(
            uploadBps = telemetry.uploadSpeedBps,
            downloadBps = telemetry.downloadSpeedBps
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Deep Tactical Telemetry 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Latency Metric
            GlassCard(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        text = stringResource(R.string.telemetry_latency),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (telemetry.status == VpnStatus.CONNECTED) "${telemetry.latencyMs} ms" else "--",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
            }

            // Packet Loss Metric
            GlassCard(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        text = stringResource(R.string.telemetry_packet_loss),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (telemetry.status == VpnStatus.CONNECTED) "${telemetry.packetLossPercent}%" else "--",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Uplink Total
            GlassCard(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        text = stringResource(R.string.telemetry_upload),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatBytes(telemetry.totalBytesUploaded),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepViolet
                    )
                }
            }

            // Downlink Total
            GlassCard(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        text = stringResource(R.string.telemetry_download),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatBytes(telemetry.totalBytesDownloaded),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // Padding for floating nav bar
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        else -> String.format("%.0f KB", kb)
    }
}
