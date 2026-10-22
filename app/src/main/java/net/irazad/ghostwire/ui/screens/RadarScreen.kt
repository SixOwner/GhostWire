package net.irazad.ghostwire.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.irazad.ghostwire.R
import net.irazad.ghostwire.data.models.RadarMetric
import net.irazad.ghostwire.data.models.ServerProfile
import net.irazad.ghostwire.data.repository.IspFilter
import net.irazad.ghostwire.ui.components.GlassCard
import net.irazad.ghostwire.ui.components.TacticalStatusBadge
import net.irazad.ghostwire.ui.theme.ElectricCyan
import net.irazad.ghostwire.ui.theme.SoftIndigo
import net.irazad.ghostwire.ui.theme.SoftTeal
import net.irazad.ghostwire.ui.theme.StatusAmber
import net.irazad.ghostwire.ui.theme.StatusGreen
import net.irazad.ghostwire.ui.theme.StatusRed

@Composable
fun RadarScreen(
    resolvers: List<RadarMetric>,
    isScanning: Boolean,
    scanProgress: Float,
    scanStage: String,
    activeFilter: IspFilter,
    activeProfile: ServerProfile?,
    onFilterChange: (IspFilter) -> Unit,
    onScanClick: () -> Unit,
    onApplyNodeToProfile: (RadarMetric) -> Unit,
    onAutoApplyBestNode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val filteredResolvers = resolvers.filter {
        when (activeFilter) {
            IspFilter.ALL -> true
            IspFilter.MCI -> it.isp.contains("MCI", ignoreCase = true)
            IspFilter.IRANCELL -> it.isp.contains("Irancell", ignoreCase = true)
            IspFilter.RIGHTEL -> it.isp.contains("Rightel", ignoreCase = true)
            IspFilter.FIXED -> it.isp.contains("Fixed", ignoreCase = true) || it.isp.contains("TCI", ignoreCase = true)
            IspFilter.CLEAN_IP -> it.targetType == "CLEAN_IP"
        }
    }

    val healthyCount = filteredResolvers.count { it.isHealthy && it.rttMs > 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.radar_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "100K+ Subnet Matrix & Anti-Poison Scanner",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onScanClick,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Scanning...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_radar),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Scan All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3-Stage Progress Indicator Banner (visible during or after scan)
        if (isScanning || scanProgress > 0f) {
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = scanStage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Text(
                            text = "${(scanProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = ElectricCyan,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Auto-Apply Best Node & Telemetry Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$healthyCount / ${filteredResolvers.size} Operational",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen
            )

            Button(
                onClick = {
                    onAutoApplyBestNode()
                    Toast.makeText(context, "Best Healthy Node applied to active config", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftTeal.copy(alpha = 0.2f),
                    contentColor = SoftTeal
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Auto-Apply Best", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                IspFilter.ALL to "All",
                IspFilter.CLEAN_IP to "Clean IP",
                IspFilter.MCI to "MCI",
                IspFilter.IRANCELL to "Irancell",
                IspFilter.FIXED to "Fixed"
            )

            filters.forEach { (filter, label) ->
                val isSelected = activeFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ElectricCyan else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onFilterChange(filter) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Resolvers & Subnets List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(filteredResolvers, key = { it.id }) { node ->
                RadarNodeCard(
                    node = node,
                    onApply = {
                        onApplyNodeToProfile(node)
                        Toast.makeText(context, "Applied ${node.ip} to active profile", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun RadarNodeCard(
    node: RadarMetric,
    onApply: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${node.ip}  •  ${node.cidrRange.ifEmpty { node.asn }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TacticalStatusBadge(
                        title = node.targetType,
                        accentColor = if (node.targetType == "CLEAN_IP") SoftIndigo else ElectricCyan
                    )
                    if (node.antiPoisonVerified && node.isHealthy) {
                        TacticalStatusBadge(
                            title = "Verified",
                            accentColor = StatusGreen
                        )
                    }
                }
            }

            // Latency & Apply Button
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (node.rttMs > 0) "${node.rttMs} ms" else "--",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (node.rttMs in 1..45) StatusGreen else if (node.rttMs > 45) StatusAmber else StatusRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan.copy(alpha = 0.15f),
                        contentColor = ElectricCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = "Use Node", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
