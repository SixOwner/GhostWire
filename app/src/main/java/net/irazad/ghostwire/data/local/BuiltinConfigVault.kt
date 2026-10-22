package net.irazad.ghostwire.data.local

import net.irazad.ghostwire.data.models.ServerProfile
import net.irazad.ghostwire.vpn.NativeBridge
import org.json.JSONArray
import org.json.JSONObject

object BuiltinConfigVault {

    // Factory Defaults (Matches the live deployed AWS EC2 instance without plain-text credential leaks)
    fun getFactoryDefaultProfiles(): List<ServerProfile> {
        if (NativeBridge.isLoaded()) {
            try {
                val jsonStr = NativeBridge.getCloakedFactoryProfiles()
                val array = JSONArray(jsonStr)
                val list = mutableListOf<ServerProfile>()
                for (i in 0 until array.length()) {
                    list.add(ServerProfile.fromJson(array.getJSONObject(i)))
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                // Fallback to internal immutable vault
            }
        }

        return listOf(
            ServerProfile(
                id = "builtin_alpha_reality",
                name = "Ghost Core - Node Alpha [Blackout Ready]",
                protocol = "WireReality",
                serverAddress = "",
                serverPort = 443,
                uuid = "",
                publicKey = "",
                shortId = "",
                sni = "gateway.icloud.com",
                mtu = 1400,
                noiseInjection = true,
                isBuiltin = true,
                pingMs = 42
            ),
            ServerProfile(
                id = "builtin_beta_dnstt",
                name = "Ghost DNS - Node Beta [Emergency Tunnel]",
                protocol = "GhostDNS Core",
                serverAddress = "",
                serverPort = 53,
                publicKey = "",
                tunnelDomain = "",
                resolverIp = "10.201.34.14",
                mtu = 1232,
                noiseInjection = true,
                isBuiltin = true,
                pingMs = 38
            )
        )
    }
}
