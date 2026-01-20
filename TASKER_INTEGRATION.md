# Tasker Integration Guide

DNS Changer Ultimate supports integration with Tasker and other automation apps through broadcast intents. This allows you to automate DNS connections based on various triggers such as WiFi networks, time of day, location, and more.

## Supported Actions

### 1. Connect to DNS

**Action:** `com.dns.changer.ultimate.TASKER_CONNECT`

Connect to a DNS server using either a preset server ID or custom DNS addresses.

#### Using Preset Server

**Extras:**
- `server_id` (String): ID of the preset server

**Preset Server IDs:**
- `cloudflare` - Cloudflare (1.1.1.1)
- `google` - Google DNS (8.8.8.8)
- `opendns` - OpenDNS (208.67.222.222)
- `quad9_secured` - Quad9 Secured (9.9.9.9)
- `adguard` - AdGuard DNS (94.140.14.14)
- `mullvad` - Mullvad DNS (194.242.2.2)
- `cloudflare_family` - Cloudflare Family (1.1.1.3)
- `cleanbrowsing_security` - CleanBrowsing Security (185.228.168.9)

See the full list in the app's DNS server picker.

**Tasker Example:**
```
Task: Connect Cloudflare
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_CONNECT
    Extra: server_id:cloudflare
```

**ADB Example:**
```bash
adb shell am broadcast -a com.dns.changer.ultimate.TASKER_CONNECT --es server_id cloudflare
```

#### Using Custom DNS

**Extras:**
- `primary_dns` (String, required): Primary DNS IP address
- `secondary_dns` (String, optional): Secondary DNS IP address (defaults to primary)
- `server_name` (String, optional): Display name (defaults to "Tasker DNS")
- `is_doh` (Boolean, optional): Enable DNS-over-HTTPS (requires premium, defaults to false)
- `doh_url` (String, optional): DoH URL (required if is_doh is true)

**Tasker Example:**
```
Task: Connect Custom DNS
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_CONNECT
    Extra: primary_dns:8.8.8.8
    Extra: secondary_dns:8.8.4.4
    Extra: server_name:My Custom DNS
```

**ADB Example:**
```bash
adb shell am broadcast -a com.dns.changer.ultimate.TASKER_CONNECT \
  --es primary_dns 8.8.8.8 \
  --es secondary_dns 8.8.4.4 \
  --es server_name "My Custom DNS"
```

#### Using DNS-over-HTTPS (Premium)

**Tasker Example:**
```
Task: Connect DoH
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_CONNECT
    Extra: primary_dns:1.1.1.1
    Extra: secondary_dns:1.0.0.1
    Extra: server_name:Cloudflare DoH
    Extra: is_doh:true
    Extra: doh_url:https://cloudflare-dns.com/dns-query
```

**ADB Example:**
```bash
adb shell am broadcast -a com.dns.changer.ultimate.TASKER_CONNECT \
  --es primary_dns 1.1.1.1 \
  --es secondary_dns 1.0.0.1 \
  --es server_name "Cloudflare DoH" \
  --ez is_doh true \
  --es doh_url "https://cloudflare-dns.com/dns-query"
```

### 2. Disconnect from DNS

**Action:** `com.dns.changer.ultimate.TASKER_DISCONNECT`

Disconnect from the current DNS server and revert to your network's default DNS.

**Tasker Example:**
```
Task: Disconnect DNS
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_DISCONNECT
```

**ADB Example:**
```bash
adb shell am broadcast -a com.dns.changer.ultimate.TASKER_DISCONNECT
```

### 3. Switch DNS Server

**Action:** `com.dns.changer.ultimate.TASKER_SWITCH`

Switch to a different DNS server while already connected. Uses the same extras as TASKER_CONNECT.

If not currently connected, this will behave the same as TASKER_CONNECT.

**Tasker Example:**
```
Task: Switch to AdGuard
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_SWITCH
    Extra: server_id:adguard
```

**ADB Example:**
```bash
adb shell am broadcast -a com.dns.changer.ultimate.TASKER_SWITCH --es server_id adguard
```

### 4. Query Connection Status

**Action:** `com.dns.changer.ultimate.TASKER_QUERY_STATUS`

Query the current connection status. The app will send a broadcast with the result.

**Result Broadcast:**
- **Action:** `com.dns.changer.ultimate.TASKER_STATUS_RESULT`
- **Extras:**
  - `is_connected` (Boolean): Whether DNS VPN is currently connected
  - `server_name` (String): Name of connected DNS server (empty if disconnected)
  - `state` (String): Current state - `connected`, `disconnected`, `connecting`, `disconnecting`, `switching`, or `error`

**Tasker Example:**
```
Task: Check DNS Status
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_QUERY_STATUS

Profile: DNS Status Changed
Event: Intent Received
    Action: com.dns.changer.ultimate.TASKER_STATUS_RESULT
Task: Handle Status
A1: Variable Set
    Name: %dns_connected
    To: %is_connected
A2: Variable Set
    Name: %dns_server
    To: %server_name
A3: Variable Set
    Name: %dns_state
    To: %state
```

**ADB Example:**
```bash
adb shell am broadcast -a com.dns.changer.ultimate.TASKER_QUERY_STATUS
```

## Use Cases & Examples

### Auto-Connect on Public WiFi

Automatically connect to a secure DNS when joining public WiFi networks.

**Profile:** WiFi Connected (SSID: "Starbucks WiFi" or "Airport WiFi")
**Task:** Connect to Cloudflare
```
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_CONNECT
    Extra: server_id:cloudflare
```

### Auto-Disconnect on Home WiFi

Disconnect DNS protection when connecting to your trusted home network.

**Profile:** WiFi Connected (SSID: "Home Network")
**Task:** Disconnect DNS
```
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_DISCONNECT
```

### Family Filter During School Hours

Switch to family-safe DNS during specific hours for parental control.

**Profile:** Time (08:00 - 16:00, School Days)
**Enter Task:** Enable Family Filter
```
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_CONNECT
    Extra: server_id:cloudflare_family
```

**Exit Task:** Switch Back to Normal
```
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_SWITCH
    Extra: server_id:cloudflare
```

### Ad Blocking on Mobile Data

Use ad-blocking DNS only when on cellular to save bandwidth.

**Profile:** State - Mobile Network
**Enter Task:** Enable AdGuard
```
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_CONNECT
    Extra: server_id:adguard
```

**Exit Task:** Disconnect
```
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_DISCONNECT
```

### Location-Based DNS

Use different DNS servers based on your location.

**Profile 1:** Location (Work)
**Task:** Work DNS
```
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_CONNECT
    Extra: primary_dns:10.0.0.1
    Extra: server_name:Work DNS
```

**Profile 2:** Location (Home)
**Task:** Home DNS
```
A1: Send Intent
    Action: com.dns.changer.ultimate.TASKER_SWITCH
    Extra: server_id:cloudflare
```

## Requirements

1. **VPN Permission:** DNS Changer requires VPN permission to function. Make sure to grant VPN permission in the app before using Tasker automation.

2. **Premium Features:** DNS-over-HTTPS (DoH) requires a premium subscription.

3. **Tasker/Automation App:** You need Tasker or another automation app that can send broadcast intents.

## Troubleshooting

### VPN Permission Error
If automation fails, ensure VPN permission is granted:
1. Open DNS Changer
2. Tap "Connect" manually once
3. Grant VPN permission when prompted
4. Now automation should work

### Commands Not Working
- Verify the action name is exactly `com.dns.changer.ultimate.TASKER_CONNECT` (case-sensitive)
- Check that extra names match exactly (e.g., `server_id`, not `serverId`)
- Use ADB to test commands directly before setting up Tasker profiles

### DoH Not Working
- Ensure you have an active premium subscription
- Verify the DoH URL is valid and uses HTTPS
- Check that `is_doh` is set to `true` (boolean)

## Testing with ADB

You can test commands using ADB before setting them up in Tasker:

```bash
# Test connection
adb shell am broadcast -a com.dns.changer.ultimate.TASKER_CONNECT --es server_id cloudflare

# Test disconnection
adb shell am broadcast -a com.dns.changer.ultimate.TASKER_DISCONNECT

# Test status query
adb shell am broadcast -a com.dns.changer.ultimate.TASKER_QUERY_STATUS
```

## Support

If you encounter issues with Tasker integration:
1. Check the app logs using `adb logcat | grep TaskerIntentReceiver`
2. Verify your Tasker task configuration
3. Report issues at: https://github.com/burakgon/dns-changer-ultimate-android/issues
