use crate::protocols::dnstt::DnsttClient;
use crate::protocols::reality::RealityEngine;
use crate::protocols::slipnet::RuntimeProfile;
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::Semaphore;

pub struct LatencyEngine;

impl LatencyEngine {
    /// Dispatches genuine protocol-level latency measurement according to profile type.
    pub async fn measure_profile_latency(profile: &RuntimeProfile) -> i32 {
        let host = &profile.server_address;
        let port = profile.server_port;

        match profile.protocol.as_str() {
            "WireReality" => {
                let target_addr: SocketAddr = match format!("{}:{}", host, port).parse() {
                    Ok(a) => a,
                    Err(_) => return -1,
                };
                let sni = profile.sni.as_deref().unwrap_or("gateway.icloud.com");
                RealityEngine::measure_reality_handshake_rtt(target_addr, sni)
                    .await
                    .unwrap_or(-1)
            }
            "GhostDNS Core" | "GhostStream" | "GhostShade" => {
                let creds = crate::crypto::obfuscation::CloakedNodeCredentials::load_primary_node();
                let resolver_ip = profile.resolver_ip.as_deref().unwrap_or("10.201.34.14");
                let resolver_addr: SocketAddr = match format!("{}:53", resolver_ip).parse() {
                    Ok(a) => a,
                    Err(_) => return -1,
                };
                let domain = profile.tunnel_domain.as_deref().unwrap_or(&creds.dnstt_domain);
                let pubkey = profile.public_key.as_deref().unwrap_or(&creds.dnstt_pubkey);
                DnsttClient::measure_dns_tunnel_rtt(resolver_addr, domain, pubkey)
                    .await
                    .unwrap_or(-1)
            }
            _ => -1,
        }
    }

    /// Concurrently tests all servers in the pool using non-blocking worker tasks.
    pub async fn test_all_servers(profiles: Vec<RuntimeProfile>) -> Vec<RuntimeProfile> {
        let semaphore = Arc::new(Semaphore::new(8)); // Max 8 concurrent probe workers
        let mut handles = Vec::new();

        for mut profile in profiles {
            let permit = semaphore.clone().acquire_owned().await.unwrap();
            let handle = tokio::spawn(async move {
                let _permit = permit;
                let rtt = Self::measure_profile_latency(&profile).await;
                profile.ping_ms = rtt;
                profile
            });
            handles.push(handle);
        }

        let mut results = Vec::new();
        for handle in handles {
            if let Ok(profile) = handle.await {
                results.push(profile);
            }
        }

        // Sort: operational nodes first, sorted by ascending true latency
        results.sort_by(|a, b| {
            let a_val = if a.ping_ms < 0 { 99999 } else { a.ping_ms };
            let b_val = if b.ping_ms < 0 { 99999 } else { b.ping_ms };
            a_val.cmp(&b_val)
        });

        results
    }
}
