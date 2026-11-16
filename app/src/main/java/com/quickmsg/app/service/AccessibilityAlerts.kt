package com.quickmsg.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.quickmsg.app.R
import java.util.concurrent.TimeUnit

object AccessibilityAlerts {
    internal const val CHANNEL_ID = "accessibility_alerts"
    internal const val NOTIFICATION_ID = 1001

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "accessibility-status", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AccessibilityCheckWorker>(15, TimeUnit.MINUTES).build()
        )
    }

    // Persist the transition so process restarts don't lose it or repeat alerts.
    // Don't warn a new user who hasn't enabled auto-send yet.
    @Synchronized
    fun check(context: Context) {
        val prefs = context.getSharedPreferences("accessibility_alerts", Context.MODE_PRIVATE)
        val manager = context.getSystemService(NotificationManager::class.java)
        if (MessageSender.isAccessibilityServiceEnabled(context)) {
            prefs.edit().putBoolean("was_enabled", true).putBoolean("notified", false).apply()
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (!prefs.getBoolean("was_enabled", false) || prefs.getBoolean("notified", false)) return

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, context.getString(R.string.accessibility_alert_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        ))
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) return
        if (!manager.areNotificationsEnabled() ||
            manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return

        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.accessibility_alert_title))
            .setContentText(context.getString(R.string.accessibility_alert_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                context.getString(R.string.accessibility_alert_body)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
            prefs.edit().putBoolean("notified", true).apply()
        } catch (_: SecurityException) {
            // Permission may have been revoked between the check and notify.
        }
    }
}

class AccessibilityCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        AccessibilityAlerts.check(applicationContext)
        return Result.success()
    }
}
