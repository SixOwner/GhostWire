package net.irazad.ghostwire.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Search
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
    totalCount: Int,
    isScanning: Boolean,
    isUpdatingDb: Boolean,
    scanProgress: Float,
    scanStage: String,
    activeFilter: IspFilter,
    searchQuery: String,
    activeProfile: ServerProfile?,
    onFilterChange: (IspFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onUpdateDbClick: () -> Unit,
    onApplyNodeToProfile: (RadarMetric) -> Unit,
    onAutoApplyBestNode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val healthyCount = resolvers.count { it.isHealthy && it.rttMs > 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = stringResource(R.string.radar_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$totalCount+ Verified Resolvers (SAMPA-ASA V8)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Update DB from GitHub Button
                FilledTonalButton(
                    onClick = onUpdateDbClick,
                    enabled = !isUpdatingDb && !isScanning,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SoftIndigo.copy(alpha = 0.2f),
                        contentColor = SoftIndigo
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    if (isUpdatingDb) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            color = SoftIndigo,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = stringResource(R.string.radar_update_db),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isUpdatingDb) stringResource(R.string.radar_updating_db) else stringResource(R.string.radar_update_db),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Scan Button
                Button(
                    onClick = onScanClick,
                    enabled = !isScanning && !isUpdatingDb,
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
                        Text(
                            text = stringResource(R.string.radar_scanning),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_radar),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.radar_scan_all),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar for 31,000+ Resolvers & Subnets
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.radar_search_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                cursorColor = ElectricCyan
            )
        )

        // 3-Stage Progress Indicator Banner
        if (isScanning || scanProgress > 0f) {
            Spacer(modifier = Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Auto-Apply Best Node & Telemetry Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$healthyCount / ${resolvers.size} Operational",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen
            )

            Button(
                onClick = {
                    onAutoApplyBestNode()
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
                Text(
                    text = stringResource(R.string.radar_auto_apply_best),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val filters = listOf(
                IspFilter.ALL to stringResource(R.string.radar_filter_all),
                IspFilter.CLEAN_IP to stringResource(R.string.radar_filter_clean_ip),
                IspFilter.MCI to stringResource(R.string.radar_filter_mci),
                IspFilter.IRANCELL to stringResource(R.string.radar_filter_irancell),
                IspFilter.RIGHTEL to stringResource(R.string.radar_filter_rightel),
                IspFilter.FIXED to stringResource(R.string.radar_filter_fixed)
            )

            filters.forEach { (filter, label) ->
                val isSelected = activeFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ElectricCyan else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onFilterChange(filter) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Resolvers & Subnets List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(resolvers, key = { it.id }) { node ->
                RadarNodeCard(
                    node = node,
                    onApply = {
                        onApplyNodeToProfile(node)
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
                    text = "${node.ip}:${node.port}  •  ${node.cidrRange.ifEmpty { node.asn }}",
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
                            title = stringResource(R.string.radar_verified),
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
                    color = if (node.rttMs in 1..48) StatusGreen else if (node.rttMs > 48) StatusAmber else StatusRed
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
                    Text(
                        text = stringResource(R.string.radar_use_node),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
