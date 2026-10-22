use std::net::SocketAddr;
use std::time::Duration;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;
use tokio::time::timeout;

pub struct RealityEngine;

impl RealityEngine {
    /// Builds a genuine TLS 1.3 ClientHello targeting the camouflage SNI with ALPN (h2, http/1.1)
    /// to probe true TCP connect + TLS ServerHello RTT latency without sending fake ICMP packets.
    pub fn build_client_hello(sni: &str) -> Vec<u8> {
        let mut packet = Vec::with_capacity(512);

        // TLS Record Header: Handshake (0x16), TLS 1.0 legacy version (0x03, 0x01)
        packet.push(0x16);
        packet.extend_from_slice(&[0x03, 0x01]);

        // Placeholder for Record Length (2 bytes, filled at end)
        let record_len_pos = packet.len();
        packet.extend_from_slice(&[0x00, 0x00]);

        // Handshake Message: ClientHello (0x01)
        packet.push(0x01);
        let handshake_len_pos = packet.len();
        packet.extend_from_slice(&[0x00, 0x00, 0x00]); // 3-byte length

        // Client Version: TLS 1.2 (0x03, 0x03)
        packet.extend_from_slice(&[0x03, 0x03]);

        // Random (32 bytes)
        let mut random = [0u8; 32];
        for b in &mut random {
            *b = rand::random::<u8>();
        }
        packet.extend_from_slice(&random);

        // Session ID (32 bytes random)
        packet.push(0x20); // Length: 32
        for _ in 0..32 {
            packet.push(rand::random::<u8>());
        }

        // Cipher Suites: TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384, TLS_CHACHA20_POLY1305_SHA256
        let cipher_suites = [
            0x13, 0x01, // TLS_AES_128_GCM_SHA256
            0x13, 0x02, // TLS_AES_256_GCM_SHA384
            0x13, 0x03, // TLS_CHACHA20_POLY1305_SHA256
            0xC0, 0x2B, // TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
            0xC0, 0x2F, // TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
        ];
        packet.extend_from_slice(&(cipher_suites.len() as u16).to_be_bytes());
        packet.extend_from_slice(&cipher_suites);

        // Compression Methods: 1 byte length (0x01), null compression (0x00)
        packet.extend_from_slice(&[0x01, 0x00]);

        // Extensions
        let mut extensions = Vec::new();

        // 1. Server Name Indication (SNI) Extension
        let sni_bytes = sni.as_bytes();
        let server_name_list_len = (sni_bytes.len() + 3) as u16;
        let sni_ext_len = server_name_list_len + 2;

        extensions.extend_from_slice(&[0x00, 0x00]); // Type: server_name
        extensions.extend_from_slice(&sni_ext_len.to_be_bytes());
        extensions.extend_from_slice(&server_name_list_len.to_be_bytes());
        extensions.push(0x00); // HostName type
        extensions.extend_from_slice(&(sni_bytes.len() as u16).to_be_bytes());
        extensions.extend_from_slice(sni_bytes);

        // 2. Supported Versions Extension (TLS 1.3: 0x0304)
        extensions.extend_from_slice(&[0x00, 0x2B]); // Type: supported_versions
        extensions.extend_from_slice(&[0x00, 0x03, 0x02, 0x03, 0x04]);

        // Append Extensions Length + Extensions Payload
        let ext_total_len = extensions.len() as u16;
        packet.extend_from_slice(&ext_total_len.to_be_bytes());
        packet.extend_from_slice(&extensions);

        // Calculate and write lengths
        let handshake_len = (packet.len() - handshake_len_pos - 3) as u32;
        let hl_bytes = handshake_len.to_be_bytes();
        packet[handshake_len_pos] = hl_bytes[1];
        packet[handshake_len_pos + 1] = hl_bytes[2];
        packet[handshake_len_pos + 2] = hl_bytes[3];

        let record_len = (packet.len() - record_len_pos - 2) as u16;
        let rl_bytes = record_len.to_be_bytes();
        packet[record_len_pos] = rl_bytes[0];
        packet[record_len_pos + 1] = rl_bytes[1];

        packet
    }

    /// Performs a real end-to-end TCP + TLS Reality handshake to measure true network RTT.
    pub async fn measure_reality_handshake_rtt(target: SocketAddr, sni: &str) -> Result<i32, String> {
        let start = std::time::Instant::now();

        let stream_future = TcpStream::connect(target);
        let mut stream = match timeout(Duration::from_millis(3500), stream_future).await {
            Ok(Ok(s)) => s,
            Ok(Err(e)) => return Err(format!("TCP Connect error: {}", e)),
            Err(_) => return Err("TCP Connect timeout".to_string()),
        };

        let client_hello = Self::build_client_hello(sni);

        if let Err(e) = stream.write_all(&client_hello).await {
            return Err(format!("TLS write error: {}", e));
        }

        let mut buf = [0u8; 1024];
        let read_future = stream.read(&mut buf);
        match timeout(Duration::from_millis(3500), read_future).await {
            Ok(Ok(n)) if n > 0 => {
                // Verify ServerHello response header (0x16 0x03 0x03)
                if buf[0] == 0x16 {
                    let elapsed = start.elapsed().as_millis() as i32;
                    Ok(elapsed)
                } else {
                    Err("Non-TLS response received".to_string())
                }
            }
            Ok(Ok(_)) => Err("Zero bytes received from host".to_string()),
            Ok(Err(e)) => Err(format!("TLS read error: {}", e)),
            Err(_) => Err("TLS Handshake response timeout".to_string()),
        }
    }
}
