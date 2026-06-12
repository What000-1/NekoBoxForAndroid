package moe.matsuri.nb4a.ui

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ui.ThemedActivity
import libcore.Libcore
import moe.matsuri.nb4a.utils.Util
import org.json.JSONObject
import java.io.File

class ExternalCoreUpdateActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "Updating External Cores...", Toast.LENGTH_SHORT).show()
        
        runOnDefaultDispatcher {
            try {
                updateCore("mihomo", DataStore.mihomoRepo)
                updateCore("xray", DataStore.xrayRepo)
                onMainDispatcher {
                    Toast.makeText(this@ExternalCoreUpdateActivity, "External Cores Updated Successfully", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onMainDispatcher {
                    Toast.makeText(this@ExternalCoreUpdateActivity, "Failed to update external cores: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private suspend fun updateCore(coreName: String, repo: String) {
        val client = Libcore.newHttpClient().apply {
            modernTLS()
            keepAlive()
            trySocks5(DataStore.mixedPort)
        }

        try {
            val cleanRepo = repo.replace(Regex(".*github\\.com/"), "").substringBefore(".git").trim('/').split("/").take(2).joinToString("/")
            var response = client.newRequest().apply {
                setURL("https://api.github.com/repos/$cleanRepo/releases/latest")
            }.execute()

            val release = JSONObject(Util.getStringBox(response.contentString))
            val releaseAssets = release.getJSONArray("assets").let { array ->
                (0 until array.length()).map { array.getJSONObject(it) }
            }

            val abi = Build.SUPPORTED_ABIS[0]
            val archMatch = when {
                abi.contains("arm64") -> listOf("arm64", "aarch64")
                abi.contains("armeabi-v7a") -> listOf("armv7", "arm")
                abi.contains("x86_64") -> listOf("amd64", "x86_64", "x64")
                abi.contains("x86") -> listOf("386", "x86")
                else -> listOf("arm64") // fallback
            }

            // Find an asset matching OS (android or linux) and arch
            val assetToDownload = releaseAssets.find { asset ->
                val name = asset.getString("name").lowercase()
                val isAndroidOrLinux = name.contains("android") || name.contains("linux")
                val isArchMatch = archMatch.any { name.contains(it) }
                val isCorrectCore = name.contains(coreName) || name.contains("core")
                isAndroidOrLinux && isArchMatch && isCorrectCore
            } ?: error("No matching binary found for $coreName on ABI $abi in release ${release.optString("tag_name")}")

            val browserDownloadUrl = assetToDownload.getString("browser_download_url")

            response = client.newRequest().apply {
                setURL(browserDownloadUrl)
            }.execute()

            val cacheFile = File(app.filesDir, "${coreName}.tmp")
            val targetFile = File(app.filesDir, coreName)
            
            response.writeTo(cacheFile.canonicalPath)

            val assetName = assetToDownload.getString("name")
            if (assetName.endsWith(".gz")) {
                val extracted = File(app.filesDir, "${coreName}_extracted")
                java.util.zip.GZIPInputStream(cacheFile.inputStream()).use { input ->
                    extracted.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                cacheFile.delete()
                extracted.renameTo(targetFile)
            } else if (assetName.endsWith(".zip")) {
                java.util.zip.ZipInputStream(cacheFile.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == coreName || entry.name == "xray" || entry.name == "mihomo") {
                            targetFile.outputStream().use { output ->
                                zis.copyTo(output)
                            }
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
                cacheFile.delete()
            } else {
                cacheFile.renameTo(targetFile)
            }

            targetFile.setExecutable(true, false)

        } finally {
            client.close()
        }
    }
}
