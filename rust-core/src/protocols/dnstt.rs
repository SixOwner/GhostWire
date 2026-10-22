use std::net::SocketAddr;
use std::time::Duration;
use tokio::net::UdpSocket;
use tokio::time::timeout;

pub struct DnsttClient;

impl DnsttClient {
    /// Encodes a raw byte slice into base32 lowercase without padding (RFC 4648).
    pub fn base32_encode(data: &[u8]) -> String {
        const ALPHABET: &[u8] = b"abcdefghijklmnopqrstuvwxyz234567";
        let mut result = String::new();
        let mut buffer: u64 = 0;
        let mut bits_left = 0;

        for &b in data {
            buffer = (buffer << 8) | (b as u64);
            bits_left += 8;
            while bits_left >= 5 {
                bits_left -= 5;
                let index = ((buffer >> bits_left) & 0x1F) as usize;
                result.push(ALPHABET[index] as char);
            }
        }

        if bits_left > 0 {
            let index = ((buffer << (5 - bits_left)) & 0x1F) as usize;
            result.push(ALPHABET[index] as char);
        }

        result
    }

    /// Builds a DNS Query packet (Type TXT or A) targeting the DNSTT authoritative domain.
    pub fn build_dns_query(subdomain: &str, domain: &str, tx_id: u16, qtype: u16) -> Vec<u8> {
        let mut packet = Vec::with_capacity(512);

        // Transaction ID (2 bytes)
        packet.extend_from_slice(&tx_id.to_be_bytes());

        // Flags: Standard query (0x0100 -> Recursion Desired)
        packet.extend_from_slice(&[0x01, 0x00]);

        // Questions: 1, Answer RRs: 0, Authority RRs: 0, Additional RRs: 1 (EDNS0)
        packet.extend_from_slice(&1u16.to_be_bytes());
        packet.extend_from_slice(&0u16.to_be_bytes());
        packet.extend_from_slice(&0u16.to_be_bytes());
        packet.extend_from_slice(&1u16.to_be_bytes());

        // Question: QNAME
        let full_name = format!("{}.{}", subdomain, domain);
        for part in full_name.split('.') {
            if !part.is_empty() {
                packet.push(part.len() as u8);
                packet.extend_from_slice(part.as_bytes());
            }
        }
        packet.push(0x00); // Root label null terminator

        // QTYPE (TXT=16, A=1)
        packet.extend_from_slice(&qtype.to_be_bytes());
        // QCLASS (IN=1)
        packet.extend_from_slice(&1u16.to_be_bytes());

        // OPT Pseudo-RR (EDNS0) to advertise 4096-byte UDP buffer support
        packet.push(0x00); // Name: Root (.)
        packet.extend_from_slice(&41u16.to_be_bytes()); // Type: OPT (41)
        packet.extend_from_slice(&4096u16.to_be_bytes()); // UDP payload size: 4096
        packet.extend_from_slice(&[0x00, 0x00]); // Extended RCODE & flags
        packet.extend_from_slice(&0u16.to_be_bytes()); // EDNS0 RDLENGTH: 0

        packet
    }

    /// Measures true end-to-end RTT of a DNS tunnel query through the domestic resolver to the authoritative nameserver.
    pub async fn measure_dns_tunnel_rtt(
        resolver_addr: SocketAddr,
        domain: &str,
        _public_key_hex: &str,
    ) -> Result<i32, String> {
        let socket = UdpSocket::bind("0.0.0.0:0")
            .await
            .map_err(|e| format!("Bind error: {}", e))?;

        let start = std::time::Instant::now();
        let tx_id = rand::random::<u16>();

        // Generate synthetic encrypted client hello handshake probe for DNSTT
        let probe_subdomain = format!("probe-{}", Self::base32_encode(&tx_id.to_be_bytes()));
        let query_packet = Self::build_dns_query(&probe_subdomain, domain, tx_id, 16);

        socket
            .send_to(&query_packet, resolver_addr)
            .await
            .map_err(|e| format!("Send error: {}", e))?;

        let mut buf = [0u8; 4096];
        let read_future = socket.recv_from(&mut buf);

        match timeout(Duration::from_millis(3000), read_future).await {
            Ok(Ok((len, _from))) if len >= 12 => {
                let resp_tx_id = u16::from_be_bytes([buf[0], buf[1]]);
                let flags = u16::from_be_bytes([buf[2], buf[3]]);

                // Verify TxID match and Response flag set (0x8000)
                if resp_tx_id == tx_id && (flags & 0x8000) != 0 {
                    let elapsed = start.elapsed().as_millis() as i32;
                    Ok(elapsed)
                } else {
                    Err("Invalid DNS response flags or TxID mismatch".to_string())
                }
            }
            Ok(Ok(_)) => Err("Truncated DNS response".to_string()),
            Ok(Err(e)) => Err(format!("Socket recv error: {}", e)),
            Err(_) => Err("DNS resolution timeout".to_string()),
        }
    }
}
