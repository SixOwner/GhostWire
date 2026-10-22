package net.irazad.ghostwire.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import net.irazad.ghostwire.ui.theme.DeepViolet
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
    onRestoreFactoryDefaults: () -> Unit,
    onUpdateTransport: (profileId: String, resolverIp: String?, mtu: Int, port: Int, noise: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<ServerProfile?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Hub Header & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.tactical_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Multi-Server & Protocol Matrix",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTestAllLatencies,
                    enabled = !isTestingLatencies,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Banner Row: Import & Restore Factory Defaults
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan)
            ) {
                Text(text = stringResource(R.string.tactical_import_profile), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAmber)
            ) {
                Text(text = stringResource(R.string.tactical_restore_defaults), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Profiles List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                val isSelected = profile.id == selectedProfileId
                ServerProfileCard(
                    profile = profile,
                    isSelected = isSelected,
                    onSelect = { onSelectProfile(profile.id) },
                    onEditTransport = { profileToEdit = profile }
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
            title = { Text(text = stringResource(R.string.import_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = {
                            importInput = it
                            importError = null
                        },
                        placeholder = { Text(text = stringResource(R.string.import_dialog_hint), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        maxLines = 6
                    )
                    if (importError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = importError!!, color = StatusRed, fontSize = 12.sp)
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
                    Text(text = stringResource(R.string.import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(text = stringResource(R.string.tactical_cancel))
                }
            }
        )
    }

    // Factory Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(text = stringResource(R.string.tactical_restore_defaults), fontWeight = FontWeight.Bold) },
            text = { Text(text = stringResource(R.string.tactical_restore_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreFactoryDefaults()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAmber, contentColor = Color.Black)
                ) {
                    Text(text = stringResource(R.string.tactical_restore_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(text = stringResource(R.string.tactical_cancel))
                }
            }
        )
    }

    // Edit Transport Parameters Sheet
    profileToEdit?.let { profile ->
        var resolverIp by remember { mutableStateOf(profile.resolverIp ?: "10.201.34.14") }
        var mtuStr by remember { mutableStateOf(profile.mtu.toString()) }
        var portStr by remember { mutableStateOf(profile.serverPort.toString()) }
        var noise by remember { mutableStateOf(profile.noiseInjection) }

        AlertDialog(
            onDismissRequest = { profileToEdit = null },
            title = { Text(text = stringResource(R.string.tactical_edit_transport), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = profile.name, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)

                    OutlinedTextField(
                        value = resolverIp,
                        onValueChange = { resolverIp = it },
                        label = { Text(stringResource(R.string.tactical_resolver_override)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mtuStr,
                        onValueChange = { mtuStr = it },
                        label = { Text(stringResource(R.string.tactical_mtu_override)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text(stringResource(R.string.tactical_port_override)) },
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
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
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
                    Text(text = stringResource(R.string.tactical_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToEdit = null }) {
                    Text(text = stringResource(R.string.tactical_cancel))
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
    onEditTransport: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${profile.protocol}  •  MTU: ${profile.mtu}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Latency Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (profile.pingMs >= 0) "${profile.pingMs} ms" else "--",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (profile.pingMs in 0..50) StatusGreen else ElectricCyan
                    )
                    if (isSelected) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges & Transport Customize Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TacticalStatusBadge(
                        title = if (profile.isBuiltin) stringResource(R.string.tactical_builtin_tag) else stringResource(R.string.tactical_custom_tag),
                        accentColor = if (profile.isBuiltin) DeepViolet else ElectricCyan
                    )
                    if (profile.noiseInjection) {
                        TacticalStatusBadge(
                            title = "Noise Active",
                            accentColor = StatusGreen
                        )
                    }
                }

                TextButton(
                    onClick = onEditTransport,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Customize",
                        fontSize = 11.sp,
                        color = ElectricCyan
                    )
                }
            }
        }
    }
}
