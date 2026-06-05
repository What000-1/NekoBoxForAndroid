package io.nekohasekai.sagernet.bg.proto

import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.Locale

data class IpInfo(val ip: String, val country: String)

class IpTest {

    private val timeout = DataStore.connectionTestTimeout

    suspend fun doTest(profile: ProxyEntity): String {
        return withContext(Dispatchers.IO) {
            // 1. Get Inbound IP & Geo
            val inboundAddress = try {
                profile.requireBean().serverAddress
            } catch (e: Exception) {
                "" // Fallback if no server address available
            }
            var inboundIp = inboundAddress
            try {
                inboundIp = InetAddress.getByName(inboundAddress).hostAddress ?: inboundAddress
            } catch (e: Exception) {
                // Ignore resolution error
            }
            val inboundInfo = getIpInfoDirect(inboundIp) ?: IpInfo(inboundIp, "Unknown")
            
            // 2. Get Outbound IP & Geo through proxy
            val outboundJsonString = try {
                TestInstance(profile, "", timeout).doIpTest()
            } catch (e: Exception) {
                return@withContext "测试失败: ${e.message}"
            }
            
            val outboundInfo = parseIpInfo(outboundJsonString) ?: return@withContext "出口信息解析失败"

            // 3. Format result
            formatTransitResult(inboundInfo, outboundInfo)
        }
    }

    private fun getIpInfoDirect(ip: String): IpInfo? {
        try {
            val url = URL("http://ipinfo.io/$ip/json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                return parseIpInfo(jsonStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseIpInfo(jsonString: String): IpInfo? {
        try {
            val json = JSONObject(jsonString)
            val ip = json.optString("ip", "")
            val countryCode = json.optString("country", "")
            if (ip.isNotEmpty()) {
                val countryName = if (countryCode.isNotEmpty()) Locale("", countryCode).displayCountry else "未知"
                return IpInfo(ip, countryName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun formatTransitResult(inbound: IpInfo, outbound: IpInfo): String {
        return if (inbound.ip == outbound.ip || (inbound.country == outbound.country && inbound.country != "未知")) {
            // No transit detected or same country
            "路由: ${inbound.country}(直连)\nIP链路: ${inbound.ip} -> ${outbound.ip}"
        } else {
            // Transit detected
            "路由: ${inbound.country}(入站) → 中转 → ${outbound.country}(出口)\nIP链路: ${inbound.ip} → ... → ${outbound.ip}"
        }
    }
}
