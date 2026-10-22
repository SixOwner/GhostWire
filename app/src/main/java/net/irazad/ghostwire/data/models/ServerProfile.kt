package net.irazad.ghostwire.data.models

import org.json.JSONObject

data class ServerProfile(
    val id: String,
    val name: String,
    val protocol: String, // WireReality, GhostDNS Core, GhostStream, GhostShade, etc.
    val serverAddress: String,
    val serverPort: Int,
    val uuid: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val sni: String? = null,
    val tunnelDomain: String? = null,
    val resolverIp: String? = null,
    val mtu: Int = 1400,
    val noiseInjection: Boolean = true,
    val isBuiltin: Boolean = false,
    val pingMs: Int = -1
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("protocol", protocol)
            put("server_address", serverAddress)
            put("server_port", serverPort)
            putOpt("uuid", uuid)
            putOpt("public_key", publicKey)
            putOpt("short_id", shortId)
            putOpt("sni", sni)
            putOpt("tunnel_domain", tunnelDomain)
            putOpt("resolver_ip", resolverIp)
            put("mtu", mtu)
            put("noise_injection", noiseInjection)
            put("is_builtin", isBuiltin)
            put("ping_ms", pingMs)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ServerProfile {
            return ServerProfile(
                id = json.optString("id", "profile_${System.currentTimeMillis()}"),
                name = json.optString("name", "Custom Node"),
                protocol = json.optString("protocol", "WireReality"),
                serverAddress = json.optString("server_address", "127.0.0.1"),
                serverPort = json.optInt("server_port", 443),
                uuid = json.optString("uuid").takeIf { it.isNotEmpty() },
                publicKey = json.optString("public_key").takeIf { it.isNotEmpty() },
                shortId = json.optString("short_id").takeIf { it.isNotEmpty() },
                sni = json.optString("sni").takeIf { it.isNotEmpty() },
                tunnelDomain = json.optString("tunnel_domain").takeIf { it.isNotEmpty() },
                resolverIp = json.optString("resolver_ip").takeIf { it.isNotEmpty() },
                mtu = json.optInt("mtu", 1400),
                noiseInjection = json.optBoolean("noise_injection", true),
                isBuiltin = json.optBoolean("is_builtin", false),
                pingMs = json.optInt("ping_ms", -1)
            )
        }
    }
}
