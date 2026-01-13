package com.dns.changer.ultimate.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

enum class DnsCategory(
    val displayName: String,
    val icon: ImageVector
) {
    SPEED("Speed", Icons.Default.Speed),
    PRIVACY("Privacy", Icons.Default.Lock),
    SECURITY("Security", Icons.Default.Security),
    AD_BLOCKING("Ad Blocking", Icons.Default.Block),
    FAMILY("Family", Icons.Default.FamilyRestroom),
    CUSTOM("Custom", Icons.Default.PersonAdd)
}

data class DnsServer(
    val id: String,
    val name: String,
    val primaryDns: String,
    val secondaryDns: String,
    val category: DnsCategory,
    val description: String = "",
    val isCustom: Boolean = false,
    val isDoH: Boolean = false,
    val dohUrl: String? = null
)

// Preset DNS servers
object PresetDnsServers {
    val all: List<DnsServer> = listOf(
        // Speed category
        DnsServer(
            id = "cloudflare",
            name = "Cloudflare",
            primaryDns = "1.1.1.1",
            secondaryDns = "1.0.0.1",
            category = DnsCategory.SPEED,
            description = "Fast & private DNS"
        ),
        DnsServer(
            id = "google",
            name = "Google DNS",
            primaryDns = "8.8.8.8",
            secondaryDns = "8.8.4.4",
            category = DnsCategory.SPEED,
            description = "Google public DNS"
        ),
        DnsServer(
            id = "opendns",
            name = "OpenDNS",
            primaryDns = "208.67.222.222",
            secondaryDns = "208.67.220.220",
            category = DnsCategory.SPEED,
            description = "Cisco OpenDNS"
        ),
        DnsServer(
            id = "nextdns",
            name = "NextDNS",
            primaryDns = "45.90.28.0",
            secondaryDns = "45.90.30.0",
            category = DnsCategory.SPEED,
            description = "Customizable DNS"
        ),
        DnsServer(
            id = "level3",
            name = "Level3",
            primaryDns = "4.2.2.1",
            secondaryDns = "4.2.2.2",
            category = DnsCategory.SPEED,
            description = "Level 3 Communications"
        ),

        // Privacy category
        DnsServer(
            id = "mullvad",
            name = "Mullvad DNS",
            primaryDns = "194.242.2.2",
            secondaryDns = "194.242.2.3",
            category = DnsCategory.PRIVACY,
            description = "Privacy-focused DNS"
        ),
        DnsServer(
            id = "quad9_unsecured",
            name = "Quad9 Unsecured",
            primaryDns = "9.9.9.10",
            secondaryDns = "149.112.112.10",
            category = DnsCategory.PRIVACY,
            description = "No filtering, no DNSSEC"
        ),
        DnsServer(
            id = "dnssb",
            name = "DNS.SB",
            primaryDns = "185.222.222.222",
            secondaryDns = "45.11.45.11",
            category = DnsCategory.PRIVACY,
            description = "Swiss privacy DNS"
        ),
        DnsServer(
            id = "controld",
            name = "Control D",
            primaryDns = "76.76.2.0",
            secondaryDns = "76.76.10.0",
            category = DnsCategory.PRIVACY,
            description = "Customizable privacy DNS"
        ),
        DnsServer(
            id = "cloudflare_privacy",
            name = "Cloudflare Privacy",
            primaryDns = "1.1.1.2",
            secondaryDns = "1.0.0.2",
            category = DnsCategory.PRIVACY,
            description = "Blocks malware"
        ),

        // Security category
        DnsServer(
            id = "quad9_secured",
            name = "Quad9 Secured",
            primaryDns = "9.9.9.9",
            secondaryDns = "149.112.112.112",
            category = DnsCategory.SECURITY,
            description = "Threat blocking with DNSSEC"
        ),
        DnsServer(
            id = "cloudflare_security",
            name = "Cloudflare Security",
            primaryDns = "1.1.1.2",
            secondaryDns = "1.0.0.2",
            category = DnsCategory.SECURITY,
            description = "Blocks malware & phishing"
        ),
        DnsServer(
            id = "comodo",
            name = "Comodo Secure",
            primaryDns = "8.26.56.26",
            secondaryDns = "8.20.247.20",
            category = DnsCategory.SECURITY,
            description = "Blocks malicious domains"
        ),
        DnsServer(
            id = "cleanbrowsing_security",
            name = "CleanBrowsing Security",
            primaryDns = "185.228.168.9",
            secondaryDns = "185.228.169.9",
            category = DnsCategory.SECURITY,
            description = "Phishing & malware filter"
        ),
        DnsServer(
            id = "neustar_threat",
            name = "Neustar Threat Protection",
            primaryDns = "156.154.70.2",
            secondaryDns = "156.154.71.2",
            category = DnsCategory.SECURITY,
            description = "Enterprise-grade threat protection"
        ),

        // Ad Blocking category
        DnsServer(
            id = "adguard",
            name = "AdGuard DNS",
            primaryDns = "94.140.14.14",
            secondaryDns = "94.140.15.15",
            category = DnsCategory.AD_BLOCKING,
            description = "Blocks ads & trackers"
        ),
        DnsServer(
            id = "mullvad_adblock",
            name = "Mullvad Ad-blocking",
            primaryDns = "194.242.2.3",
            secondaryDns = "194.242.2.4",
            category = DnsCategory.AD_BLOCKING,
            description = "Privacy + ad blocking"
        ),
        DnsServer(
            id = "controld_ads",
            name = "Control D Ads",
            primaryDns = "76.76.2.2",
            secondaryDns = "76.76.10.2",
            category = DnsCategory.AD_BLOCKING,
            description = "Blocks ads"
        ),
        DnsServer(
            id = "nextdns_ads",
            name = "NextDNS Ads & Trackers",
            primaryDns = "45.90.28.167",
            secondaryDns = "45.90.30.167",
            category = DnsCategory.AD_BLOCKING,
            description = "Ad & tracker blocking"
        ),
        DnsServer(
            id = "tiarap",
            name = "Tiarap DNS",
            primaryDns = "188.166.206.224",
            secondaryDns = "139.59.134.108",
            category = DnsCategory.AD_BLOCKING,
            description = "Japanese ad-blocking DNS"
        ),

        // Family category
        DnsServer(
            id = "cloudflare_family",
            name = "Cloudflare Family",
            primaryDns = "1.1.1.3",
            secondaryDns = "1.0.0.3",
            category = DnsCategory.FAMILY,
            description = "Blocks malware & adult content"
        ),
        DnsServer(
            id = "opendns_family",
            name = "OpenDNS FamilyShield",
            primaryDns = "208.67.222.123",
            secondaryDns = "208.67.220.123",
            category = DnsCategory.FAMILY,
            description = "Pre-configured family filter"
        ),
        DnsServer(
            id = "cleanbrowsing_family",
            name = "CleanBrowsing Family",
            primaryDns = "185.228.168.168",
            secondaryDns = "185.228.169.168",
            category = DnsCategory.FAMILY,
            description = "Family-safe browsing"
        ),
        DnsServer(
            id = "adguard_family",
            name = "AdGuard Family",
            primaryDns = "94.140.14.15",
            secondaryDns = "94.140.15.16",
            category = DnsCategory.FAMILY,
            description = "Safe search + ad blocking"
        ),
        DnsServer(
            id = "mullvad_family",
            name = "Mullvad Family",
            primaryDns = "194.242.2.4",
            secondaryDns = "194.242.2.5",
            category = DnsCategory.FAMILY,
            description = "Privacy + family filter"
        ),
        DnsServer(
            id = "neustar_family",
            name = "Neustar Family Secure",
            primaryDns = "156.154.70.3",
            secondaryDns = "156.154.71.3",
            category = DnsCategory.FAMILY,
            description = "Family-safe DNS"
        )
    )

    fun getByCategory(category: DnsCategory): List<DnsServer> =
        all.filter { it.category == category }

    fun getById(id: String): DnsServer? = all.find { it.id == id }
}
