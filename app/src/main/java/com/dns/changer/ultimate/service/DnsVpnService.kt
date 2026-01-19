package com.dns.changer.ultimate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import com.dns.changer.ultimate.MainActivity
import com.dns.changer.ultimate.R
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * DNS VPN Service - Changes DNS without affecting other traffic.
 *
 * This VPN only handles DNS queries (UDP port 53) and forwards them to the
 * configured DNS servers. All other traffic bypasses the VPN completely.
 */
class DnsVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.dns.changer.ultimate.START_VPN"
        const val ACTION_STOP = "com.dns.changer.ultimate.STOP_VPN"
        const val EXTRA_PRIMARY_DNS = "primary_dns"
        const val EXTRA_SECONDARY_DNS = "secondary_dns"
        const val EXTRA_SERVER_NAME = "server_name"
        const val EXTRA_IS_DOH = "is_doh"
        const val EXTRA_DOH_URL = "doh_url"

        private const val NOTIFICATION_CHANNEL_ID = "dns_vpn_channel"
        private const val NOTIFICATION_ID = 1

        private const val VPN_ADDRESS = "10.255.255.1"
        private const val VPN_MTU = 1500

        @Volatile
        var isVpnRunning: Boolean = false
            private set

        @Volatile
        var currentServerName: String? = null
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile
    private var isRunning = false
    private var workerThread: Thread? = null

    private var primaryDns: String = "1.1.1.1"
    private var secondaryDns: String = "1.0.0.1"
    private var serverName: String = "DNS Server"
    private var isDoH: Boolean = false
    private var dohUrl: String? = null
    private var dohClient: DohClient? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val newPrimaryDns = intent.getStringExtra(EXTRA_PRIMARY_DNS) ?: "1.1.1.1"
                val newSecondaryDns = intent.getStringExtra(EXTRA_SECONDARY_DNS) ?: "1.0.0.1"
                val newServerName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "DNS Server"
                val newIsDoH = intent.getBooleanExtra(EXTRA_IS_DOH, false)
                val newDohUrl = intent.getStringExtra(EXTRA_DOH_URL)

                // CRITICAL: Call startForeground immediately to avoid ForegroundServiceDidNotStartInTimeException
                // On Android 12+, this must happen within 10 seconds of startForegroundService()
                serverName = newServerName
                startForeground(NOTIFICATION_ID, createNotification())

                if (isRunning) {
                    android.util.Log.d("DnsVpnService", "Switching DNS from $serverName to $newServerName (DoH: $newIsDoH)")
                    primaryDns = newPrimaryDns
                    secondaryDns = newSecondaryDns
                    isDoH = newIsDoH
                    dohUrl = newDohUrl
                    updateDohClient()
                    currentServerName = serverName

                    // Restart VPN to apply new DNS
                    stopWorker()
                    closeVpnInterface()
                    startVpn()
                } else {
                    primaryDns = newPrimaryDns
                    secondaryDns = newSecondaryDns
                    isDoH = newIsDoH
                    dohUrl = newDohUrl
                    updateDohClient()
                    startVpn()
                }
            }
            ACTION_STOP -> {
                stopVpn()
            }
        }
        return START_STICKY
    }

    private fun updateDohClient() {
        dohClient?.shutdown()
        dohClient = if (isDoH && !dohUrl.isNullOrBlank()) {
            android.util.Log.d("DnsVpnService", "Creating DoH client for: $dohUrl")
            DohClient(dohUrl!!)
        } else {
            null
        }
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn() {
        try {
            android.util.Log.d("DnsVpnService", "Starting VPN with DNS: $primaryDns, $secondaryDns (DoH: $isDoH)")

            val builder = Builder()
                .setSession(serverName)
                .setMtu(VPN_MTU)

            // Try multiple VPN address prefixes (like the working DNS Changer app)
            var addressSet = false
            for (prefix in listOf("10.0.0", "192.0.2", "198.51.100", "203.0.113", "192.168.50")) {
                try {
                    builder.addAddress("$prefix.1", 24)  // Use /24 subnet like working app
                    addressSet = true
                    android.util.Log.d("DnsVpnService", "Using VPN address: $prefix.1/24")
                    break
                } catch (e: IllegalArgumentException) {
                    android.util.Log.w("DnsVpnService", "Address $prefix.1 not available, trying next")
                    continue
                }
            }

            if (!addressSet) {
                // Fallback to original address
                builder.addAddress(VPN_ADDRESS, 24)
                android.util.Log.d("DnsVpnService", "Using fallback VPN address: $VPN_ADDRESS/24")
            }

            // Add IPv6 address for full IPv6 support (like the working DNS Changer app)
            try {
                builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
                android.util.Log.d("DnsVpnService", "Added IPv6 VPN address")
            } catch (e: Exception) {
                android.util.Log.w("DnsVpnService", "Could not add IPv6 address: ${e.message}")
            }

            // CRITICAL: Use allowFamily() - this is what makes DNS work properly!
            // The working DNS Changer app uses this API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.allowFamily(OsConstants.AF_INET)
                builder.allowFamily(OsConstants.AF_INET6)
                android.util.Log.d("DnsVpnService", "Enabled allowFamily(AF_INET + AF_INET6)")
            }

            // Add IPv4 DNS servers
            builder.addDnsServer(primaryDns)
            if (secondaryDns != primaryDns) {
                builder.addDnsServer(secondaryDns)
            }
            android.util.Log.d("DnsVpnService", "Added IPv4 DNS servers: $primaryDns, $secondaryDns")

            // Add IPv6 DNS servers for the same provider to prevent IPv6 DNS leakage
            val ipv6DnsMap = mapOf(
                // Cloudflare
                "1.1.1.1" to "2606:4700:4700::1111",
                "1.0.0.1" to "2606:4700:4700::1001",
                // Google
                "8.8.8.8" to "2001:4860:4860::8888",
                "8.8.4.4" to "2001:4860:4860::8844",
                // Quad9
                "9.9.9.9" to "2620:fe::fe",
                "149.112.112.112" to "2620:fe::9",
                // OpenDNS
                "208.67.222.222" to "2620:119:35::35",
                "208.67.220.220" to "2620:119:53::53",
                // AdGuard
                "94.140.14.14" to "2a10:50c0::ad1:ff",
                "94.140.15.15" to "2a10:50c0::ad2:ff",
                // CleanBrowsing
                "185.228.168.9" to "2a0d:2a00:1::2",
                "185.228.169.9" to "2a0d:2a00:2::2",
                // Alternate DNS
                "76.76.19.19" to "2602:fcbc::ad",
                "76.223.122.150" to "2602:fcbc:2::ad",
                // DNS.Watch
                "84.200.69.80" to "2001:1608:10:25::1c04:b12f",
                "84.200.70.40" to "2001:1608:10:25::9249:d69b",
                // Yandex
                "77.88.8.8" to "2a02:6b8::feed:0ff",
                "77.88.8.1" to "2a02:6b8:0:1::feed:0ff",
                // Neustar UltraDNS
                "156.154.70.1" to "2610:a1:1018::1",
                "156.154.71.1" to "2610:a1:1019::1",
                // Comodo Secure DNS
                "8.26.56.26" to "2620:0:ccd::2",
                "8.20.247.20" to "2620:0:ccc::2"
            )

            ipv6DnsMap[primaryDns]?.let { ipv6Dns ->
                builder.addDnsServer(ipv6Dns)
                android.util.Log.d("DnsVpnService", "Added IPv6 DNS: $ipv6Dns")
            }
            ipv6DnsMap[secondaryDns]?.let { ipv6Dns ->
                if (ipv6Dns != ipv6DnsMap[primaryDns]) {
                    builder.addDnsServer(ipv6Dns)
                    android.util.Log.d("DnsVpnService", "Added IPv6 DNS: $ipv6Dns")
                }
            }

            // NOTE: The working app does NOT add routes for DNS servers!
            // Android's VPN DNS mechanism handles routing automatically when using allowFamily()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                isRunning = true
                isVpnRunning = true
                currentServerName = serverName
                android.util.Log.d("DnsVpnService", "VPN established successfully")
                startWorker()
            } else {
                android.util.Log.e("DnsVpnService", "Failed to establish VPN interface")
                stopVpn()
            }
        } catch (e: Exception) {
            android.util.Log.e("DnsVpnService", "Error starting VPN: ${e.message}")
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun startWorker() {
        workerThread = Thread {
            android.util.Log.d("DnsVpnService", "Worker thread starting...")
            // Cache vpnInterface locally to prevent race conditions
            val vpnFd = vpnInterface ?: run {
                android.util.Log.e("DnsVpnService", "VPN interface is null!")
                return@Thread
            }
            val buffer = ByteArray(VPN_MTU)
            val inputStream = FileInputStream(vpnFd.fileDescriptor)
            val outputStream = FileOutputStream(vpnFd.fileDescriptor)

            android.util.Log.d("DnsVpnService", "Worker thread ready, waiting for packets...")

            // Use only isRunning for loop control - vpnFd is cached locally
            while (isRunning) {
                try {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        android.util.Log.d("DnsVpnService", "Received packet: $length bytes")
                        handlePacket(buffer, length, outputStream)
                    } else if (length < 0) {
                        android.util.Log.w("DnsVpnService", "Read returned $length, stopping")
                        break
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        android.util.Log.e("DnsVpnService", "Read error: ${e.message}")
                    }
                    break
                }
            }
            android.util.Log.d("DnsVpnService", "Worker thread stopped")
        }
        workerThread?.start()
    }

    private fun handlePacket(buffer: ByteArray, length: Int, outputStream: FileOutputStream) {
        if (length < 28) return // Minimum IP + UDP header

        // Check IP version
        val version = (buffer[0].toInt() shr 4) and 0xF

        when (version) {
            4 -> handleIPv4Packet(buffer, length, outputStream)
            6 -> handleIPv6Packet(buffer, length, outputStream)
            else -> android.util.Log.d("DnsVpnService", "Ignoring unknown IP version: $version")
        }
    }

    private fun handleIPv4Packet(buffer: ByteArray, length: Int, outputStream: FileOutputStream) {
        val protocol = buffer[9].toInt() and 0xFF
        if (protocol != 17) {
            android.util.Log.d("DnsVpnService", "Ignoring non-UDP IPv4 packet, protocol=$protocol")
            return
        }

        val ipHeaderLength = (buffer[0].toInt() and 0xF) * 4
        if (length < ipHeaderLength + 8) return

        val destPort = ((buffer[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
                      (buffer[ipHeaderLength + 3].toInt() and 0xFF)

        // Extract IPs for logging
        val srcIp = "${buffer[12].toInt() and 0xFF}.${buffer[13].toInt() and 0xFF}.${buffer[14].toInt() and 0xFF}.${buffer[15].toInt() and 0xFF}"
        val dstIp = "${buffer[16].toInt() and 0xFF}.${buffer[17].toInt() and 0xFF}.${buffer[18].toInt() and 0xFF}.${buffer[19].toInt() and 0xFF}"

        android.util.Log.d("DnsVpnService", "IPv4 Packet: $srcIp -> $dstIp:$destPort (len=$length)")

        // Only handle DNS queries (port 53)
        if (destPort == 53) {
            android.util.Log.d("DnsVpnService", "IPv4 DNS query detected, forwarding...")
            forwardDnsQueryIPv4(buffer, length, ipHeaderLength, outputStream)
        }
    }

    private fun handleIPv6Packet(buffer: ByteArray, length: Int, outputStream: FileOutputStream) {
        // IPv6 header is fixed 40 bytes
        val ipv6HeaderLength = 40
        if (length < ipv6HeaderLength + 8) return

        // Next header field at byte 6 (0x11 = UDP)
        val nextHeader = buffer[6].toInt() and 0xFF
        if (nextHeader != 17) {
            android.util.Log.d("DnsVpnService", "Ignoring non-UDP IPv6 packet, nextHeader=$nextHeader")
            return
        }

        // Extract destination port from UDP header
        val destPort = ((buffer[ipv6HeaderLength + 2].toInt() and 0xFF) shl 8) or
                      (buffer[ipv6HeaderLength + 3].toInt() and 0xFF)

        android.util.Log.d("DnsVpnService", "IPv6 Packet: destPort=$destPort (len=$length)")

        // Only handle DNS queries (port 53)
        if (destPort == 53) {
            android.util.Log.d("DnsVpnService", "IPv6 DNS query detected, forwarding...")
            forwardDnsQueryIPv6(buffer, length, outputStream)
        }
    }

    private fun forwardDnsQueryIPv4(buffer: ByteArray, length: Int, ipHeaderLength: Int, outputStream: FileOutputStream) {
        try {
            val udpHeaderLength = 8
            val dnsStart = ipHeaderLength + udpHeaderLength
            if (length <= dnsStart) {
                android.util.Log.w("DnsVpnService", "Packet too short for DNS query")
                return
            }

            val dnsQuery = buffer.copyOfRange(dnsStart, length)
            android.util.Log.d("DnsVpnService", "Extracted IPv4 DNS query: ${dnsQuery.size} bytes, isDoH=$isDoH")

            // Choose DoH or traditional UDP DNS forwarding
            val response: ByteArray? = if (isDoH && dohClient != null) {
                forwardViaDoH(dnsQuery)
            } else {
                forwardViaUdp(dnsQuery)
            }

            // Send response back through VPN
            if (response != null) {
                android.util.Log.d("DnsVpnService", "Building IPv4 response packet for ${response.size} bytes DNS response")
                val responsePacket = buildDnsResponseIPv4(buffer, ipHeaderLength, response)
                synchronized(outputStream) {
                    outputStream.write(responsePacket)
                    outputStream.flush()
                }
                android.util.Log.d("DnsVpnService", "IPv4 Response packet written: ${responsePacket.size} bytes")
            } else {
                android.util.Log.e("DnsVpnService", "No DNS response received!")
            }
        } catch (e: Exception) {
            android.util.Log.e("DnsVpnService", "IPv4 DNS forward error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun forwardDnsQueryIPv6(buffer: ByteArray, length: Int, outputStream: FileOutputStream) {
        try {
            val ipv6HeaderLength = 40
            val udpHeaderLength = 8
            val dnsStart = ipv6HeaderLength + udpHeaderLength
            if (length <= dnsStart) {
                android.util.Log.w("DnsVpnService", "IPv6 packet too short for DNS query")
                return
            }

            val dnsQuery = buffer.copyOfRange(dnsStart, length)
            android.util.Log.d("DnsVpnService", "Extracted IPv6 DNS query: ${dnsQuery.size} bytes, isDoH=$isDoH")

            // Choose DoH or traditional UDP DNS forwarding
            val response: ByteArray? = if (isDoH && dohClient != null) {
                forwardViaDoH(dnsQuery)
            } else {
                forwardViaUdp(dnsQuery)
            }

            // Send response back through VPN
            if (response != null) {
                android.util.Log.d("DnsVpnService", "Building IPv6 response packet for ${response.size} bytes DNS response")
                val responsePacket = buildDnsResponseIPv6(buffer, response)
                synchronized(outputStream) {
                    outputStream.write(responsePacket)
                    outputStream.flush()
                }
                android.util.Log.d("DnsVpnService", "IPv6 Response packet written: ${responsePacket.size} bytes")
            } else {
                android.util.Log.e("DnsVpnService", "No DNS response received!")
            }
        } catch (e: Exception) {
            android.util.Log.e("DnsVpnService", "IPv6 DNS forward error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun forwardViaDoH(dnsQuery: ByteArray): ByteArray? {
        val response = dohClient?.query(dnsQuery)
        if (response != null) {
            android.util.Log.d("DnsVpnService", "DoH query successful")
            return response
        }

        // Fallback to UDP if DoH fails
        android.util.Log.w("DnsVpnService", "DoH failed, falling back to UDP DNS")
        return forwardViaUdp(dnsQuery)
    }

    private fun forwardViaUdp(dnsQuery: ByteArray): ByteArray? {
        android.util.Log.d("DnsVpnService", "forwardViaUdp: query size=${dnsQuery.size}, primary=$primaryDns, secondary=$secondaryDns")

        var response: ByteArray? = null
        var socket: DatagramSocket? = null

        try {
            // Create protected socket (bypasses VPN)
            socket = DatagramSocket()
            socket.soTimeout = 5000

            // CRITICAL: protect() must be called to bypass VPN routing
            val protectResult = protect(socket)
            android.util.Log.d("DnsVpnService", "Socket protected: $protectResult")

            if (!protectResult) {
                android.util.Log.e("DnsVpnService", "Failed to protect socket!")
                socket.close()
                return null
            }

            // Try primary DNS
            android.util.Log.d("DnsVpnService", "Sending DNS query to primary: $primaryDns")
            val dnsServer = InetAddress.getByName(primaryDns)
            val packet = DatagramPacket(dnsQuery, dnsQuery.size, dnsServer, 53)
            socket.send(packet)
            android.util.Log.d("DnsVpnService", "Query sent, waiting for response...")

            val responseBuffer = ByteArray(4096)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            response = responseBuffer.copyOfRange(0, responsePacket.length)
            android.util.Log.d("DnsVpnService", "Got DNS response: ${response.size} bytes")
        } catch (e: Exception) {
            android.util.Log.w("DnsVpnService", "Primary DNS failed: ${e.message}")
            // Try secondary DNS with a new protected socket
            socket?.close()

            try {
                socket = DatagramSocket()
                socket.soTimeout = 5000
                val protectResult = protect(socket)
                android.util.Log.d("DnsVpnService", "Secondary socket protected: $protectResult")

                if (!protectResult) {
                    android.util.Log.e("DnsVpnService", "Failed to protect secondary socket!")
                    socket.close()
                    return null
                }

                android.util.Log.d("DnsVpnService", "Sending DNS query to secondary: $secondaryDns")
                val dnsServer = InetAddress.getByName(secondaryDns)
                val packet = DatagramPacket(dnsQuery, dnsQuery.size, dnsServer, 53)
                socket.send(packet)
                android.util.Log.d("DnsVpnService", "Query sent to secondary, waiting for response...")

                val responseBuffer = ByteArray(4096)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(responsePacket)
                response = responseBuffer.copyOfRange(0, responsePacket.length)
                android.util.Log.d("DnsVpnService", "Got DNS response from secondary: ${response.size} bytes")
            } catch (e2: Exception) {
                android.util.Log.e("DnsVpnService", "Secondary DNS failed: ${e2.message}")
            }
        } finally {
            socket?.close()
        }
        return response
    }

    private fun buildDnsResponseIPv4(originalPacket: ByteArray, ipHeaderLength: Int, dnsResponse: ByteArray): ByteArray {
        val udpHeaderLength = 8
        val totalLength = ipHeaderLength + udpHeaderLength + dnsResponse.size
        val response = ByteArray(totalLength)

        // Copy original IP header
        System.arraycopy(originalPacket, 0, response, 0, ipHeaderLength)

        // Swap source and destination IP addresses
        System.arraycopy(originalPacket, 16, response, 12, 4) // dest -> source
        System.arraycopy(originalPacket, 12, response, 16, 4) // source -> dest

        // Update IP header
        response[2] = ((totalLength shr 8) and 0xFF).toByte() // Total length
        response[3] = (totalLength and 0xFF).toByte()
        response[8] = 64 // TTL

        // Clear checksum before computing
        response[10] = 0
        response[11] = 0

        // Compute IP header checksum
        var sum = 0
        for (i in 0 until ipHeaderLength step 2) {
            val word = ((response[i].toInt() and 0xFF) shl 8) or (response[i + 1].toInt() and 0xFF)
            sum += word
        }
        // Fold 32-bit sum to 16 bits
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        response[10] = ((checksum shr 8) and 0xFF).toByte()
        response[11] = (checksum and 0xFF).toByte()

        // Swap UDP ports
        response[ipHeaderLength] = originalPacket[ipHeaderLength + 2] // dest -> source
        response[ipHeaderLength + 1] = originalPacket[ipHeaderLength + 3]
        response[ipHeaderLength + 2] = originalPacket[ipHeaderLength] // source -> dest
        response[ipHeaderLength + 3] = originalPacket[ipHeaderLength + 1]

        // UDP length
        val udpLength = udpHeaderLength + dnsResponse.size
        response[ipHeaderLength + 4] = ((udpLength shr 8) and 0xFF).toByte()
        response[ipHeaderLength + 5] = (udpLength and 0xFF).toByte()

        // UDP checksum (0 = disabled for IPv4)
        response[ipHeaderLength + 6] = 0
        response[ipHeaderLength + 7] = 0

        // DNS response data
        System.arraycopy(dnsResponse, 0, response, ipHeaderLength + udpHeaderLength, dnsResponse.size)

        return response
    }

    private fun buildDnsResponseIPv6(originalPacket: ByteArray, dnsResponse: ByteArray): ByteArray {
        val ipv6HeaderLength = 40
        val udpHeaderLength = 8
        val totalLength = ipv6HeaderLength + udpHeaderLength + dnsResponse.size
        val response = ByteArray(totalLength)

        // Copy original IPv6 header
        System.arraycopy(originalPacket, 0, response, 0, ipv6HeaderLength)

        // Swap source and destination IPv6 addresses (16 bytes each)
        // Source is at offset 8, destination is at offset 24
        System.arraycopy(originalPacket, 24, response, 8, 16)  // dest -> source
        System.arraycopy(originalPacket, 8, response, 24, 16)  // source -> dest

        // Update payload length (bytes 4-5)
        val payloadLength = udpHeaderLength + dnsResponse.size
        response[4] = ((payloadLength shr 8) and 0xFF).toByte()
        response[5] = (payloadLength and 0xFF).toByte()

        // Hop limit
        response[7] = 64

        // Swap UDP ports
        response[ipv6HeaderLength] = originalPacket[ipv6HeaderLength + 2] // dest -> source
        response[ipv6HeaderLength + 1] = originalPacket[ipv6HeaderLength + 3]
        response[ipv6HeaderLength + 2] = originalPacket[ipv6HeaderLength] // source -> dest
        response[ipv6HeaderLength + 3] = originalPacket[ipv6HeaderLength + 1]

        // UDP length
        val udpLength = udpHeaderLength + dnsResponse.size
        response[ipv6HeaderLength + 4] = ((udpLength shr 8) and 0xFF).toByte()
        response[ipv6HeaderLength + 5] = (udpLength and 0xFF).toByte()

        // UDP checksum - required for IPv6, compute it
        response[ipv6HeaderLength + 6] = 0
        response[ipv6HeaderLength + 7] = 0

        // Copy DNS response data
        System.arraycopy(dnsResponse, 0, response, ipv6HeaderLength + udpHeaderLength, dnsResponse.size)

        // Compute UDP checksum for IPv6 (required, unlike IPv4)
        val checksumValue = computeUdpChecksumIPv6(response, ipv6HeaderLength, udpLength)
        response[ipv6HeaderLength + 6] = ((checksumValue shr 8) and 0xFF).toByte()
        response[ipv6HeaderLength + 7] = (checksumValue and 0xFF).toByte()

        return response
    }

    private fun computeUdpChecksumIPv6(packet: ByteArray, udpOffset: Int, udpLength: Int): Int {
        var sum = 0L

        // Pseudo-header: source address (16 bytes)
        for (i in 8 until 24 step 2) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
        }

        // Pseudo-header: destination address (16 bytes)
        for (i in 24 until 40 step 2) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
        }

        // Pseudo-header: UDP length (32-bit)
        sum += udpLength

        // Pseudo-header: next header (UDP = 17)
        sum += 17

        // UDP header and data
        for (i in udpOffset until udpOffset + udpLength step 2) {
            if (i + 1 < packet.size) {
                sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            } else {
                sum += (packet[i].toInt() and 0xFF) shl 8
            }
        }

        // Fold 32-bit sum to 16 bits
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        val checksum = sum.inv().toInt() and 0xFFFF
        return if (checksum == 0) 0xFFFF else checksum
    }

    private fun stopWorker() {
        val thread = workerThread
        workerThread = null
        // Note: Thread is blocked on I/O read, so interrupt alone won't work.
        // The VPN interface must be closed first (done in stopVpn/closeVpnInterface)
        // which causes the read to fail and the thread to exit naturally.
        thread?.interrupt()
        // Wait briefly for thread to finish (with timeout to prevent blocking)
        try {
            thread?.join(1000)
        } catch (e: InterruptedException) {
            android.util.Log.w("DnsVpnService", "Interrupted while waiting for worker thread")
        }
    }

    private fun closeVpnInterface() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            android.util.Log.w("DnsVpnService", "Error closing VPN interface: ${e.message}")
        }
    }

    private fun stopVpn() {
        android.util.Log.d("DnsVpnService", "Stopping VPN")
        isRunning = false
        isVpnRunning = false
        currentServerName = null
        // Close VPN interface BEFORE stopping worker - this causes read() to fail
        // and allows the worker thread to exit cleanly
        closeVpnInterface()
        stopWorker()
        dohClient?.shutdown()
        dohClient = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        android.util.Log.d("DnsVpnService", "VPN stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, DnsVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, serverName))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.disconnect), stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
