# Project Notes for Claude

## Build Environment (Linux)

Android Studio is installed via JetBrains Toolbox at:
`~/.local/share/JetBrains/Toolbox/apps/android-studio`

Java (JBR) bundled with Android Studio:
`~/.local/share/JetBrains/Toolbox/apps/android-studio/jbr`

### Build Commands

To build the project:
```bash
export JAVA_HOME="$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr"
./gradlew assembleDebug
```

To install to connected device:
```bash
export JAVA_HOME="$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr"
~/Android/Sdk/platform-tools/adb install -r ./app/build/outputs/apk/debug/DNSChanger-*.apk
```

To launch the app:
```bash
~/Android/Sdk/platform-tools/adb shell am start -n com.dns.changer.ultimate/.MainActivity
```

### ADB Location
ADB is at: `~/Android/Sdk/platform-tools/adb`

If multiple devices connected, use `-s DEVICE_ID` flag (check devices with `adb devices`).

---

## Project Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3 / Material You
- **DI**: Hilt
- **Navigation**: Compose Navigation
- **Preferences**: DataStore
- **Subscriptions**: RevenueCat
- **Ads**: AdMob (rewarded ads for some features)

### Key Directories
```
app/src/main/java/com/dns/changer/ultimate/
├── data/
│   ├── model/          # Data classes (DnsServer, ConnectionState, etc.)
│   └── preferences/    # DataStore preferences (DnsPreferences, RatingPreferences)
├── service/
│   ├── DnsVpnService.kt       # Main VPN service for DNS routing
│   ├── DnsConnectionManager.kt # Manages VPN connection state
│   ├── DnsSpeedTestService.kt  # Speed test functionality
│   ├── DohClient.kt            # DNS-over-HTTPS client
│   ├── BootReceiver.kt         # Handles Start on Boot
│   └── DnsQuickSettingsTile.kt # Quick Settings tile for toggling DNS
├── ui/
│   ├── components/     # Reusable UI components
│   ├── navigation/     # Navigation setup (DnsNavHost, Screen)
│   ├── screens/
│   │   ├── connect/    # Main connect screen, server picker, custom DNS dialog
│   │   ├── speedtest/  # DNS speed test screen
│   │   ├── leaktest/   # DNS leak test screen
│   │   ├── settings/   # Settings screen
│   │   └── paywall/    # Premium subscription paywall
│   ├── theme/          # Material 3 theming
│   └── viewmodel/      # ViewModels (MainViewModel, PremiumViewModel, etc.)
├── ads/                # AdMob integration
└── MainActivity.kt     # Entry point
```

---

## Premium Features

### RevenueCat Setup
- App ID: `appf893c24c9f`
- Products:
  - `dns_changer_pro_monthly` - Monthly subscription
  - `dns_changer_pro_annual` - Annual subscription
  - `dns_changer_pro_weekly_trial` - Weekly with trial

### Premium-Only Features (No Watch Ad Option)
1. **Start on Boot** - Auto-connect VPN on device boot (SettingsScreen)
2. **DNS over HTTPS (DoH)** - Encrypted DNS queries (AddCustomDnsDialog)

### Premium-Gated Features (Watch Ad to Unlock)
- **Multiple Custom DNS** - Free users limited to 1 custom DNS (ConnectScreen)
- Speed test (more than basic usage)
- Other features use `onShowPremiumGate` callback with rewarded ads

### Key Files for Premium
- `PremiumViewModel.kt` - Manages premium state, products, purchases
- `PaywallScreen.kt` - Beautiful Material 3 subscription screen
- `DnsPreferences.kt` - Stores `isPremium`, `startOnBoot` preferences
- `BootReceiver.kt` - Listens for BOOT_COMPLETED, starts VPN if premium

---

## UI Patterns

### Premium Badge Style
Use consistent badge across the app:
```kotlin
Box(
    modifier = Modifier
        .background(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(4.dp)
        )
        .padding(horizontal = 6.dp, vertical = 2.dp)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "PRO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
```

### Showing Paywall
For premium-only features (no watch ad):
```kotlin
onShowPaywall = { showPaywall = true }
```

For features with watch ad option:
```kotlin
onShowPremiumGate = { unlockCallback -> /* shows PremiumGatePopup */ }
```

---

## String Resources
Located in `app/src/main/res/values/strings.xml`

---

## Notes
- System Java 25 is incompatible with Gradle - always use Android Studio JBR
- APK output has versioned name: `DNSChanger-X.X.X-debug.apk`
- App uses VpnService for DNS routing (requires VPN permission from user)
