# Project Notes for Claude

## Build Environment

### macOS (JetBrains Toolbox)

Android Studio is installed via JetBrains Toolbox at:
`~/Applications/Android Studio.app`

Java (JBR) bundled with Android Studio:
`~/Applications/Android Studio.app/Contents/jbr/Contents/Home`

ADB Location:
`~/Library/Android/sdk/platform-tools/adb`

### Linux (JetBrains Toolbox)

Android Studio: `~/.local/share/JetBrains/Toolbox/apps/android-studio`
Java (JBR): `~/.local/share/JetBrains/Toolbox/apps/android-studio/jbr`
ADB: `~/Android/Sdk/platform-tools/adb`

### Build Commands

To build the project:
```bash
# macOS
export JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug

# Linux
export JAVA_HOME="$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr"
./gradlew assembleDebug
```

To install to connected device:
```bash
# macOS
export JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew installDebug

# Linux
export JAVA_HOME="$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr"
./gradlew installDebug
```

To launch the app:
```bash
# macOS
~/Library/Android/sdk/platform-tools/adb shell monkey -p com.burakgon.dnschanger -c android.intent.category.LAUNCHER 1

# Linux
~/Android/Sdk/platform-tools/adb shell monkey -p com.burakgon.dnschanger -c android.intent.category.LAUNCHER 1
```

### ADB Notes
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
3. **Quick Settings Tile** - Toggle DNS from notification shade (DnsQuickSettingsTile)

### Premium-Gated Features (Watch Ad to Unlock)
- **Multiple Custom DNS** - Free users limited to 1 custom DNS (ConnectScreen)
- Speed test (more than basic usage)
- Other features use `onShowPremiumGate` callback with rewarded ads

### Key Files for Premium
- `PremiumViewModel.kt` - Manages premium state, products, purchases
- `PaywallScreen.kt` - High-converting paywall with animated crown, social proof, Material You support
- `PremiumGatePopup.kt` - Popup for premium-gated features (Watch Ad / Go Premium options)
- `DnsPreferences.kt` - Stores `isPremium`, `startOnBoot` preferences
- `BootReceiver.kt` - Listens for BOOT_COMPLETED, starts VPN if premium

### PaywallScreen Features
- Animated crown icon with jewels, shimmer, floating animation, orbiting particles
- Social proof banner (4.7 rating, 10M+ users from Play Store)
- 6 benefit items in 2x3 grid
- Plan selection cards (Monthly, Yearly with trial, Yearly)
- Animated CTA button with shimmer effect
- Full light/dark mode support with Material You dynamic colors
- Theme detection via background luminance (works with manual theme switch)

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
