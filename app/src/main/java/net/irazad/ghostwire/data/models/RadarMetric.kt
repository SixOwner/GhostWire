package net.irazad.ghostwire.data.models

import org.json.JSONObject

data class RadarMetric(
    val id: String,
    val name: String,
    val isp: String,
    val ip: String,
    val port: Int,
    val asn: String,
    val ednsBufferSupported: Int = 4096,
    val antiPoisonVerified: Boolean = true,
    val priority: Int = 1,
    val rttMs: Int = -1,
    val packetLossPercent: Int = 0,
    val isHealthy: Boolean = true,
    val cidrRange: String = "",
    val targetType: String = "DNS" // DNS or CLEAN_IP
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("isp", isp)
            put("ip", ip)
            put("port", port)
            put("asn", asn)
            put("edns_buffer_supported", ednsBufferSupported)
            put("anti_poison_verified", antiPoisonVerified)
            put("priority", priority)
            put("rtt_ms", rttMs)
            put("packet_loss_percent", packetLossPercent)
            put("is_healthy", isHealthy)
            put("cidr_range", cidrRange)
            put("target_type", targetType)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): RadarMetric {
            return RadarMetric(
                id = json.optString("id", "rm_${System.currentTimeMillis()}"),
                name = json.optString("name", "Scanner Node"),
                isp = json.optString("isp", "Domestic"),
                ip = json.optString("ip", "10.201.34.14"),
                port = json.optInt("port", 53),
                asn = json.optString("asn", "AS197207"),
                ednsBufferSupported = json.optInt("edns_buffer_supported", 4096),
                antiPoisonVerified = json.optBoolean("anti_poison_verified", true),
                priority = json.optInt("priority", 1),
                rttMs = json.optInt("rtt_ms", -1),
                packetLossPercent = json.optInt("packet_loss_percent", 0),
                isHealthy = json.optBoolean("is_healthy", true),
                cidrRange = json.optString("cidr_range", ""),
                targetType = json.optString("target_type", "DNS")
            )
        }
    }
}
