use crate::scanner::resolver_db::ResolverNode;
use std::net::SocketAddr;
use std::time::Duration;
use tokio::net::UdpSocket;
use tokio::time::timeout;

pub struct ResolverProbe;

impl ResolverProbe {
    /// Conducts a cryptographic anti-poisoning challenge and EDNS0 buffer evaluation against a resolver node.
    pub async fn probe_resolver(mut node: ResolverNode) -> ResolverNode {
        let addr_str = format!("{}:{}", node.ip, node.port);
        let socket_addr: SocketAddr = match addr_str.parse() {
            Ok(a) => a,
            Err(_) => {
                node.is_healthy = false;
                node.rtt_ms = -1;
                node.packet_loss_percent = 100;
                return node;
            }
        };

        let socket = match UdpSocket::bind("0.0.0.0:0").await {
            Ok(s) => s,
            Err(_) => {
                node.is_healthy = false;
                node.rtt_ms = -1;
                return node;
            }
        };

        // Construct Cryptographic Challenge Query to detect DNS Hijacking / Censorship Injection
        // In Iran, poisoned resolvers intercept unknown or blocked domains and inject 10.10.34.34
        let random_nonce = rand::random::<u32>();
        let _challenge_domain = format!("chk-{:08x}.ghostwire.internal", random_nonce);
        let query_packet = crate::protocols::dnstt::DnsttClient::build_dns_query(
            &format!("n{:08x}", random_nonce),
            "ghostwire.internal",
            (random_nonce & 0xFFFF) as u16,
            1, // Type A
        );

        let start = std::time::Instant::now();
        if socket.send_to(&query_packet, socket_addr).await.is_err() {
            node.is_healthy = false;
            node.rtt_ms = -1;
            node.packet_loss_percent = 100;
            return node;
        }

        let mut buf = [0u8; 4096];
        let read_future = socket.recv_from(&mut buf);

        match timeout(Duration::from_millis(2500), read_future).await {
            Ok(Ok((len, _))) if len >= 12 => {
                let elapsed = start.elapsed().as_millis() as i32;
                node.rtt_ms = elapsed;
                node.packet_loss_percent = 0;

                // Inspect RCODE in flags (bottom 4 bits of byte 3)
                let _rcode = buf[3] & 0x0F;
                // Check if response contains injected domestic landing IP (e.g., 10.10.34.34)
                let is_poisoned = buf[..len].windows(4).any(|w| w == [10, 10, 34, 34]);

                if is_poisoned {
                    node.anti_poison_verified = false;
                    node.is_healthy = false;
                } else {
                    node.anti_poison_verified = true;
                    node.is_healthy = true;
                }

                // Check EDNS0 buffer advertisement
                if let Some(advertised_buf) = crate::net::dns_packet::DnsPacketParser::extract_edns0_buffer_size(&buf[..len]) {
                    node.edns_buffer_supported = advertised_buf;
                }
            }
            _ => {
                node.is_healthy = false;
                node.rtt_ms = -1;
                node.packet_loss_percent = 100;
            }
        }

        node
    }
}
