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
    val browser_download_url: String
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
            
            // Simple version check: if tag name doesn't match current versionName
            if (release.tag_name != "v${BuildConfig.VERSION_NAME}") {
                return@withContext release.assets.find { it.name.endsWith(".apk") }
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to check for updates", e)
        }
        null
    }

    fun downloadAndInstall(asset: GitHubAsset) {
        val request = DownloadManager.Request(asset.browser_download_url.toUri())
            .setTitle("Aire Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "aire-update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(downloadId)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, filter)
        }
    }

    @SuppressLint("Range")
    private fun installApk(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            if (uriString != null) {
                val uri = Uri.parse(uriString)
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(installIntent)
            }
        }
        cursor.close()
    }
}
