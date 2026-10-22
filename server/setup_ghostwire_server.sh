#!/usr/bin/env bash
# ==============================================================================
# GhostWire Automated Server Provisioning Pipeline
# Components: Sing-box (VLESS-Reality) + DNSTT-Server (Noise KCP over UDP/TCP 53)
# ==============================================================================
set -euo pipefail

echo ">>> [1/7] Initializing GhostWire Environment..."
export DEBIAN_FRONTEND=noninteractive
SERVER_DIR="/etc/ghostwire"
mkdir -p "${SERVER_DIR}"

# Update & install core build tools
apt-get update -y
apt-get install -y --no-install-recommends \
    golang \
    git \
    build-essential \
    iptables \
    certbot \
    curl \
    jq \
    tar \
    ca-certificates \
    libcap2-bin \
    net-tools

# Enable IPv4 Forwarding
echo ">>> [2/7] Configuring Kernel Networking Parameters..."
sysctl -w net.ipv4.ip_forward=1
sysctl -w net.core.rmem_max=16777216
sysctl -w net.core.wmem_max=16777216
cat << 'EOF' > /etc/sysctl.d/99-ghostwire.conf
net.ipv4.ip_forward=1
net.core.rmem_max=16777216
net.core.wmem_max=16777216
net.ipv4.tcp_fastopen=3
EOF
sysctl --system > /dev/null 2>&1 || true

# Free UDP & TCP Port 53 by disabling systemd-resolved DNSStubListener
echo ">>> [3/7] Freeing Port 53 (systemd-resolved reconfiguration)..."
if [ -f /etc/systemd/resolved.conf ]; then
    sed -i 's/^#DNSStubListener=.*/DNSStubListener=no/' /etc/systemd/resolved.conf
    sed -i 's/^DNSStubListener=.*/DNSStubListener=no/' /etc/systemd/resolved.conf
    if ! grep -q "DNSStubListener=no" /etc/systemd/resolved.conf; then
        echo "DNSStubListener=no" >> /etc/systemd/resolved.conf
    fi
fi

# Ensure valid upstream DNS for the host itself
if [ -L /etc/resolv.conf ] || [ -f /etc/resolv.conf ]; then
    rm -f /etc/resolv.conf
    cat << 'EOF' > /etc/resolv.conf
nameserver 1.1.1.1
nameserver 8.8.8.8
nameserver 9.9.9.9
EOF
fi
systemctl restart systemd-resolved || true

# Install Sing-box (VLESS + Reality XTLS + SOCKS5 local ingress for DNSTT)
echo ">>> [4/7] Installing Sing-box Core..."
SINGBOX_VER="1.14.3"
ARCH=$(dpkg --print-architecture)
if [ "$ARCH" = "amd64" ]; then
    SB_ARCH="linux-amd64"
elif [ "$ARCH" = "arm64" ]; then
    SB_ARCH="linux-arm64"
else
    SB_ARCH="linux-amd64"
fi

TMP_DIR=$(mktemp -d)
curl -fsSL "https://github.com/SagerNet/sing-box/releases/download/v${SINGBOX_VER}/sing-box-${SINGBOX_VER}-${SB_ARCH}.tar.gz" -o "${TMP_DIR}/sing-box.tar.gz"
tar -xzf "${TMP_DIR}/sing-box.tar.gz" -C "${TMP_DIR}"
install -m 755 "${TMP_DIR}/sing-box-${SINGBOX_VER}-${SB_ARCH}/sing-box" /usr/local/bin/sing-box
rm -rf "${TMP_DIR}"

# Generate Reality Keys and UUID
echo ">>> [5/7] Generating Cryptographic Keys (Reality & DNSTT)..."
KEYPAIR_OUTPUT=$(/usr/local/bin/sing-box generate reality-keypair)
REALITY_PRIVKEY=$(echo "$KEYPAIR_OUTPUT" | grep -i "PrivateKey:" | awk '{print $2}')
REALITY_PUBKEY=$(echo "$KEYPAIR_OUTPUT" | grep -i "PublicKey:" | awk '{print $2}')
REALITY_UUID=$(/usr/local/bin/sing-box generate uuid)
REALITY_SHORT_ID=$(/usr/local/bin/sing-box generate rand --hex 8)
REALITY_SNI="gateway.icloud.com"

# Build DNSTT Server
echo ">>> [6/7] Building DNSTT-Server from Source..."
DNSTT_SRC_DIR="/opt/dnstt"
if [ -d "${DNSTT_SRC_DIR}" ]; then
    rm -rf "${DNSTT_SRC_DIR}"
fi
git clone https://www.bamsoftware.com/git/dnstt.git "${DNSTT_SRC_DIR}"
(cd "${DNSTT_SRC_DIR}/dnstt-server" && CGO_ENABLED=0 go build -v -ldflags="-s -w" -o /usr/local/bin/dnstt-server)
setcap 'cap_net_bind_service=+ep' /usr/local/bin/dnstt-server

# Generate DNSTT Noise Keys
/usr/local/bin/dnstt-server -gen-key -privkey-file "${SERVER_DIR}/dnstt.key" -pubkey-file "${SERVER_DIR}/dnstt.pub"
DNSTT_PUBKEY=$(cat "${SERVER_DIR}/dnstt.pub" | tr -d '[:space:]')
DNSTT_DOMAIN="t.nteam.bond"

# Configure Sing-box with Reality on :443 and internal Mixed SOCKS on 127.0.0.1:8000
cat << EOF > "${SERVER_DIR}/sing-box.json"
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
        "server_name": "${REALITY_SNI}",
        "reality": {
          "enabled": true,
          "handshake": {
            "server": "${REALITY_SNI}",
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
ExecStart=/usr/local/bin/dnstt-server -udp :53 -privkey-file /etc/ghostwire/dnstt.key ${DNSTT_DOMAIN} 127.0.0.1:8000
Restart=always
RestartSec=3
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
EOF

# Reload and Start Services
systemctl daemon-reload
systemctl enable sing-box dnstt-server
systemctl restart sing-box
systemctl restart dnstt-server

# Public IP Detection
PUBLIC_IP=$(curl -s https://api.ipify.org || echo "18.221.128.150")

# Write Credentials Export
cat << EOF > "${SERVER_DIR}/credentials.json"
{
  "server_ip": "${PUBLIC_IP}",
  "server_host": "ec2-18-221-128-150.us-east-2.compute.amazonaws.com",
  "reality": {
    "port": 443,
    "uuid": "${REALITY_UUID}",
    "public_key": "${REALITY_PUBKEY}",
    "short_id": "${REALITY_SHORT_ID}",
    "sni": "${REALITY_SNI}",
    "flow": "xtls-rprx-vision",
    "uri": "vless://${REALITY_UUID}@${PUBLIC_IP}:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=${REALITY_SNI}&fp=chrome&pbk=${REALITY_PUBKEY}&sid=${REALITY_SHORT_ID}&type=tcp#GhostWire-Reality-NodeAlpha"
  },
  "dnstt": {
    "port": 53,
    "domain": "${DNSTT_DOMAIN}",
    "public_key": "${DNSTT_PUBKEY}",
    "uri": "dnstt://${DNSTT_PUBKEY}@${DNSTT_DOMAIN}:53?server=${PUBLIC_IP}#GhostWire-DNSCore-NodeAlpha"
  },
  "ghostwire": {
    "uri": "ghostwire://${REALITY_UUID}@${PUBLIC_IP}:443?reality_pk=${REALITY_PUBKEY}&reality_sid=${REALITY_SHORT_ID}&sni=${REALITY_SNI}&dnstt_pk=${DNSTT_PUBKEY}&dnstt_domain=${DNSTT_DOMAIN}&dnstt_ip=${PUBLIC_IP}#GhostWire-Primary"
  }
}
EOF

chmod 600 "${SERVER_DIR}/credentials.json"
echo ">>> [7/7] Server Provisioning Complete!"
cat "${SERVER_DIR}/credentials.json"
