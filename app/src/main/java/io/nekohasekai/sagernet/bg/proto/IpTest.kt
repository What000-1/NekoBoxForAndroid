package io.nekohasekai.sagernet.bg.proto

import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.ktx.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetAddress
import java.util.Locale

data class IpInfo(val ip: String, val country: String)

class IpTest {

    private val timeout = DataStore.connectionTestTimeout

    suspend fun doTest(profile: ProxyEntity): String {
        return withContext(Dispatchers.IO) {
            // 1. Get Inbound (transit) IP & Geo
            val inboundAddress = try {
                profile.requireBean().serverAddress
            } catch (e: Exception) {
                "" // Fallback if no server address available
            }
            var inboundIp = inboundAddress
            try {
                // Resolve domain to IP if needed
                inboundIp = InetAddress.getByName(inboundAddress).hostAddress ?: inboundAddress
            } catch (e: Exception) {
                Logs.d("IpTest: Failed to resolve inbound address: $inboundAddress, ${e.message}")
            }

            // Get geo info for inbound IP - use ipinfo.io with the resolved IP
            // This is a direct request (not through proxy) to look up the transit server's geo
            val inboundInfo = getIpGeoInfo(inboundIp)

            // 2. Get Outbound (exit) IP & Geo through proxy
            val outboundJsonString = try {
                TestInstance(profile, "", timeout).doIpTest()
            } catch (e: Exception) {
                Logs.d("IpTest: Outbound test failed: ${e.message}")
                return@withContext "测试失败: ${e.message}"
            }

            val outboundInfo = parseIpInfo(outboundJsonString) 
                ?: return@withContext "出口信息解析失败"

            // 3. Format result
            formatTransitResult(inboundInfo, outboundInfo)
        }
    }

    /**
     * Get geo info for an IP address.
     * For the transit/inbound IP, we just need to know the country code.
     * We resolve it from the IP address itself using ipinfo.io.
     * Falls back to just the IP with "Unknown" country if the lookup fails.
     */
    private fun getIpGeoInfo(ip: String): IpInfo {
        if (ip.isEmpty()) return IpInfo("", "Unknown")

        try {
            // Use HTTPS instead of HTTP for better reliability
            val url = java.net.URL("https://ipinfo.io/$ip/json")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "curl/7.74.0")
            try {
                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val info = parseIpInfo(jsonStr)
                    if (info != null) return info
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Logs.d("IpTest: Failed to get geo info for $ip: ${e.message}")
        }

        // Fallback: return IP with unknown country
        return IpInfo(ip, "Unknown")
    }

    private fun parseIpInfo(jsonString: String): IpInfo? {
        try {
            val json = JSONObject(jsonString)
            val ip = json.optString("ip", "")
            val countryCode = json.optString("country", "")
            if (ip.isNotEmpty()) {
                val countryName = if (countryCode.isNotEmpty()) {
                    try {
                        Locale("", countryCode).displayCountry
                    } catch (e: Exception) {
                        countryCode
                    }
                } else {
                    "未知"
                }
                return IpInfo(ip, countryName)
            }
        } catch (e: Exception) {
            Logs.d("IpTest: Failed to parse IP info: ${e.message}")
        }
        return null
    }

    private fun formatTransitResult(inbound: IpInfo, outbound: IpInfo): String {
        val inboundDisplay = if (inbound.ip.isNotEmpty()) {
            "${inbound.ip} (${inbound.country})"
        } else {
            "Unknown"
        }
        val outboundDisplay = "${outbound.ip} (${outbound.country})"

        return if (inbound.ip == outbound.ip) {
            // Direct connection (same IP)
            "出口: $outboundDisplay\n入口: 直连"
        } else {
            // Transit detected
            "出口: $outboundDisplay\n入口: $inboundDisplay"
        }
    }
}
