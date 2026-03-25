package com.veteranop.cellfire

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

object AppUpdater {

    private const val TAG = "AppUpdater"
    private const val RELEASES_API =
        "https://api.github.com/repos/veteranop/Cellfire_app_public/releases/latest"
    private const val APK_FILENAME = "cellfire-update.apk"

    data class UpdateInfo(
        val tagName: String,
        val downloadUrl: String,
        val isNewer: Boolean
    )

    /**
     * Hits the GitHub Releases API and compares the latest tag to the running version.
     * Returns null on network failure. Returns UpdateInfo with isNewer=false if already current.
     */
    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val conn = URL(RELEASES_API).openConnection()
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "CellfireApp/$currentVersionName")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val text = conn.getInputStream().bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val tagName = json.getString("tag_name")          // e.g. "v1.0.1.7"
                val assets = json.getJSONArray("assets")
                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                if (downloadUrl.isEmpty()) return@withContext null
                // Strip leading 'v' for comparison: "v1.0.1.7" → "1.0.1.7"
                val remoteVersion = tagName.removePrefix("v")
                val isNewer = remoteVersion != currentVersionName
                UpdateInfo(tagName, downloadUrl, isNewer)
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed: ${e.message}")
                null
            }
        }

    /**
     * Downloads the APK to cache and fires the system installer.
     * Returns true if the installer was launched successfully.
     */
    suspend fun downloadAndInstall(context: Context, downloadUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val apkFile = File(context.cacheDir, APK_FILENAME)
                URL(downloadUrl).openStream().use { input ->
                    apkFile.outputStream().use { output -> input.copyTo(output) }
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    apkFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Download/install failed: ${e.message}")
                false
            }
        }
}
