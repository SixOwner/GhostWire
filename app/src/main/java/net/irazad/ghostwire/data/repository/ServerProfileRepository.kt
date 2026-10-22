package net.irazad.ghostwire.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.irazad.ghostwire.data.local.BuiltinConfigVault
import net.irazad.ghostwire.data.models.ServerProfile
import net.irazad.ghostwire.vpn.NativeBridge
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class ServerProfileRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ghostwire_profiles_store", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow<List<ServerProfile>>(emptyList())
    val profiles: StateFlow<List<ServerProfile>> = _profiles.asStateFlow()

    private val _isTestingLatencies = MutableStateFlow(false)
    val isTestingLatencies: StateFlow<Boolean> = _isTestingLatencies.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val storedJson = prefs.getString("custom_profiles_json", null)
        val customList = mutableListOf<ServerProfile>()

        if (!storedJson.isNullOrEmpty()) {
            try {
                val array = JSONArray(storedJson)
                for (i in 0 until array.length()) {
                    customList.add(ServerProfile.fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        val factoryProfiles = BuiltinConfigVault.getFactoryDefaultProfiles()
        val combined = factoryProfiles + customList
        _profiles.value = combined
    }

    private fun saveCustomProfiles() {
        val customOnly = _profiles.value.filter { !it.isBuiltin }
        val array = JSONArray()
        for (profile in customOnly) {
            array.put(profile.toJson())
        }
        prefs.edit().putString("custom_profiles_json", array.toString()).apply()
    }

    fun restoreFactoryDefaults() {
        val customOnly = _profiles.value.filter { !it.isBuiltin }
        val factoryProfiles = BuiltinConfigVault.getFactoryDefaultProfiles()
        _profiles.value = factoryProfiles + customOnly
    }

    fun addProfile(profile: ServerProfile) {
        val current = _profiles.value.toMutableList()
        current.removeAll { it.id == profile.id }
        current.add(profile)
        _profiles.value = current
        saveCustomProfiles()
    }

    fun deleteProfile(profileId: String) {
        val current = _profiles.value.toMutableList()
        current.removeAll { it.id == profileId && !it.isBuiltin }
        _profiles.value = current
        saveCustomProfiles()
    }

    fun updateTransportParameters(
        profileId: String,
        newResolverIp: String?,
        newMtu: Int,
        newPort: Int,
        noiseInjection: Boolean
    ) {
        val updated = _profiles.value.map { profile ->
            if (profile.id == profileId) {
                profile.copy(
                    resolverIp = newResolverIp,
                    mtu = newMtu,
                    serverPort = newPort,
                    noiseInjection = noiseInjection
                )
            } else {
                profile
            }
        }
        _profiles.value = updated
        saveCustomProfiles()
    }

    fun applyIpOrDnsToProfile(profileId: String, newIp: String?, newDns: String?) {
        val updated = _profiles.value.map { profile ->
            if (profile.id == profileId) {
                profile.copy(
                    serverAddress = newIp?.takeIf { it.isNotBlank() } ?: profile.serverAddress,
                    resolverIp = newDns?.takeIf { it.isNotBlank() } ?: profile.resolverIp
                )
            } else {
                profile
            }
        }
        _profiles.value = updated
        saveCustomProfiles()
    }

    /**
     * Imports configuration from URI (vless://, vmess://, trojan://, ss://, dnstt://, ghostwire://)
     */
    suspend fun importProfile(input: String): Result<ServerProfile> = withContext(Dispatchers.IO) {
        try {
            val trimmed = input.trim()

            // WireGuard text format (.conf)
            if (trimmed.contains("[Interface]", ignoreCase = true) && trimmed.contains("[Peer]", ignoreCase = true)) {
                return@withContext importWireguardConfig(trimmed, "WireGuard Imported")
            }

            // Universal Multi-Protocol Parser
            val profile = when {
                trimmed.startsWith("ghostwire://", true) -> parseGhostwireUri(trimmed)
                trimmed.startsWith("vless://", true) -> parseVlessUri(trimmed)
                trimmed.startsWith("vmess://", true) -> parseVmessUri(trimmed)
                trimmed.startsWith("trojan://", true) -> parseTrojanUri(trimmed)
                trimmed.startsWith("ss://", true) -> parseShadowsocksUri(trimmed)
                trimmed.startsWith("dnstt://", true) -> parseDnsttUri(trimmed)
                else -> {
                    if (NativeBridge.isLoaded()) {
                        val nativeParsedJson = NativeBridge.parseConfigurationUri(trimmed)
                        val json = JSONObject(nativeParsedJson)
                        if (!json.has("error")) {
                            ServerProfile.fromJson(json)
                        } else {
                            throw IllegalArgumentException(json.getString("error"))
                        }
                    } else {
                        throw IllegalArgumentException("Unsupported configuration format")
                    }
                }
            }

            addProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports standard WireGuard .conf file
     */
    fun importWireguardConfig(confText: String, profileName: String): Result<ServerProfile> {
        try {
            var privateKey = ""
            var address = ""
            var dns = ""
            var mtu = 1420
            var publicKey = ""
            var endpoint = ""

            val lines = confText.lines()
            var currentSection = ""
            for (line in lines) {
                val clean = line.trim()
                if (clean.startsWith("#") || clean.isEmpty()) continue
                if (clean.startsWith("[") && clean.endsWith("]")) {
                    currentSection = clean.uppercase()
                    continue
                }
                val parts = clean.split("=", limit = 2)
                if (parts.size != 2) continue
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()

                when (currentSection) {
                    "[INTERFACE]" -> {
                        when (key) {
                            "privatekey" -> privateKey = value
                            "address" -> address = value
                            "dns" -> dns = value
                            "mtu" -> mtu = value.toIntOrNull() ?: 1420
                        }
                    }
                    "[PEER]" -> {
                        when (key) {
                            "publickey" -> publicKey = value
                            "endpoint" -> endpoint = value
                        }
                    }
                }
            }

            val hostPort = endpoint.split(":")
            val host = hostPort.getOrElse(0) { "127.0.0.1" }
            val port = hostPort.getOrNull(1)?.toIntOrNull() ?: 51820

            val profile = ServerProfile(
                id = "wg_${System.currentTimeMillis()}",
                name = profileName.takeIf { it.isNotBlank() } ?: "WireGuard Secure",
                protocol = "WireGuard",
                serverAddress = host,
                serverPort = port,
                uuid = privateKey,
                publicKey = publicKey,
                resolverIp = dns.takeIf { it.isNotEmpty() },
                mtu = mtu,
                customConfig = confText,
                isBuiltin = false,
                pingMs = -1
            )
            addProfile(profile)
            return Result.success(profile)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun parseGhostwireUri(uriStr: String): ServerProfile {
        val uri = Uri.parse(uriStr)
        val userInfo = uri.userInfo ?: ""
        val host = uri.host ?: "127.0.0.1"
        val port = if (uri.port > 0) uri.port else 443
        val realityPk = uri.getQueryParameter("reality_pk")
        val realitySid = uri.getQueryParameter("reality_sid")
        val sni = uri.getQueryParameter("sni") ?: "gateway.icloud.com"
        val dnsttPk = uri.getQueryParameter("dnstt_pk")
        val dnsttDomain = uri.getQueryParameter("dnstt_domain")
        val name = uri.fragment?.takeIf { it.isNotEmpty() } ?: "GhostWire Universal Node"

        return ServerProfile(
            id = "gw_${System.currentTimeMillis()}",
            name = name,
            protocol = "WireReality",
            serverAddress = host,
            serverPort = port,
            uuid = userInfo,
            publicKey = realityPk ?: dnsttPk,
            shortId = realitySid,
            sni = sni,
            tunnelDomain = dnsttDomain,
            mtu = 1400,
            isBuiltin = false
        )
    }

    private fun parseVlessUri(uriStr: String): ServerProfile {
        val uri = Uri.parse(uriStr)
        val uuid = uri.userInfo ?: ""
        val host = uri.host ?: "127.0.0.1"
        val port = if (uri.port > 0) uri.port else 443
        val pbk = uri.getQueryParameter("pbk")
        val sid = uri.getQueryParameter("sid")
        val sni = uri.getQueryParameter("sni")
        val name = uri.fragment?.takeIf { it.isNotEmpty() } ?: "VLESS Reality Node"

        return ServerProfile(
            id = "vless_${System.currentTimeMillis()}",
            name = name,
            protocol = "WireReality",
            serverAddress = host,
            serverPort = port,
            uuid = uuid,
            publicKey = pbk,
            shortId = sid,
            sni = sni,
            mtu = 1400,
            isBuiltin = false
        )
    }

    private fun parseVmessUri(uriStr: String): ServerProfile {
        val b64 = uriStr.removePrefix("vmess://").trim()
        val decoded = String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8)
        val json = JSONObject(decoded)
        val host = json.optString("add", "127.0.0.1")
        val port = json.optInt("port", 443)
        val id = json.optString("id", "")
        val sni = json.optString("sni", json.optString("host", ""))
        val ps = json.optString("ps", "VMess Node")

        return ServerProfile(
            id = "vmess_${System.currentTimeMillis()}",
            name = ps,
            protocol = "VMess",
            serverAddress = host,
            serverPort = port,
            uuid = id,
            sni = sni.takeIf { it.isNotEmpty() },
            mtu = 1400,
            isBuiltin = false
        )
    }

    private fun parseTrojanUri(uriStr: String): ServerProfile {
        val uri = Uri.parse(uriStr)
        val password = uri.userInfo ?: ""
        val host = uri.host ?: "127.0.0.1"
        val port = if (uri.port > 0) uri.port else 443
        val sni = uri.getQueryParameter("sni") ?: host
        val name = uri.fragment?.takeIf { it.isNotEmpty() } ?: "Trojan Node"

        return ServerProfile(
            id = "trojan_${System.currentTimeMillis()}",
            name = name,
            protocol = "Trojan",
            serverAddress = host,
            serverPort = port,
            uuid = password,
            sni = sni,
            mtu = 1400,
            isBuiltin = false
        )
    }

    private fun parseShadowsocksUri(uriStr: String): ServerProfile {
        val uri = Uri.parse(uriStr)
        val host = uri.host ?: "127.0.0.1"
        val port = if (uri.port > 0) uri.port else 8388
        val name = uri.fragment?.takeIf { it.isNotEmpty() } ?: "Shadowsocks Node"

        return ServerProfile(
            id = "ss_${System.currentTimeMillis()}",
            name = name,
            protocol = "Shadowsocks",
            serverAddress = host,
            serverPort = port,
            uuid = uri.userInfo,
            mtu = 1400,
            isBuiltin = false
        )
    }

    private fun parseDnsttUri(uriStr: String): ServerProfile {
        val uri = Uri.parse(uriStr)
        val pubkey = uri.userInfo ?: ""
        val domain = uri.host ?: "t.nteam.bond"
        val server = uri.getQueryParameter("server") ?: "18.222.138.62"
        val port = if (uri.port > 0) uri.port else 53
        val name = uri.fragment?.takeIf { it.isNotEmpty() } ?: "GhostDNS Core"

        return ServerProfile(
            id = "dnstt_${System.currentTimeMillis()}",
            name = name,
            protocol = "GhostDNS Core",
            serverAddress = server,
            serverPort = port,
            publicKey = pubkey,
            tunnelDomain = domain,
            resolverIp = "10.201.34.14",
            mtu = 1232,
            isBuiltin = false
        )
    }

    suspend fun testAllLatencies() = withContext(Dispatchers.IO) {
        _isTestingLatencies.value = true
        try {
            val currentList = _profiles.value
            val updated = currentList.map { profile ->
                val latency = measureTcpLatency(profile.serverAddress, profile.serverPort)
                profile.copy(pingMs = latency)
            }
            _profiles.value = updated
        } finally {
            _isTestingLatencies.value = false
        }
    }

    private fun measureTcpLatency(host: String, port: Int): Int {
        if (host.isBlank()) return -1
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2500)
            }
            (System.currentTimeMillis() - start).toInt().coerceAtLeast(1)
        } catch (e: Exception) {
            -1
        }
    }
}
