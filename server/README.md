# GhostWire Server Deployment & Administration Guide
# راهنمای جامع راه‌اندازی سرور اختصاصی پروتکل GhostWire

[English](#english-guide) | [راهنمای فارسی](#راهنمای-فارسی)

---

<a name="راهنمای-فارسی"></a>
## 🇮🇷 راهنمای فارسی

### درباره پروتکل دوگانه GhostWire
پروتکل `ghostwire://` یک معماری هیبریدی نسل جدید برای دور زدن شدیدترین سطوح فیلترینگ و حتی **شرایط قطعی کامل اینترنت بین‌الملل (بلک‌اوت ملی)** است. این سیستم از دو لایه انتقال هوشمند تشکیل شده است:

1. **لایه WireReality (پورت ۴۴۳ TCP):**
   - استتار ترافیک در قالب بسته‌های TLS 1.3 به سرورهای رسمی اپل (`gateway.icloud.com`).
   - استفاده از الگوریتم پیشرفته `xtls-rprx-vision` برای حذف کامل الگوهای رمزنگاری و فریب فایروال‌های هوش مصنوعی (DPI).
   - حداکثر سرعت برای تماشای ویدیو، بازی آنلاین و دانلود روزمره.

2. **لایه GhostDNS Core (پورت ۵۳ UDP با پروتکل Noise):**
   - عبور بسته‌های داده با استفاده از تونل رمزنگاری‌شده DNS بر روی ریزالورهای داخلی اپراتورهای ایرانی (همراه اول، ایرانسل، شاتل، مخابرات و...).
   - **سپر بقا در زمان قطعی کامل اینترنت:** از آنجایی که ارتباط با سرورهای نام دامنه (Authoritative Nameservers) در لایه ۵۳ برای کارکرد شبکه ملی اینترانت باز می‌ماند، این پروتکل ترافیک را داخل کدهای رمزنگاری‌شده Base32 حمل کرده و تحریم و فیلترینگ را دور می‌زند.

---

### پیش‌نیازها
1. یک سرور مجازی یا اختصاصی لینوکس (توصیه: **Ubuntu 22.04 / 24.04** یا **Debian 11 / 12**).
2. یک دامنه شخصی (ثبت شده در هر شرکتی مانند Namecheap، Spaceship یا هاست‌های داخلی) متصل به کلودفلر (Cloudflare).

---

### گام اول: تنظیم رکوردهای DNS (بسیار مهم)
وارد پنل مدیریت DNS دامنه خود در Cloudflare شوید و دو رکورد زیر را اضافه کنید:

| نوع رکورد (Type) | نام زیردامنه (Name) | آدرس مقصد (Target / Content) | وضعیت پروکسی (Proxy Status) | مدت زمان (TTL) |
| :--- | :--- | :--- | :--- | :--- |
| **A** | `ns` | `IP سرور شما` (مثال: `18.221.128.150`) | **DNS Only (ابر خاموش / خاکستری)** | Auto یا ۲ دقیقه |
| **NS** | `t` | `ns.yourdomain.com` | **DNS Only (ابر خاموش / خاکستری)** | Auto یا ۱ ساعت |

> [!CAUTION]
> توجه داشته باشید که تیک ابر نارنجی (Proxied) حتماً باید **خاموش** (DNS Only) باشد تا ترافیک مستقیماً به سرور شما برسد و کلودفلر پورت ۵۳ را مسدود نکند.

---

### گام دوم: راه‌اندازی با یک دستور
وارد محیط ترمینال سرور خود (از طریق SSH) شده و دستور زیر را اجرا کنید:

```bash
bash <(curl -sSL https://raw.githubusercontent.com/SixOwner/GhostWire/main/server/deploy.sh)
```

اسکریپت به صورت خودکار:
- هسته لینوکس را با الگوریتم BBR و بافرهای ۱۶ مگابایتی بهینه‌سازی می‌کند.
- پورت ۵۳ را از سرویس داخلی لینوکس آزاد می‌کند.
- نسخه رسمی Sing-Box را برای لایه Reality نصب و پیکربندی می‌کند.
- سورس‌کد امنیتی DNSTT را کامپایل کرده و سرویس Noise Tunnel را فعال می‌سازد.
- کلیدهای رمزنگاری را تولید و یک لینک یکپارچه اختصاصی `ghostwire://` به شما تحویل می‌دهد.

---

### گام سوم: اتصال در برنامه GhostWire اندروید
1. لینک خروجی ترمینال که با `ghostwire://` شروع می‌شود را کپی کنید.
2. برنامه **GhostWire** را در گوشی اندرویدی خود باز کنید.
3. برنامه به طور خودکار کلیپ‌بورد شما را شناسایی کرده و کانفیگ را اضافه می‌کند (یا می‌توانید در تب **Tactical** روی دکمه *Import Config* بزنید و لینک را پیست کنید).
4. در صفحه اول (Nexus)، انگشت خود را روی حلقه مرکزی بگذارید و به اینترنت امن متصل شوید!

---

### دستورات مدیریت سرور
- بررسی وضعیت سرویس‌ها:
  ```bash
  systemctl status sing-box dnstt-server
  ```
- مشاهده اطلاعات و کانفیگ‌های تولید شده:
  ```bash
  cat /etc/ghostwire/credentials.json
  ```
- راه‌اندازی مجدد سرویس‌ها:
  ```bash
  systemctl restart sing-box dnstt-server
  ```

---

<a name="english-guide"></a>
## 🌐 English Guide

### GhostWire Dual-Engine Architecture
The `ghostwire://` protocol standard delivers carrier-grade censorship circumvention by fusing two complementary transport channels:

1. **WireReality Layer (Port 443 TCP):**
   - Masquerades as TLS 1.3 handshakes to Apple infrastructure (`gateway.icloud.com`).
   - Employs XTLS Vision flow for dynamic zero-entropy packet shaping.
   - Low latency, high bandwidth optimized for streaming and real-time interaction.

2. **GhostDNS Core Layer (Port 53 UDP Noise Tunnel):**
   - High-velocity Noise-protocol encrypted frames packed inside standard recursive DNS queries.
   - **National Blackout Survival:** Survives national intranet disconnects by bouncing encapsulated packets off domestic ISP recursive resolvers to your authoritative NS.

---

### Prerequisites
- A Linux VPS/Server running **Ubuntu 20.04/22.04/24.04** or **Debian 11/12**.
- A custom domain managed on Cloudflare or any DNS provider.

---

### Step 1: DNS Records Setup
In your Cloudflare DNS manager, create the following two records:

| Record Type | Subdomain / Name | Target / Content | Proxy Status | TTL |
| :--- | :--- | :--- | :--- | :--- |
| **A** | `ns` | `YOUR_SERVER_IP` (e.g. `18.221.128.150`) | **DNS Only (Gray Cloud)** | Auto / 2 min |
| **NS** | `t` | `ns.yourdomain.com` | **DNS Only (Gray Cloud)** | Auto / 1 hour |

> [!IMPORTANT]
> The Cloudflare proxy status must be **DNS Only** (unproxied) to ensure port 53 UDP reaches your server directly.

---

### Step 2: Automated Deployment
Connect to your server via SSH and execute:

```bash
bash <(curl -sSL https://raw.githubusercontent.com/SixOwner/GhostWire/main/server/deploy.sh)
```

The script will:
- Enable BBR congestion control and expand network buffer pools.
- Release port 53 from `systemd-resolved`.
- Deploy and configure Sing-Box with Reality XTLS masquerade.
- Compile and arm DNSTT-Server with Noise protocol cryptography.
- Print your personal unified `ghostwire://` link.

---

### Step 3: Connect from GhostWire Android
1. Copy the `ghostwire://...` link printed on your terminal.
2. Open the **GhostWire** Android app.
3. The app detects the clipboard automatically, or navigate to **Tactical** -> **Import Config** and paste the link.
4. On the **Nexus** screen, tap the central pulse ring to establish the tunnel.

---

### Server Health & Diagnostics
```bash
# Check daemons status
systemctl status sing-box dnstt-server

# View saved credentials & URIs
cat /etc/ghostwire/credentials.json

# Restart both daemons
systemctl restart sing-box dnstt-server
```

---
*Developed by SixOwner | Community: [ایران آزاد](https://t.me/irazadnet)*
