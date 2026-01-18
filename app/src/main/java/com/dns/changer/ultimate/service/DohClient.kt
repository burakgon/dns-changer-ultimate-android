package com.dns.changer.ultimate.service

import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * DNS over HTTPS client for making encrypted DNS queries.
 * Implements RFC 8484 - DNS Queries over HTTPS (DoH)
 */
class DohClient(
    private val dohUrl: String
) {
    companion object {
        private val DNS_MESSAGE_TYPE = "application/dns-message".toMediaType()
        private const val TIMEOUT_SECONDS = 5L

        // Pre-resolved IPs for common DoH providers to avoid circular DNS dependency
        private val DOH_SERVER_IPS = mapOf(
            "cloudflare-dns.com" to listOf("104.16.248.249", "104.16.249.249"),
            "dns.cloudflare.com" to listOf("104.16.248.249", "104.16.249.249"),
            "one.one.one.one" to listOf("1.1.1.1", "1.0.0.1"),
            "dns.google" to listOf("8.8.8.8", "8.8.4.4"),
            "dns.quad9.net" to listOf("9.9.9.9", "149.112.112.112"),
            "doh.opendns.com" to listOf("208.67.222.222", "208.67.220.220"),
            "dns.adguard.com" to listOf("94.140.14.14", "94.140.15.15"),
            "dns.nextdns.io" to listOf("45.90.28.0", "45.90.30.0")
        )
    }

    // Custom DNS resolver that uses pre-resolved IPs to avoid VPN circular dependency
    private val customDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val preResolved = DOH_SERVER_IPS[hostname.lowercase()]
            if (preResolved != null) {
                android.util.Log.d("DohClient", "Using pre-resolved IP for $hostname")
                return preResolved.map { InetAddress.getByName(it) }
            }
            // Fallback to system DNS for unknown hosts
            android.util.Log.d("DohClient", "Using system DNS for $hostname")
            return Dns.SYSTEM.lookup(hostname)
        }
    }

    private val client = OkHttpClient.Builder()
        .dns(customDns)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Send a DNS query over HTTPS using POST method (RFC 8484)
     *
     * @param dnsQuery Raw DNS query bytes (from intercepted UDP packet)
     * @return DNS response bytes or null on failure
     */
    fun query(dnsQuery: ByteArray): ByteArray? {
        return try {
            val request = Request.Builder()
                .url(dohUrl)
                .post(dnsQuery.toRequestBody(DNS_MESSAGE_TYPE))
                .header("Accept", "application/dns-message")
                .header("Content-Type", "application/dns-message")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    android.util.Log.w("DohClient", "DoH query failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DohClient", "DoH query error: ${e.message}")
            null
        }
    }

    /**
     * Shutdown the client and release resources synchronously.
     * Call this from a background thread or coroutine to avoid blocking the main thread.
     */
    fun shutdown() {
        try {
            // Shutdown dispatcher to stop accepting new requests
            client.dispatcher.executorService.shutdown()
            // Evict idle connections from the pool
            client.connectionPool.evictAll()
            // Close cache if present
            client.cache?.close()
        } catch (e: Exception) {
            android.util.Log.w("DohClient", "Shutdown error: ${e.message}")
        }
    }
}
