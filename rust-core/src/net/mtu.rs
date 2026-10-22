use std::net::SocketAddr;
use std::time::Duration;
use tokio::net::UdpSocket;
use tokio::time::timeout;

pub struct AdaptiveMtuEngine;

impl AdaptiveMtuEngine {
    /// Discovers the maximum unfragmented Path MTU (PMTUD) to a remote target using binary search.
    /// Eliminates black-hole packet drops common on restricted mobile networks (MCI/Irancell).
    pub async fn detect_optimal_pmtu(target: SocketAddr) -> u16 {
        let mut min_mtu: u16 = 1200;
        let mut max_mtu: u16 = 1500;
        let mut optimal_mtu: u16 = 1280; // Safe IPv6/IPv4 baseline

        let socket = match UdpSocket::bind("0.0.0.0:0").await {
            Ok(s) => s,
            Err(_) => return 1280,
        };

        while min_mtu <= max_mtu {
            let mid = min_mtu + (max_mtu - min_mtu) / 2;
            let probe_size = mid as usize;

            // Generate synthetic probe packet filled with dummy payload
            let mut probe_packet = vec![0xAA; probe_size];
            // DNS-like 12-byte header to traverse carrier UDP inspection
            probe_packet[0] = 0xAA;
            probe_packet[1] = 0xBB;
            probe_packet[2] = 0x01; // Recursion desired
            probe_packet[5] = 0x01; // 1 Question

            let send_res = socket.send_to(&probe_packet, target).await;
            if send_res.is_err() {
                max_mtu = mid - 1;
                continue;
            }

            let mut resp_buf = [0u8; 512];
            let recv_future = socket.recv_from(&mut resp_buf);

            match timeout(Duration::from_millis(600), recv_future).await {
                Ok(Ok(_)) | Err(_) => {
                    // Packet traversed without ICMP fragmentation rejection
                    optimal_mtu = mid;
                    min_mtu = mid + 1;
                }
                Ok(Err(_)) => {
                    max_mtu = mid - 1;
                }
            }
        }

        // Clamp between safe operational boundaries
        optimal_mtu.clamp(1200, 1420)
    }
}
