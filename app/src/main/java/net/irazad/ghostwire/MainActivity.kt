package net.irazad.ghostwire

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.irazad.ghostwire.data.models.AppLanguage
import net.irazad.ghostwire.data.models.ServerProfile
import net.irazad.ghostwire.data.models.VpnStatus
import net.irazad.ghostwire.data.repository.RadarRepository
import net.irazad.ghostwire.data.repository.ServerProfileRepository
import net.irazad.ghostwire.data.repository.UserPreferencesRepository
import net.irazad.ghostwire.ui.components.FloatingNavBar
import net.irazad.ghostwire.ui.navigation.NavigationTab
import net.irazad.ghostwire.ui.screens.*
import net.irazad.ghostwire.ui.theme.GhostWireTheme
import net.irazad.ghostwire.vpn.GhostWireVpnController
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var userPrefsRepo: UserPreferencesRepository
    private lateinit var profileRepo: ServerProfileRepository
    private lateinit var radarRepo: RadarRepository

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            connectCurrentProfile()
        } else {
            Toast.makeText(this, "VPN Permission is required to route traffic", Toast.LENGTH_SHORT).show()
        }
    }

    private val wireguardPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                if (!content.isNullOrBlank()) {
                    val fileName = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".conf") ?: "WireGuard Node"
                    val result = profileRepo.importWireguardConfig(content, fileName)
                    if (result.isSuccess) {
                        Toast.makeText(this@MainActivity, getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.import_error), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userPrefsRepo = UserPreferencesRepository(this)
        profileRepo = ServerProfileRepository(this)
        radarRepo = RadarRepository(this)

        handleIncomingUri(intent)

        setContent {
            val userPrefs by userPrefsRepo.userPreferences.collectAsState()
            val profiles by profileRepo.profiles.collectAsState()
            val isTestingLatencies by profileRepo.isTestingLatencies.collectAsState()
            val resolvers by radarRepo.resolvers.collectAsState()
            val isScanningRadar by radarRepo.isScanning.collectAsState()
            val scanProgress by radarRepo.scanProgress.collectAsState()
            val scanStage by radarRepo.scanStage.collectAsState()
            val activeFilter by radarRepo.activeFilter.collectAsState()
            val searchQuery by radarRepo.searchQuery.collectAsState()
            val isUpdatingDb by radarRepo.isUpdatingDb.collectAsState()
            val totalResolversCount by radarRepo.totalResolversCount.collectAsState()
            val telemetry by GhostWireVpnController.telemetry.collectAsState()

            var selectedTab by remember { mutableStateOf(NavigationTab.NEXUS) }
            var isAppRoutingOpen by remember { mutableStateOf(false) }

            // Dynamic Runtime Language and Directionality Engine
            val locale = if (userPrefs.language == AppLanguage.FA) Locale("fa") else Locale("en")
            val baseContext = LocalContext.current
            val localizedContext = remember(userPrefs.language) {
                val conf = Configuration(baseContext.resources.configuration)
                conf.setLocale(locale)
                conf.setLayoutDirection(locale)
                baseContext.createConfigurationContext(conf)
            }
            val localizedConfig = remember(userPrefs.language) {
                Configuration(baseContext.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }
            val layoutDirection = if (userPrefs.language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig,
                LocalLayoutDirection provides layoutDirection
            ) {
                GhostWireTheme(selectedTheme = userPrefs.theme) {
                    if (isAppRoutingOpen) {
                        AppRoutingScreen(
                            currentMode = userPrefs.splitTunnelMode,
                            selectedApps = userPrefs.splitTunnelApps,
                            onModeChange = { userPrefsRepo.updateSplitTunnelMode(it) },
                            onToggleApp = { userPrefsRepo.toggleAppInSplitTunnel(it) },
                            onSelectAll = { userPrefsRepo.updateSplitTunnelApps(it) },
                            onClearAll = { userPrefsRepo.updateSplitTunnelApps(emptySet()) },
                            onBack = { isAppRoutingOpen = false }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = MaterialTheme.colorScheme.background,
                            bottomBar = {
                                FloatingNavBar(
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it }
                                )
                            }
                        ) { innerPadding ->
                            val activeProfile = profiles.find { it.id == userPrefs.selectedProfileId } ?: profiles.firstOrNull()

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                when (selectedTab) {
                                    NavigationTab.NEXUS -> {
                                        NexusScreen(
                                            telemetry = telemetry,
                                            activeProfile = activeProfile,
                                            onToggleConnection = { toggleVpnConnection(activeProfile) }
                                        )
                                    }
                                    NavigationTab.RADAR -> {
                                        RadarScreen(
                                            resolvers = resolvers,
                                            totalCount = totalResolversCount,
                                            isScanning = isScanningRadar,
                                            isUpdatingDb = isUpdatingDb,
                                            scanProgress = scanProgress,
                                            scanStage = scanStage,
                                            activeFilter = activeFilter,
                                            searchQuery = searchQuery,
                                            activeProfile = activeProfile,
                                            onFilterChange = { radarRepo.setFilter(it) },
                                            onSearchQueryChange = { radarRepo.setSearchQuery(it) },
                                            onScanClick = {
                                                lifecycleScope.launch {
                                                    radarRepo.scanAllResolvers()
                                                }
                                            },
                                            onUpdateDbClick = {
                                                lifecycleScope.launch {
                                                    val result = radarRepo.updateResolversFromGithub()
                                                    if (result.isSuccess) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            getString(R.string.radar_db_updated, result.getOrNull() ?: 31247),
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    } else {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            getString(R.string.radar_db_update_failed),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            },
                                            onApplyNodeToProfile = { node ->
                                                if (activeProfile != null) {
                                                    if (node.targetType == "CLEAN_IP") {
                                                        profileRepo.applyIpOrDnsToProfile(activeProfile.id, node.ip, null)
                                                    } else {
                                                        profileRepo.applyIpOrDnsToProfile(activeProfile.id, null, node.ip)
                                                    }
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        getString(R.string.radar_applied_node, node.ip),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onAutoApplyBestNode = {
                                                val best = resolvers.filter { it.isHealthy && it.rttMs > 0 }.minByOrNull { it.rttMs }
                                                if (best != null && activeProfile != null) {
                                                    if (best.targetType == "CLEAN_IP") {
                                                        profileRepo.applyIpOrDnsToProfile(activeProfile.id, best.ip, null)
                                                    } else {
                                                        profileRepo.applyIpOrDnsToProfile(activeProfile.id, null, best.ip)
                                                    }
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        getString(R.string.radar_applied_node, best.ip),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                    NavigationTab.TACTICAL -> {
                                        TacticalScreen(
                                            profiles = profiles,
                                            selectedProfileId = userPrefs.selectedProfileId,
                                            isTestingLatencies = isTestingLatencies,
                                            onSelectProfile = { userPrefsRepo.updateSelectedProfileId(it) },
                                            onTestAllLatencies = {
                                                lifecycleScope.launch {
                                                    profileRepo.testAllLatencies()
                                                }
                                            },
                                            onImportProfile = { uri ->
                                                profileRepo.importProfile(uri)
                                            },
                                            onImportWireguardClick = {
                                                wireguardPickerLauncher.launch(arrayOf("*/*"))
                                            },
                                            onRestoreFactoryDefaults = {
                                                profileRepo.restoreFactoryDefaults()
                                                Toast.makeText(this@MainActivity, "Factory defaults restored", Toast.LENGTH_SHORT).show()
                                            },
                                            onDeleteProfile = { profileId ->
                                                profileRepo.deleteProfile(profileId)
                                            },
                                            onUpdateTransport = { pId, resIp, mtu, port, noise ->
                                                profileRepo.updateTransportParameters(pId, resIp, mtu, port, noise)
                                            }
                                        )
                                    }
                                    NavigationTab.ENGINE -> {
                                        EngineScreen(
                                            userPreferences = userPrefs,
                                            onLanguageChange = { newLang ->
                                                userPrefsRepo.updateLanguage(newLang)
                                            },
                                            onThemeChange = { userPrefsRepo.updateTheme(it) },
                                            onAutoMtuToggle = { userPrefsRepo.updateAutoMtu(it) },
                                            onManualMtuChange = { userPrefsRepo.updateManualMtu(it) },
                                            onDesyncToggle = { userPrefsRepo.updateDesync(it) },
                                            onResolverHoppingToggle = { userPrefsRepo.updateResolverHopping(it) },
                                            onSurvivalModeToggle = { userPrefsRepo.updateSurvivalMode(it) },
                                            onBackgroundStripperToggle = { userPrefsRepo.updateBackgroundStripper(it) },
                                            onZstdToggle = { userPrefsRepo.updateZstdCompression(it) },
                                            onConfigureAppRouting = { isAppRoutingOpen = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingUri(intent)
    }

    private fun handleIncomingUri(intent: Intent?) {
        val uri = intent?.dataString
        if (!uri.isNullOrEmpty()) {
            lifecycleScope.launch {
                val res = profileRepo.importProfile(uri)
                if (res.isSuccess) {
                    Toast.makeText(this@MainActivity, getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.import_error), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun toggleVpnConnection(profile: ServerProfile?) {
        val currentStatus = GhostWireVpnController.telemetry.value.status
        if (currentStatus == VpnStatus.CONNECTED || currentStatus == VpnStatus.CONNECTING) {
            GhostWireVpnController.disconnect(this)
        } else {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnPrepareLauncher.launch(vpnIntent)
            } else {
                connectCurrentProfile()
            }
        }
    }

    private fun connectCurrentProfile() {
        val profile = profileRepo.profiles.value.find { it.id == userPrefsRepo.userPreferences.value.selectedProfileId }
            ?: profileRepo.profiles.value.firstOrNull()

        if (profile != null) {
            GhostWireVpnController.connect(this, profile)
        }
    }
}
