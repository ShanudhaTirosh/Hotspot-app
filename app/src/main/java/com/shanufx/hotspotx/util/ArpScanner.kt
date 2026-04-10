package com.shanufx.hotspotx.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader

/**
 * Parses /proc/net/arp to discover devices on the local network.
 * This file is world-readable on all Android versions (no root required).
 *
 * ARP table format:
 * IP address       HW type     Flags       HW address          Mask     Device
 * 192.168.43.100   0x1         0x2         aa:bb:cc:dd:ee:ff   *        wlan0
 */
object ArpScanner {

    data class ArpEntry(
        val ip: String,
        val mac: String,
        val device: String,
        val flags: String
    )

    suspend fun scan(): List<ArpEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ArpEntry>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.readLine() // skip header
                var line = reader.readLine()
                while (line != null) {
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 6) {
                        val ip = parts[0]
                        val flags = parts[2]
                        val mac = parts[3]
                        val iface = parts[5]
                        // flags 0x2 = completed/reachable, 0x0 = incomplete
                        if (mac != "00:00:00:00:00:00" && flags != "0x0") {
                            entries += ArpEntry(ip = ip, mac = mac, device = iface, flags = flags)
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (_: Exception) {}
        entries
    }

    /** Best-effort hostname resolution via reverse DNS (may time out quickly) */
    suspend fun resolveHostname(ip: String): String? = withContext(Dispatchers.IO) {
        try {
            val addr = java.net.InetAddress.getByName(ip)
            val hostname = addr.canonicalHostName
            if (hostname == ip) null else hostname
        } catch (_: Exception) {
            null
        }
    }

    /** Heuristic device type from hostname or MAC OUI */
    fun guessDeviceType(hostname: String?, mac: String): String {
        val host = hostname?.lowercase() ?: ""
        return when {
            host.contains("iphone") || host.contains("android") || host.contains("phone") -> "phone"
            host.contains("ipad") || host.contains("tablet") -> "tablet"
            host.contains("macbook") || host.contains("laptop") || host.contains("pc") ||
            host.contains("desktop") || host.contains("windows") || host.contains("linux") -> "laptop"
            host.contains("tv") || host.contains("roku") || host.contains("firetv") -> "tv"
            else -> guessFromOui(mac)
        }
    }

    /** Very basic OUI lookup for common vendors */
    private fun guessFromOui(mac: String): String {
        val oui = mac.take(8).uppercase()
        return when {
            oui.startsWith("00:1A:11") || oui.startsWith("FC:3F:7C") -> "phone" // Google
            oui.startsWith("B8:27:EB") || oui.startsWith("DC:A6:32") -> "laptop" // RPi
            oui.startsWith("AC:DE:48") || oui.startsWith("00:03:93") -> "phone" // Apple
            else -> "unknown"
        }
    }
}
