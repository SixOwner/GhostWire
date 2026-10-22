<div align="center">

# ⚡ GhostWire
### Ultra-Resilient Censorship-Circumvention Client & Mesh Protocol
**Engineered for Zero-Downtime Connectivity & National Internet Blackouts**

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-00E5FF?style=for-the-badge&logo=android)](https://github.com/SixOwner/GhostWire)
[![Architecture](https://img.shields.io/badge/Core-Rust%20%2B%20Tokio%20Async-8A2BE2?style=for-the-badge&logo=rust)](https://github.com/SixOwner/GhostWire)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%202027-00E5FF?style=for-the-badge&logo=jetpackcompose)](https://github.com/SixOwner/GhostWire)
[![APK Size](https://img.shields.io/badge/APK%20Size-2.1%20MB-00E676?style=for-the-badge)](https://github.com/SixOwner/GhostWire/releases)
[![Community](https://img.shields.io/badge/Community-%D8%A7%DB%8C%D8%B1%D8%A7%D9%86%20%D8%A2%D8%B2%D8%A7%D8%AF-0088cc?style=for-the-badge&logo=telegram)](https://t.me/irazadnet)
[![License](https://img.shields.io/badge/License-MIT-white?style=for-the-badge)](LICENSE)

<br/>

[راهنمای فارسی](#-راهنمای-فارسی) • [English Guide](#-english-guide) • [Server Deployment](#-1-click-server-deployment) • [Architecture](#-architecture) • [Protocol Specification](#-ghostwire-uri-specification)

</div>

---

## 📖 Overview

**GhostWire** is an enterprise-grade, native Android censorship-circumvention client powered by a custom asynchronous **Rust Core** (`ghostwire-core`) and an ultra-modern **2027 Jetpack Compose** interface. 

Engineered specifically to survive extreme deep-packet inspection (DPI), active probing, and total international gateway shutdowns (National Intranet Blackouts / شبکه ملی اطلاعات), GhostWire fuses optical TLS camouflage with recursive domestic DNS tunneling.

---

## 🇮🇷 راهنمای فارسی

### ویژگی‌های کلیدی کلاینت GhostWire
- **هسته قدرتمند و فوق‌سریع Rust:** بهره‌گیری از توکیو (Tokio Async I/O) بدون وقفه زباله‌روب جاوا (Zero GC Pause)، بهینه‌سازی شده در سطح حافظه بدون داغ شدن گوشی یا مصرف باتری.
- **سپر بقای لایه ۵۳ (GhostDNS Core):** عبور ترافیک از داخل ریزالورهای داخلی (همراه اول، ایرانسل، شاتل و مخابرات) در زمان **قطعی کامل اینترنت بین‌الملل**.
- **استتار اپل (WireReality):** هماهنگ با هندشیک‌های TLS 1.3 سرورهای رسمی اپل (`gateway.icloud.com`) با جریان ضدشناسایی `xtls-rprx-vision`.
- **مسیریابی تفکیک‌شده هوشمند (Dynamic Split Tunneling):** امکان عبور دادن یا فیلتر کردن برنامه‌های خاص (مانند تفکیک تلگرام، واتساپ از برنامه‌های بانکی).
- **حجم فوق‌العاده کم (Ultra-Lean):** کمتر از **۲.۱ مگابایت** با استفاده از تفکیک معماری ABI و تکنولوژی پیشرفته R8 Shrinking.
- **کاملاً آفلاین و مستقل:** تمام فونت‌های وزیرمتن (Vazirmatn) و بانک اطلاعاتی ریزالورهای داخلی به صورت محلی در خود اپلیکیشن قرار دارند و به هیچ سرور خارجی برای رابط کاربری وابسته نیستند.
- **پشتیبانی کامل دو زبانه (فارسی RTL و انگلیسی LTR):** با چیدمان استاندارد و تغییر لحظه‌ای بدون نیاز به ریستارت دستگاه.

---

### 🚀 راه‌اندازی سرور شخصی با یک دستور (لینوکس)
اگر یک سرور مجازی اوبونتو یا دبیان دارید، می‌توانید پروتکل `ghostwire://` را تنها با یک دستور روی آن بالا بیاورید:

```bash
bash <(curl -sSL https://raw.githubusercontent.com/SixOwner/GhostWire/main/server/deploy.sh)
```

#### تنظیمات DNS در کلودفلر قبل از اجرا:
1. یک رکورد **A** با نام `ns` به سمت `IP سرور شما` ایجاد کنید (پروکسی **خاموش** / ابر خاکستری).
2. یک رکورد **NS** با نام `t` به سمت `ns.yourdomain.com` ایجاد کنید (پروکسی **خاموش** / ابر خاکستری).

پس از اجرای اسکریپت، لینک اختصاصی `ghostwire://` را کپی کرده و در اپلیکیشن اندروید وارد کنید.

---

## 🌐 English Guide

### Core Capabilities
- **Rust Native Engine:** Pure zero-copy memory architecture written in Rust with Tokio asynchronous reactor.
- **WireReality Optical Camouflage:** High-throughput TLS 1.3 carrier fronting Apple edge nodes (`gateway.icloud.com`).
- **GhostDNS Noise Tunneling:** Transports Base32 Noise frames over domestic recursive UDP port 53 to circumvent national blackouts.
- **Dynamic Split Tunneling:** Kernel-level application whitelisting and blacklisting via `VpnService.Builder`.
- **Ultra-Lean Binary:** Less than **2.2 MB** total release size with ProGuard/R8 dead-code stripping and ABI splits (`arm64-v8a`, `armeabi-v7a`).
- **Zero Cloud Runtime Asset Dependencies:** Bundled offline Persian typography (Vazirmatn) and embedded Iranian ISP DNS resolver database.
- **Dual Visual Theme:** Cyber Minimalist *Dark Shield* (`#0D0F12`) and frosted high-contrast *Light Cyber* (`#F4F6F9`).

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 GhostWire Android Client                    │
│      (Jetpack Compose • DataStore • Dynamic Split Tunnel)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ JNI Zero-Copy Interface
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 ghostwire-core (Rust Engine)                │
│       PMTUD • Packet Ring • L7 Filter • TLS Desync          │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
    GhostDNS Noise Frame (UDP 53)     WireReality Handshake (TCP 443)
                │                             │
                ▼                             ▼
    Domestic Iranian Resolvers        Public Internet Transit
     (MCI / Irancell / Shatel)                │
                │                             │
                ▼                             │
    AWS / Personal Linux Server ◄─────────────┘
      (DNSTT-Server + Sing-Box)
```

---

## 📡 1-Click Server Deployment

Deploy the complete `ghostwire://` backend on any clean **Ubuntu 20.04/22.04/24.04** or **Debian 11/12** server:

```bash
bash <(curl -sSL https://raw.githubusercontent.com/SixOwner/GhostWire/main/server/deploy.sh)
```

### Required DNS Hierarchy
Before running the deployment script, configure two records in your DNS dashboard (e.g., Cloudflare):

| Record Type | Subdomain | Target | Proxy Status |
| :--- | :--- | :--- | :--- |
| **A** | `ns` | `<YOUR_SERVER_IP>` | **DNS Only (Unproxied)** |
| **NS** | `t` | `ns.<yourdomain.com>` | **DNS Only (Unproxied)** |

---

## 🔗 `ghostwire://` URI Specification

GhostWire uses a unified URI scheme that binds both transport layers into a single self-healing configuration profile:

```text
ghostwire://<REALITY_UUID>@<SERVER_IP>:443?reality_pk=<REALITY_PUBKEY>&reality_sid=<SHORT_ID>&sni=<CAMOUFLAGE_SNI>&dnstt_pk=<DNSTT_PUBKEY>&dnstt_domain=<TUNNEL_DOMAIN>&dnstt_ip=<SERVER_IP>#GhostWire-NodeName
```

### Parameters:
- `reality_pk`: Curve25519 public key for Reality TLS camouflage.
- `reality_sid`: Hexadecimal short ID for handshake authentication.
- `sni`: Whitelisted camouflage server name (e.g. `gateway.icloud.com`).
- `dnstt_pk`: Noise protocol static public key for DNS tunnel session keys.
- `dnstt_domain`: Base authoritative tunnel subdomain (e.g. `t.nteam.bond`).
- `dnstt_ip`: Primary fallback nameserver address.

---

## 📱 Releases & Installation

Download the official optimized release APK from the [Releases](https://github.com/SixOwner/GhostWire/releases) section or compile directly:

```bash
# Build native release libraries (arm64-v8a & armeabi-v7a)
cargo build --release --target aarch64-linux-android
cargo build --release --target armv7-linux-androideabi

# Assemble optimized Android release APKs
./gradlew assembleRelease
```

Generated APK Artifacts:
- `app/build/outputs/apk/release/app-arm64-v8a-release.apk` (**2.16 MB**)
- `app/build/outputs/apk/release/app-armeabi-v7a-release.apk` (**2.07 MB**)

---

## 🛡 Security & Privacy

- **Zero AI / Scaffolding Footprint:** Strictly handcrafted human enterprise engineering.
- **Compile-Time Cloaking:** Sensitive server endpoints, IPs, and cryptographic keys are encrypted using multi-layer rolling XOR masking inside native Rust binaries to prevent plain-text string scraping.
- **Zero Log Storage:** The server operates strictly in RAM with zero request logging.

---

## 👥 Credits & Community

- **Lead Architecture & Engineering:** **SixOwner**
- **Official Channel:** [ایران آزاد (@irazadnet)](https://t.me/irazadnet)

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
