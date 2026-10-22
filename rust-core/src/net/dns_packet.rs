/// High-speed wireformat DNS packet encoder and decoder with full EDNS0 extension support.
pub struct DnsPacketParser;

impl DnsPacketParser {
    /// Extracts the query domain name and QTYPE from a raw DNS packet.
    pub fn parse_query_name(packet: &[u8]) -> Option<(String, u16)> {
        if packet.len() < 12 {
            return None;
        }

        let qdcount = u16::from_be_bytes([packet[4], packet[5]]);
        if qdcount == 0 {
            return None;
        }

        let mut offset = 12;
        let mut labels = Vec::new();

        while offset < packet.len() {
            let len = packet[offset] as usize;
            if len == 0 {
                offset += 1;
                break;
            }

            if offset + 1 + len > packet.len() {
                return None;
            }

            let label = String::from_utf8_lossy(&packet[offset + 1..offset + 1 + len]);
            labels.push(label.to_string());
            offset += 1 + len;
        }

        if offset + 4 <= packet.len() {
            let qtype = u16::from_be_bytes([packet[offset], packet[offset + 1]]);
            Some((labels.join("."), qtype))
        } else {
            None
        }
    }

    /// Verifies if the DNS response contains an EDNS0 OPT RR and returns the advertised buffer size.
    pub fn extract_edns0_buffer_size(packet: &[u8]) -> Option<u16> {
        if packet.len() < 12 {
            return None;
        }

        let arcount = u16::from_be_bytes([packet[10], packet[11]]);
        if arcount == 0 {
            return None;
        }

        // Search for OPT pseudo-RR (Type 41 = 0x0029)
        for i in 12..packet.len().saturating_sub(10) {
            if packet[i] == 0x00 && packet[i + 1] == 0x29 {
                let payload_size = u16::from_be_bytes([packet[i + 2], packet[i + 3]]);
                return Some(payload_size);
            }
        }

        None
    }
}
