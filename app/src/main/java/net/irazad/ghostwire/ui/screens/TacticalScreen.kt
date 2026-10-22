package net.irazad.ghostwire.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.irazad.ghostwire.R
import net.irazad.ghostwire.data.models.ServerProfile
import net.irazad.ghostwire.ui.components.GlassCard
import net.irazad.ghostwire.ui.components.TacticalStatusBadge
import net.irazad.ghostwire.ui.theme.ElectricCyan
import net.irazad.ghostwire.ui.theme.SoftIndigo
import net.irazad.ghostwire.ui.theme.StatusAmber
import net.irazad.ghostwire.ui.theme.StatusGreen
import net.irazad.ghostwire.ui.theme.StatusRed

@Composable
fun TacticalScreen(
    profiles: List<ServerProfile>,
    selectedProfileId: String,
    isTestingLatencies: Boolean,
    onSelectProfile: (String) -> Unit,
    onTestAllLatencies: () -> Unit,
    onImportProfile: suspend (String) -> Result<ServerProfile>,
    onImportWireguardClick: () -> Unit,
    onRestoreFactoryDefaults: () -> Unit,
    onDeleteProfile: (String) -> Unit = {},
    onUpdateTransport: (profileId: String, resolverIp: String?, mtu: Int, port: Int, noise: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<ServerProfile?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Hub Header & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tactical_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "V2Ray, WireReality, WireGuard & DNS Matrix",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onTestAllLatencies,
                enabled = !isTestingLatencies,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isTestingLatencies) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.tactical_test_all),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row: Responsive 3-Action Suite
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Import URI / Clipboard
            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Import URI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            // Import WireGuard (.conf)
            OutlinedButton(
                onClick = onImportWireguardClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIndigo),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "WireGuard",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            // Restore Factory Defaults
            OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAmber),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Reset",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Profiles List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                val isSelected = profile.id == selectedProfileId
                ServerProfileCard(
                    profile = profile,
                    isSelected = isSelected,
                    onSelect = { onSelectProfile(profile.id) },
                    onEditTransport = { profileToEdit = profile },
                    onDelete = { onDeleteProfile(profile.id) }
                )
            }
        }
    }

    // Import Dialog
    if (showImportDialog) {
        var importInput by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showImportDialog = false },
            title = { Text(text = stringResource(R.string.import_dialog_title), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Supports: ghostwire://, vless://, vmess://, trojan://, ss://, dnstt://, WireGuard config",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipMgr.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clip.isNullOrBlank()) {
                                importInput = clip.trim()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = ElectricCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📋 Paste from Clipboard", fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = importInput,
                        onValueChange = {
                            importInput = it
                            importError = null
                        },
                        placeholder = { Text(text = stringResource(R.string.import_dialog_hint), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        maxLines = 5
                    )
                    if (importError != null) {
                        Text(text = importError!!, color = StatusRed, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInput.isBlank()) return@Button
                        isSubmitting = true
                        coroutineScope.launch {
                            val res = onImportProfile(importInput)
                            isSubmitting = false
                            if (res.isSuccess) {
                                showImportDialog = false
                            } else {
                                importError = res.exceptionOrNull()?.message ?: "Parsing error"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
                ) {
                    Text(text = stringResource(R.string.import_action), fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(text = stringResource(R.string.tactical_cancel), fontSize = 12.sp)
                }
            }
        )
    }

    // Factory Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(text = stringResource(R.string.tactical_restore_defaults), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text(text = stringResource(R.string.tactical_restore_confirm), fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreFactoryDefaults()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAmber, contentColor = Color.Black)
                ) {
                    Text(text = stringResource(R.string.tactical_restore_action), fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(text = stringResource(R.string.tactical_cancel), fontSize = 12.sp)
                }
            }
        )
    }

    // Edit Transport Modal Dialog
    if (profileToEdit != null) {
        val profile = profileToEdit!!
        var resolverIp by remember { mutableStateOf(profile.resolverIp ?: "") }
        var mtuStr by remember { mutableStateOf(profile.mtu.toString()) }
        var portStr by remember { mutableStateOf(profile.serverPort.toString()) }
        var noise by remember { mutableStateOf(profile.noiseInjection) }

        AlertDialog(
            onDismissRequest = { profileToEdit = null },
            title = { Text(text = stringResource(R.string.tactical_edit_transport), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = profile.name, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)

                    OutlinedTextField(
                        value = resolverIp,
                        onValueChange = { resolverIp = it },
                        label = { Text(stringResource(R.string.tactical_resolver_override), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mtuStr,
                        onValueChange = { mtuStr = it },
                        label = { Text(stringResource(R.string.tactical_mtu_override), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text(stringResource(R.string.tactical_port_override), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.tactical_noise_toggle), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = noise,
                            onCheckedChange = { noise = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricCyan,
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedMtu = mtuStr.toIntOrNull() ?: profile.mtu
                        val parsedPort = portStr.toIntOrNull() ?: profile.serverPort
                        onUpdateTransport(profile.id, resolverIp.takeIf { it.isNotBlank() }, parsedMtu, parsedPort, noise)
                        profileToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
                ) {
                    Text(text = stringResource(R.string.tactical_save), fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToEdit = null }) {
                    Text(text = stringResource(R.string.tactical_cancel), fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
private fun ServerProfileCard(
    profile: ServerProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEditTransport: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Profile Title, Protocol & Latency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${profile.protocol}  •  ${profile.serverAddress}:${profile.serverPort}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // Latency Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (profile.pingMs >= 0) "${profile.pingMs} ms" else "--",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (profile.pingMs in 0..60) StatusGreen else if (profile.pingMs > 60) StatusAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isSelected) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges & Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    TacticalStatusBadge(
                        title = if (profile.isBuiltin) "BUILTIN" else "CUSTOM",
                        accentColor = if (profile.isBuiltin) SoftIndigo else ElectricCyan
                    )
                    if (profile.noiseInjection) {
                        TacticalStatusBadge(
                            title = "NOISE",
                            accentColor = StatusGreen
                        )
                    }
                }

                // Right: Action Icons (Tune & Delete)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditTransport,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Edit",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (!profile.isBuiltin) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = StatusRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
