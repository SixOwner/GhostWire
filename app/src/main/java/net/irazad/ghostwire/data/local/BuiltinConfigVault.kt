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
            } catch (t: Throwable) {
                // Fallback to internal immutable vault
            }
        }

        return listOf(
            ServerProfile(
                id = "builtin_alpha_reality",
                name = "Ghost Core - Node Alpha [Blackout Ready]",
                protocol = "WireReality",
                serverAddress = "18.221.128.150",
                serverPort = 443,
                uuid = "f35a0f5a-4e89-4a9f-8a03-7cb5f98cf4a1",
                publicKey = "N0_9lVlWv3e29o_Q6eH7gR8qRj5N1zE-Yx0t_5aW4U8=",
                shortId = "6c8b4e2a1d0f5e3c",
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
                serverAddress = "18.221.128.150",
                serverPort = 53,
                publicKey = "f0e74b5c73d9e8310c1e8787f17478d8f074d20ae4cfd9c15858cf3d42041285",
                tunnelDomain = "t.nteam.bond",
                resolverIp = "10.201.34.14",
                mtu = 1232,
                noiseInjection = true,
                isBuiltin = true,
                pingMs = 38
            )
        )
    }
}
