package com.aire.data

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import com.aire.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    var releaseTag: String = "" // Added to keep track of tag
)

/**
 * Handles checking for updates from GitHub and managing the download/install process.
 */
class UpdateManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val repoUrl = "https://api.github.com/repos/TECWiSaRd/Aire/releases/latest"

    suspend fun checkForUpdate(): GitHubAsset? = withContext(Dispatchers.IO) {
        try {
            val response = URL(repoUrl).readText()
            val release = json.decodeFromString<GitHubRelease>(response)
            
            // Strictly compare versions (assuming "vX.Y.Z" format)
            val latestVersion = release.tag_name.removePrefix("v")
            if (isNewer(latestVersion, "${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}")) {
                return@withContext release.assets.find { it.name.endsWith(".apk") }?.apply { 
                    releaseTag = release.tag_name 
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to check for updates", e)
        }
        null
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until minOf(latestParts.size, currentParts.size)) {
            if (latestParts[i] > currentParts[i]) return true
            if (latestParts[i] < currentParts[i]) return false
        }
        return latestParts.size > currentParts.size
    }

    fun downloadAndInstall(asset: GitHubAsset) {
        val extension = if (asset.name.endsWith(".apk")) "" else ".apk"
        val fileName = "aire-update-${asset.releaseTag}-${asset.name}$extension"
        val request = DownloadManager.Request(asset.browser_download_url.toUri())
            .setTitle("Aire Update")
            .setDescription("Downloading ${asset.name}...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            @SuppressLint("Range")
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val query = DownloadManager.Query().setFilterById(id)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            installApk(id)
                            context.unregisterReceiver(this)
                        }
                    }
                    cursor.close()
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, filter)
        }
    }

    @SuppressLint("Range")
    private fun installApk(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId)
        
        if (uri != null) {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        }
    }
}
