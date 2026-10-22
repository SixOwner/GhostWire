use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use url::Url;

/// Unified runtime profile used across GhostWire's native networking pipeline.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RuntimeProfile {
    pub id: String,
    pub name: String,
    pub protocol: String,
    pub server_address: String,
    pub server_port: u16,
    pub uuid: Option<String>,
    pub public_key: Option<String>,
    pub short_id: Option<String>,
    pub sni: Option<String>,
    pub tunnel_domain: Option<String>,
    pub resolver_ip: Option<String>,
    pub mtu: u16,
    pub noise_injection: bool,
    pub is_builtin: bool,
    pub ping_ms: i32,
}

/// SlipNet exported configuration schema for cross-compatibility.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SlipNetConfig {
    #[serde(rename = "type")]
    pub protocol_type: String, // dnstt, slipstream_quic, noizdns, vaydns, vless_ws, naiveproxy
    pub name: Option<String>,
    pub host: Option<String>,
    pub port: Option<u16>,
    pub uuid: Option<String>,
    pub public_key: Option<String>,
    pub domain: Option<String>,
    pub sni: Option<String>,
    pub resolver: Option<String>,
    pub mtu: Option<u16>,
    pub kcp_noise: Option<bool>,
}

pub struct UniversalImporter;

impl UniversalImporter {
    /// Parses any supported URI scheme or JSON configuration into a RuntimeProfile.
    pub fn parse(input: &str) -> Result<RuntimeProfile, String> {
        let trimmed = input.trim();

        if trimmed.starts_with('{') {
            Self::parse_json(trimmed)
        } else if trimmed.starts_with("ghostwire://") {
            Self::parse_ghostwire_uri(trimmed)
        } else if trimmed.starts_with("vless://") {
            Self::parse_vless_uri(trimmed)
        } else if trimmed.starts_with("dnstt://") {
            Self::parse_dnstt_uri(trimmed)
        } else {
            Err("Unsupported URI scheme or configuration structure".to_string())
        }
    }

    /// Parses raw JSON configuration (either GhostWire native format or SlipNet export).
    fn parse_json(json_str: &str) -> Result<RuntimeProfile, String> {
        if let Ok(profile) = serde_json::from_str::<RuntimeProfile>(json_str) {
            return Ok(profile);
        }

        if let Ok(slipnet) = serde_json::from_str::<SlipNetConfig>(json_str) {
            return Ok(RuntimeProfile {
                id: format!("slipnet_{}", rand::random::<u32>()),
                name: slipnet.name.unwrap_or_else(|| format!("SlipNet ({})", slipnet.protocol_type)),
                protocol: match slipnet.protocol_type.as_str() {
                    "slipstream_quic" => "GhostStream".to_string(),
                    "noizdns" | "vaydns" => "GhostShade".to_string(),
                    "dnstt" => "GhostDNS Core".to_string(),
                    "vless_ws" | "reality" => "WireReality".to_string(),
                    _ => "GhostDNS Core".to_string(),
                },
                server_address: slipnet.host.unwrap_or_else(|| "127.0.0.1".to_string()),
                server_port: slipnet.port.unwrap_or(53),
                uuid: slipnet.uuid,
                public_key: slipnet.public_key,
                short_id: None,
                sni: slipnet.sni,
                tunnel_domain: slipnet.domain,
                resolver_ip: slipnet.resolver,
                mtu: slipnet.mtu.unwrap_or(1232),
                noise_injection: slipnet.kcp_noise.unwrap_or(true),
                is_builtin: false,
                ping_ms: -1,
            });
        }

        Err("Failed to parse JSON configuration".to_string())
    }

    /// Parses ghostwire:// native URI:
    /// ghostwire://{uuid}@{host}:{port}?reality_pk={pk}&reality_sid={sid}&sni={sni}&dnstt_pk={dpk}&dnstt_domain={dom}&dnstt_ip={dip}#{name}
    fn parse_ghostwire_uri(uri_str: &str) -> Result<RuntimeProfile, String> {
        let parsed = Url::parse(uri_str).map_err(|e| e.to_string())?;
        let host = parsed.host_str().ok_or("Missing host")?.to_string();
        let port = parsed.port().unwrap_or(443);
        let uuid = parsed.username().to_string();
        let query_pairs: HashMap<String, String> = parsed.query_pairs().into_owned().collect();
        let name = parsed.fragment().unwrap_or("GhostWire Custom Node").to_string();

        Ok(RuntimeProfile {
            id: format!("gw_{}", rand::random::<u32>()),
            name,
            protocol: "WireReality".to_string(),
            server_address: host,
            server_port: port,
            uuid: if uuid.is_empty() { None } else { Some(uuid) },
            public_key: query_pairs.get("reality_pk").cloned(),
            short_id: query_pairs.get("reality_sid").cloned(),
            sni: query_pairs.get("sni").cloned(),
            tunnel_domain: query_pairs.get("dnstt_domain").cloned(),
            resolver_ip: query_pairs.get("resolver_ip").cloned(),
            mtu: 1400,
            noise_injection: true,
            is_builtin: false,
            ping_ms: -1,
        })
    }

    /// Parses vless:// standard URI.
    fn parse_vless_uri(uri_str: &str) -> Result<RuntimeProfile, String> {
        let parsed = Url::parse(uri_str).map_err(|e| e.to_string())?;
        let host = parsed.host_str().ok_or("Missing host")?.to_string();
        let port = parsed.port().unwrap_or(443);
        let uuid = parsed.username().to_string();
        let query: HashMap<String, String> = parsed.query_pairs().into_owned().collect();
        let name = parsed.fragment().unwrap_or("VLESS Reality Node").to_string();

        Ok(RuntimeProfile {
            id: format!("vless_{}", rand::random::<u32>()),
            name,
            protocol: "WireReality".to_string(),
            server_address: host,
            server_port: port,
            uuid: if uuid.is_empty() { None } else { Some(uuid) },
            public_key: query.get("pbk").cloned(),
            short_id: query.get("sid").cloned(),
            sni: query.get("sni").cloned(),
            tunnel_domain: None,
            resolver_ip: None,
            mtu: 1400,
            noise_injection: false,
            is_builtin: false,
            ping_ms: -1,
        })
    }

    /// Parses dnstt:// URI: dnstt://{pubkey}@{domain}:{port}?server={server_ip}#{name}
    fn parse_dnstt_uri(uri_str: &str) -> Result<RuntimeProfile, String> {
        let parsed = Url::parse(uri_str).map_err(|e| e.to_string())?;
        let domain = parsed.host_str().ok_or("Missing domain")?.to_string();
        let port = parsed.port().unwrap_or(53);
        let pubkey = parsed.username().to_string();
        let query: HashMap<String, String> = parsed.query_pairs().into_owned().collect();
        let server_ip = query.get("server").cloned().unwrap_or_else(|| domain.clone());
        let name = parsed.fragment().unwrap_or("DNSTT Core Node").to_string();

        Ok(RuntimeProfile {
            id: format!("dnstt_{}", rand::random::<u32>()),
            name,
            protocol: "GhostDNS Core".to_string(),
            server_address: server_ip,
            server_port: port,
            uuid: None,
            public_key: if pubkey.is_empty() { None } else { Some(pubkey) },
            short_id: None,
            sni: None,
            tunnel_domain: Some(domain),
            resolver_ip: query.get("resolver").cloned(),
            mtu: 1232,
            noise_injection: true,
            is_builtin: false,
            ping_ms: -1,
        })
    }
}
