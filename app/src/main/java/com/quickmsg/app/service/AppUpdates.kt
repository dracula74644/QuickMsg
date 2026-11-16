package com.quickmsg.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.*
import com.quickmsg.app.MainActivity
import com.quickmsg.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

data class AppUpdate(val versionCode: Long, val versionName: String, val apkUrl: String)

object AppUpdates {
    const val MANIFEST_URL = "https://github.com/dracula74644/QuickMsg/releases/latest/download/update.json"
    private const val CHANNEL = "app_updates"
    private const val NOTIFICATION_ID = 1002
    private val mutex = Mutex()
    private val available = MutableStateFlow<AppUpdate?>(null)
    val update = available.asStateFlow()

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "github-release-check", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AppUpdateWorker>(6, TimeUnit.HOURS)
                .setInitialDelay(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )
    }

    internal fun parseManifest(json: String, installedVersion: Long): AppUpdate? {
        val data = JSONObject(json)
        val code = data.getLong("versionCode")
        val name = data.getString("versionName")
        val url = data.getString("apkUrl")
        val uri = URI(url)
        require(code > 0 && name.isNotBlank() && name.length <= 40)
        require(uri.scheme == "https" && uri.host == "github.com" && uri.userInfo == null &&
            uri.port == -1 && uri.query == null && uri.fragment == null &&
            uri.path.startsWith("/dracula74644/QuickMsg/releases/download/") &&
            uri.path.endsWith(".apk") && uri.normalize() == uri)
        return if (code > installedVersion) AppUpdate(code, name, url) else null
    }

    suspend fun check(context: Context, force: Boolean = false): AppUpdate? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
            val installed = PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(context.packageName, 0))
            available.value = prefs.getString("manifest", null)?.let {
                runCatching { parseManifest(it, installed) }.getOrNull()
            }
            if (available.value == null) {
                context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
            }
            val age = System.currentTimeMillis() - prefs.getLong("last_check", 0)
            if (!force && age in 0 until TimeUnit.HOURS.toMillis(1)) return@withLock available.value
            val connection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.useCaches = false
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode != 200) throw IOException("Update check failed")
                val json = connection.inputStream.bufferedReader().use { reader ->
                    val buffer = CharArray(16_385)
                    var count = 0
                    while (count < buffer.size) {
                        val read = reader.read(buffer, count, buffer.size - count)
                        if (read < 0) break
                        count += read
                    }
                    require(count < buffer.size) { "Update manifest too large" }
                    String(buffer, 0, count)
                }
                val result = parseManifest(json, installed)
                prefs.edit().putString("manifest", json)
                    .putLong("last_check", System.currentTimeMillis()).apply()
                available.value = result
                if (result == null) context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
                result
            } finally {
                connection.disconnect()
            }
        }
    }

    fun download(context: Context, update: AppUpdate) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(context, "Install a browser to download the update", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun notifyAvailable(context: Context, update: AppUpdate) {
        val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
        if (prefs.getLong("notified_version", 0) >= update.versionCode) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "App updates", NotificationManager.IMPORTANCE_DEFAULT))
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!manager.areNotificationsEnabled() || manager.getNotificationChannel(CHANNEL).importance == NotificationManager.IMPORTANCE_NONE) return
        val intent = PendingIntent.getActivity(context, NOTIFICATION_ID,
            Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            manager.notify(NOTIFICATION_ID, NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("QuickMsg ${update.versionName} is available")
                .setContentText("Tap to open QuickMsg and download the update.")
                .setContentIntent(intent).setAutoCancel(true).setOnlyAlertOnce(true).build())
            prefs.edit().putLong("notified_version", update.versionCode).apply()
        } catch (_: SecurityException) { /* Permission changed while posting. */ }
    }
}

class AppUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        AppUpdates.check(applicationContext)?.let { AppUpdates.notifyAvailable(applicationContext, it) }
        Result.success()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (_: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
