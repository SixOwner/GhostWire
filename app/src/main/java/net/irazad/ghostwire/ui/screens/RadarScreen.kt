package net.irazad.ghostwire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.irazad.ghostwire.R
import net.irazad.ghostwire.data.models.RadarMetric
import net.irazad.ghostwire.data.repository.IspFilter
import net.irazad.ghostwire.ui.components.GlassCard
import net.irazad.ghostwire.ui.components.TacticalStatusBadge
import net.irazad.ghostwire.ui.theme.ElectricCyan
import net.irazad.ghostwire.ui.theme.StatusGreen
import net.irazad.ghostwire.ui.theme.StatusRed

@Composable
fun RadarScreen(
    resolvers: List<RadarMetric>,
    isScanning: Boolean,
    activeFilter: IspFilter,
    onFilterChange: (IspFilter) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredResolvers = resolvers.filter {
        when (activeFilter) {
            IspFilter.ALL -> true
            IspFilter.MCI -> it.isp.contains("MCI", ignoreCase = true)
            IspFilter.IRANCELL -> it.isp.contains("Irancell", ignoreCase = true)
            IspFilter.RIGHTEL -> it.isp.contains("Rightel", ignoreCase = true)
            IspFilter.FIXED -> !it.isp.contains("MCI", true) && !it.isp.contains("Irancell", true) && !it.isp.contains("Rightel", true)
        }
    }

    val healthyCount = filteredResolvers.count { it.isHealthy }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.radar_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.radar_subtitle),
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
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.radar_scanning), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_radar),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.radar_scan_all), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Health Matrix Metric Summary
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.radar_healthy_count, healthyCount, filteredResolvers.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                    Text(
                        text = "Sub-Net Matrix Operational",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TacticalStatusBadge(
                    title = "EDNS0 4096 Safe",
                    accentColor = ElectricCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ISP Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                label = stringResource(R.string.radar_filter_all),
                isSelected = activeFilter == IspFilter.ALL,
                onClick = { onFilterChange(IspFilter.ALL) }
            )
            FilterChip(
                label = stringResource(R.string.radar_filter_mci),
                isSelected = activeFilter == IspFilter.MCI,
                onClick = { onFilterChange(IspFilter.MCI) }
            )
            FilterChip(
                label = stringResource(R.string.radar_filter_irancell),
                isSelected = activeFilter == IspFilter.IRANCELL,
                onClick = { onFilterChange(IspFilter.IRANCELL) }
            )
            FilterChip(
                label = stringResource(R.string.radar_filter_rightel),
                isSelected = activeFilter == IspFilter.RIGHTEL,
                onClick = { onFilterChange(IspFilter.RIGHTEL) }
            )
            FilterChip(
                label = stringResource(R.string.radar_filter_fixed),
                isSelected = activeFilter == IspFilter.FIXED,
                onClick = { onFilterChange(IspFilter.FIXED) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Resolvers List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(filteredResolvers, key = { it.id }) { resolver ->
                ResolverCard(resolver = resolver)
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(
                if (isSelected) ElectricCyan else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResolverCard(resolver: RadarMetric) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolver.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${resolver.ip}:${resolver.port}  •  ${resolver.asn}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalStatusBadge(
                        title = if (resolver.antiPoisonVerified) {
                            stringResource(R.string.radar_verified)
                        } else {
                            stringResource(R.string.radar_poisoned)
                        },
                        accentColor = if (resolver.antiPoisonVerified) StatusGreen else StatusRed
                    )
                    TacticalStatusBadge(
                        title = stringResource(R.string.radar_edns_buffer, resolver.ednsBufferSupported),
                        accentColor = ElectricCyan
                    )
                }
            }

            // Latency Badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (resolver.rttMs >= 0) "${resolver.rttMs} ms" else "--",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (resolver.rttMs in 0..40) StatusGreen else ElectricCyan
                )
                Text(
                    text = if (resolver.packetLossPercent == 0) "0% Loss" else "${resolver.packetLossPercent}% Loss",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
