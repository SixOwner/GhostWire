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
                serverAddress = "18.222.138.62",
                serverPort = 443,
                uuid = "28553ef2-129e-47ab-996a-bb72d9332de8",
                publicKey = "C1AqBvToTDE3pO5mmee-gwhv7zs31JW04gyy9FzAz3U",
                shortId = "bb1e90923f11e3fb",
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
                serverAddress = "18.222.138.62",
                serverPort = 53,
                publicKey = "fca1646f8c93ee016f2c6e138ffc75920e7890b78eba028de456b54baff43445",
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
