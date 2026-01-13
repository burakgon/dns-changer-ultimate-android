package com.dns.changer.ultimate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
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

                // CRITICAL: Call startForeground immediately to avoid ForegroundServiceDidNotStartInTimeException
                // On Android 12+, this must happen within 10 seconds of startForegroundService()
                serverName = newServerName
                startForeground(NOTIFICATION_ID, createNotification())

                if (isRunning) {
                    android.util.Log.d("DnsVpnService", "Switching DNS from $serverName to $newServerName")
                    primaryDns = newPrimaryDns
                    secondaryDns = newSecondaryDns
                    currentServerName = serverName

                    // Restart VPN to apply new DNS
                    stopWorker()
                    closeVpnInterface()
                    startVpn()
                } else {
                    primaryDns = newPrimaryDns
                    secondaryDns = newSecondaryDns
                    startVpn()
                }
            }
            ACTION_STOP -> {
                stopVpn()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn() {
        try {
            val builder = Builder()
                .setSession(serverName)
                .setMtu(VPN_MTU)
                .addAddress(VPN_ADDRESS, 32)
                .addDnsServer(primaryDns)
                .addDnsServer(secondaryDns)
                // Only route DNS traffic (to specific DNS IP addresses)
                // This allows all other traffic to bypass the VPN
                .addRoute(primaryDns, 32)
                .addRoute(secondaryDns, 32)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                isRunning = true
                isVpnRunning = true
                currentServerName = serverName
                android.util.Log.d("DnsVpnService", "VPN started with DNS: $primaryDns, $secondaryDns")
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
            val vpnFd = vpnInterface ?: return@Thread
            val buffer = ByteArray(VPN_MTU)
            val inputStream = FileInputStream(vpnFd.fileDescriptor)
            val outputStream = FileOutputStream(vpnFd.fileDescriptor)

            while (isRunning && vpnInterface != null) {
                try {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        handlePacket(buffer, length, outputStream)
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
        if (version != 4) return // IPv4 only

        val protocol = buffer[9].toInt() and 0xFF
        if (protocol != 17) return // UDP only

        val ipHeaderLength = (buffer[0].toInt() and 0xF) * 4
        if (length < ipHeaderLength + 8) return

        val destPort = ((buffer[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
                      (buffer[ipHeaderLength + 3].toInt() and 0xFF)

        // Only handle DNS queries (port 53)
        if (destPort == 53) {
            forwardDnsQuery(buffer, length, ipHeaderLength, outputStream)
        }
    }

    private fun forwardDnsQuery(buffer: ByteArray, length: Int, ipHeaderLength: Int, outputStream: FileOutputStream) {
        try {
            val udpHeaderLength = 8
            val dnsStart = ipHeaderLength + udpHeaderLength
            if (length <= dnsStart) return

            val dnsQuery = buffer.copyOfRange(dnsStart, length)

            // Create protected socket (bypasses VPN)
            val socket = DatagramSocket()
            socket.soTimeout = 5000
            protect(socket)

            // Try primary DNS
            var response: ByteArray? = null
            try {
                val dnsServer = InetAddress.getByName(primaryDns)
                val packet = DatagramPacket(dnsQuery, dnsQuery.size, dnsServer, 53)
                socket.send(packet)

                val responseBuffer = ByteArray(4096)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(responsePacket)
                response = responseBuffer.copyOfRange(0, responsePacket.length)
            } catch (e: Exception) {
                android.util.Log.w("DnsVpnService", "Primary DNS failed: ${e.message}")
                // Try secondary DNS
                try {
                    val dnsServer = InetAddress.getByName(secondaryDns)
                    val packet = DatagramPacket(dnsQuery, dnsQuery.size, dnsServer, 53)
                    socket.send(packet)

                    val responseBuffer = ByteArray(4096)
                    val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                    socket.receive(responsePacket)
                    response = responseBuffer.copyOfRange(0, responsePacket.length)
                } catch (e2: Exception) {
                    android.util.Log.e("DnsVpnService", "Secondary DNS failed: ${e2.message}")
                }
            }
            socket.close()

            // Send response back through VPN
            if (response != null) {
                val responsePacket = buildDnsResponse(buffer, ipHeaderLength, response)
                synchronized(outputStream) {
                    outputStream.write(responsePacket)
                    outputStream.flush()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DnsVpnService", "DNS forward error: ${e.message}")
        }
    }

    private fun buildDnsResponse(originalPacket: ByteArray, ipHeaderLength: Int, dnsResponse: ByteArray): ByteArray {
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
        response[10] = 0 // Header checksum (0 for simplicity)
        response[11] = 0

        // Swap UDP ports
        response[ipHeaderLength] = originalPacket[ipHeaderLength + 2] // dest -> source
        response[ipHeaderLength + 1] = originalPacket[ipHeaderLength + 3]
        response[ipHeaderLength + 2] = originalPacket[ipHeaderLength] // source -> dest
        response[ipHeaderLength + 3] = originalPacket[ipHeaderLength + 1]

        // UDP length
        val udpLength = udpHeaderLength + dnsResponse.size
        response[ipHeaderLength + 4] = ((udpLength shr 8) and 0xFF).toByte()
        response[ipHeaderLength + 5] = (udpLength and 0xFF).toByte()

        // UDP checksum (0 = disabled)
        response[ipHeaderLength + 6] = 0
        response[ipHeaderLength + 7] = 0

        // DNS response data
        System.arraycopy(dnsResponse, 0, response, ipHeaderLength + udpHeaderLength, dnsResponse.size)

        return response
    }

    private fun stopWorker() {
        workerThread?.interrupt()
        workerThread = null
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
        stopWorker()
        closeVpnInterface()
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
