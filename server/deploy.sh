#!/usr/bin/env bash
# ==============================================================================
# GhostWire Personal Server Automated Deployment Pipeline
# Dual-Engine Censorship-Bypass Protocol (WireReality + GhostDNS Noise Tunnel)
# Author: SixOwner | Network: ایران آزاد (https://t.me/irazadnet)
# ==============================================================================
set -euo pipefail

# ANSI Color Palette
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

# Terminal Banner
clear
echo -e "${CYAN}${BOLD}"
cat << "EOF"
   ______ __                 __  _       __ _           
  / ____// /_   ____   _____/ /_| |     / /(_)_____ ___ 
 / / __ / __ \ / __ \ / ___/ __/| | /| / // // ___// _ \
/ /_/ // / / // /_/ /(__  )/ /_  | |/ |/ // // /   /  __/
\____//_/ /_/ \____//____/ \__/  |__/|__//_//_/    \___/ 
             Autonomous Anti-Censorship Mesh Core
             Developed by SixOwner | ایران آزاد
EOF
echo -e "${NC}"

# Root verification
if [[ $EUID -ne 0 ]]; then
   echo -e "${RED}[ERROR] This installer must be executed as root (sudo bash $0).${NC}"
   exit 1
fi

echo -e "${PURPLE}==> [1/6] Detecting Environment & Network Configuration...${NC}"

# Detect Public IP
SERVER_IP=$(curl -4 -s https://api.ipify.org || curl -4 -s https://ifconfig.me || curl -4 -s https://icanhazip.com || echo "")
if [[ -z "$SERVER_IP" ]]; then
    read -rp "Could not auto-detect public IPv4. Please enter server IP: " SERVER_IP
fi
echo -e "    ${GREEN}✓ Detected Server IPv4:${NC} ${BOLD}${SERVER_IP}${NC}"

# Interactive Domain Input
echo ""
echo -e "${CYAN}${BOLD}--- DNS Infrastructure Configuration ---${NC}"
echo -e "To route DNS queries through national censorship firewalls,"
echo -e "you need an NS record pointing to an A record (e.g. t.yourdomain.com -> ns.yourdomain.com -> ${SERVER_IP})."
echo ""

if [[ -n "${1:-}" ]]; then
    TUNNEL_DOMAIN="$1"
else
    read -rp "Enter your DNS Tunnel Subdomain (e.g., t.nteam.bond) [Default: t.nteam.bond]: " INPUT_DOMAIN
    TUNNEL_DOMAIN="${INPUT_DOMAIN:-t.nteam.bond}"
fi
echo -e "    ${GREEN}✓ Tunnel Base Domain:${NC} ${BOLD}${TUNNEL_DOMAIN}${NC}"

# Reality Camouflage SNI
CAMOUFLAGE_SNI="gateway.icloud.com"

# Prepare Directories
CONFIG_DIR="/etc/ghostwire"
mkdir -p "${CONFIG_DIR}"
chmod 700 "${CONFIG_DIR}"

# 2. Kernel & Network Optimization
echo ""
echo -e "${PURPLE}==> [2/6] Hardening Kernel & Freeing Port 53...${NC}"

# Enable IPv4 Forwarding and BBR
cat << 'EOF' > /etc/sysctl.d/99-ghostwire.conf
net.ipv4.ip_forward=1
net.core.default_qdisc=fq
net.ipv4.tcp_congestion_control=bbr
net.core.rmem_max=16777216
net.core.wmem_max=16777216
net.ipv4.tcp_rmem=4096 87380 16777216
net.ipv4.tcp_wmem=4096 65536 16777216
net.ipv4.tcp_fastopen=3
net.ipv4.tcp_mtu_probing=1
EOF
sysctl --system > /dev/null 2>&1 || true

# Free Port 53 from systemd-resolved DNSStubListener
if [ -f /etc/systemd/resolved.conf ]; then
    sed -i 's/^#DNSStubListener=.*/DNSStubListener=no/' /etc/systemd/resolved.conf
    sed -i 's/^DNSStubListener=.*/DNSStubListener=no/' /etc/systemd/resolved.conf
    if ! grep -q "DNSStubListener=no" /etc/systemd/resolved.conf; then
        echo "DNSStubListener=no" >> /etc/systemd/resolved.conf
    fi
fi

# Ensure reliable public resolver for the host itself
rm -f /etc/resolv.conf
cat << 'EOF' > /etc/resolv.conf
nameserver 1.1.1.1
nameserver 8.8.8.8
nameserver 9.9.9.9
EOF
systemctl restart systemd-resolved > /dev/null 2>&1 || true

# 3. Installing Dependencies
echo ""
echo -e "${PURPLE}==> [3/6] Installing Core Build Dependencies & Runtimes...${NC}"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y -q
apt-get install -y -q --no-install-recommends \
    curl \
    tar \
    git \
    golang \
    build-essential \
    libcap2-bin \
    net-tools \
    iptables \
    ca-certificates

# 4. Installing Sing-Box (VLESS + Reality Core)
echo ""
echo -e "${PURPLE}==> [4/6] Deploying Sing-Box (WireReality XTLS Core)...${NC}"
SINGBOX_VER="1.14.3"
ARCH=$(dpkg --print-architecture)
case "$ARCH" in
    amd64) SB_ARCH="linux-amd64" ;;
    arm64) SB_ARCH="linux-arm64" ;;
    *) SB_ARCH="linux-amd64" ;;
esac

TMP_DIR=$(mktemp -d)
curl -fsSL "https://github.com/SagerNet/sing-box/releases/download/v${SINGBOX_VER}/sing-box-${SINGBOX_VER}-${SB_ARCH}.tar.gz" -o "${TMP_DIR}/sb.tar.gz"
tar -xzf "${TMP_DIR}/sb.tar.gz" -C "${TMP_DIR}"
install -m 755 "${TMP_DIR}/sing-box-${SINGBOX_VER}-${SB_ARCH}/sing-box" /usr/local/bin/sing-box
rm -rf "${TMP_DIR}"

# Generate Reality Keys
KEYPAIR=$(/usr/local/bin/sing-box generate reality-keypair)
REALITY_PRIVKEY=$(echo "$KEYPAIR" | grep -i "PrivateKey:" | awk '{print $2}')
REALITY_PUBKEY=$(echo "$KEYPAIR" | grep -i "PublicKey:" | awk '{print $2}')
REALITY_UUID=$(/usr/local/bin/sing-box generate uuid)
REALITY_SHORT_ID=$(/usr/local/bin/sing-box generate rand --hex 8)

# Write Sing-Box Configuration
cat << EOF > "${CONFIG_DIR}/sing-box.json"
{
  "log": {
    "level": "warn",
    "timestamp": true
  },
  "inbounds": [
    {
      "type": "vless",
      "tag": "vless-reality-in",
      "listen": "::",
      "listen_port": 443,
      "users": [
        {
          "uuid": "${REALITY_UUID}",
          "flow": "xtls-rprx-vision"
        }
      ],
      "tls": {
        "enabled": true,
        "server_name": "${CAMOUFLAGE_SNI}",
        "reality": {
          "enabled": true,
          "handshake": {
            "server": "${CAMOUFLAGE_SNI}",
            "server_port": 443
          },
          "private_key": "${REALITY_PRIVKEY}",
          "short_id": [
            "${REALITY_SHORT_ID}"
          ]
        }
      }
    },
    {
      "type": "mixed",
      "tag": "dnstt-ingress-in",
      "listen": "127.0.0.1",
      "listen_port": 8000
    }
  ],
  "outbounds": [
    {
      "type": "direct",
      "tag": "direct"
    }
  ]
}
EOF

# Setup Sing-box Systemd Service
cat << 'EOF' > /etc/systemd/system/sing-box.service
[Unit]
Description=GhostWire Sing-Box Reality Service
After=network.target nss-lookup.target

[Service]
Type=simple
User=root
CapabilityBoundingSet=CAP_NET_ADMIN CAP_NET_BIND_SERVICE CAP_NET_RAW
AmbientCapabilities=CAP_NET_ADMIN CAP_NET_BIND_SERVICE CAP_NET_RAW
ExecStart=/usr/local/bin/sing-box run -c /etc/ghostwire/sing-box.json
Restart=always
RestartSec=3
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
EOF

# 5. Installing DNSTT-Server (Encrypted DNS Tunnel)
echo ""
echo -e "${PURPLE}==> [5/6] Building & Configuring DNSTT Noise Core...${NC}"
DNSTT_DIR="/opt/ghostwire-dnstt"
rm -rf "${DNSTT_DIR}"
git clone https://www.bamsoftware.com/git/dnstt.git "${DNSTT_DIR}"
(cd "${DNSTT_DIR}/dnstt-server" && CGO_ENABLED=0 go build -v -ldflags="-s -w" -o /usr/local/bin/dnstt-server)
setcap 'cap_net_bind_service=+ep' /usr/local/bin/dnstt-server

# Generate DNSTT Noise Keys
/usr/local/bin/dnstt-server -gen-key -privkey-file "${CONFIG_DIR}/dnstt.key" -pubkey-file "${CONFIG_DIR}/dnstt.pub"
chmod 400 "${CONFIG_DIR}/dnstt.key"
chmod 644 "${CONFIG_DIR}/dnstt.pub"
DNSTT_PUBKEY=$(cat "${CONFIG_DIR}/dnstt.pub" | tr -d '[:space:]')

# Setup DNSTT Systemd Service
cat << EOF > /etc/systemd/system/dnstt-server.service
[Unit]
Description=GhostWire DNSTT Tunnel Server
After=network.target sing-box.service

[Service]
Type=simple
User=root
CapabilityBoundingSet=CAP_NET_BIND_SERVICE
AmbientCapabilities=CAP_NET_BIND_SERVICE
ExecStart=/usr/local/bin/dnstt-server -udp :53 -privkey-file ${CONFIG_DIR}/dnstt.key ${TUNNEL_DOMAIN} 127.0.0.1:8000
Restart=always
RestartSec=3
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
EOF

# Reload & Start Daemons
echo ""
echo -e "${PURPLE}==> [6/6] Starting GhostWire Daemons...${NC}"
systemctl daemon-reload
systemctl enable sing-box dnstt-server > /dev/null 2>&1
systemctl restart sing-box
systemctl restart dnstt-server

# Generate URIs
VLESS_URI="vless://${REALITY_UUID}@${SERVER_IP}:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=${CAMOUFLAGE_SNI}&fp=chrome&pbk=${REALITY_PUBKEY}&sid=${REALITY_SHORT_ID}&type=tcp#GhostWire-Reality-${SERVER_IP}"
DNSTT_URI="dnstt://${DNSTT_PUBKEY}@${TUNNEL_DOMAIN}:53?server=${SERVER_IP}#GhostWire-DNSCore-${SERVER_IP}"
GHOSTWIRE_URI="ghostwire://${REALITY_UUID}@${SERVER_IP}:443?reality_pk=${REALITY_PUBKEY}&reality_sid=${REALITY_SHORT_ID}&sni=${CAMOUFLAGE_SNI}&dnstt_pk=${DNSTT_PUBKEY}&dnstt_domain=${TUNNEL_DOMAIN}&dnstt_ip=${SERVER_IP}#GhostWire-Node"

# Save Credentials Summary
cat << EOF > "${CONFIG_DIR}/credentials.json"
{
  "server_ip": "${SERVER_IP}",
  "tunnel_domain": "${TUNNEL_DOMAIN}",
  "reality": {
    "port": 443,
    "uuid": "${REALITY_UUID}",
    "public_key": "${REALITY_PUBKEY}",
    "short_id": "${REALITY_SHORT_ID}",
    "sni": "${CAMOUFLAGE_SNI}",
    "uri": "${VLESS_URI}"
  },
  "dnstt": {
    "port": 53,
    "domain": "${TUNNEL_DOMAIN}",
    "public_key": "${DNSTT_PUBKEY}",
    "uri": "${DNSTT_URI}"
  },
  "ghostwire_unified_uri": "${GHOSTWIRE_URI}"
}
EOF

# Display Final Tactical Summary Box
echo ""
echo -e "${GREEN}${BOLD}========================================================================${NC}"
echo -e "${GREEN}${BOLD}     ★ GHOSTWIRE PERSONAL SERVER SUCCESSFULLY DEPLOYED & ARMED ★     ${NC}"
echo -e "${GREEN}${BOLD}========================================================================${NC}"
echo -e " ${BOLD}Architecture:${NC}  Dual-Engine Censorship Circumvention Core"
echo -e " ${BOLD}Developer:${NC}     SixOwner"
echo -e " ${BOLD}Community:${NC}     ایران آزاد (https://t.me/irazadnet)"
echo -e " ${BOLD}Server IP:${NC}     ${SERVER_IP}"
echo -e " ${BOLD}Tunnel Domain:${NC} ${TUNNEL_DOMAIN}"
echo -e " ${BOLD}Ports Active:${NC}  TCP 443 (VLESS-Reality) | UDP 53 (DNSTT Noise)"
echo -e "${GREEN}${BOLD}------------------------------------------------------------------------${NC}"
echo -e "${CYAN}${BOLD}[1] UNIFIED GHOSTWIRE URI (Copy & Import in GhostWire Android App):${NC}"
echo -e "${YELLOW}${GHOSTWIRE_URI}${NC}"
echo ""
echo -e "${CYAN}${BOLD}[2] STANDALONE VLESS-REALITY URI:${NC}"
echo -e "${YELLOW}${VLESS_URI}${NC}"
echo ""
echo -e "${CYAN}${BOLD}[3] STANDALONE DNSTT ENCRYPTED DNS URI:${NC}"
echo -e "${YELLOW}${DNSTT_URI}${NC}"
echo -e "${GREEN}${BOLD}------------------------------------------------------------------------${NC}"
echo -e "${BOLD}Management Commands:${NC}"
echo -e "  - Check Status:   ${CYAN}systemctl status sing-box dnstt-server${NC}"
echo -e "  - View Config:    ${CYAN}cat /etc/ghostwire/credentials.json${NC}"
echo -e "  - Restart Core:   ${CYAN}systemctl restart sing-box dnstt-server${NC}"
echo -e "${GREEN}${BOLD}========================================================================${NC}"
echo ""
