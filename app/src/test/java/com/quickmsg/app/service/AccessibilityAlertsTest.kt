package com.quickmsg.app.service

import android.app.Application
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AccessibilityAlertsTest {
    private val context: Application = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(NotificationManager::class.java)

    private fun setEnabled(enabled: Boolean) {
        Settings.Secure.putString(context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            if (enabled) "com.quickmsg.app/.service.WhatsAppAccessibilityService" else "")
        AccessibilityAlerts.check(context)
    }

    @Test
    fun noWarningBeforeFirstEnable() {
        setEnabled(false)
        assertEquals(0, manager.activeNotifications.size)
    }

    @Test
    fun alertsOncePerOutageAndClearsAfterReenable() {
        setEnabled(true)
        setEnabled(false)
        assertEquals(1, manager.activeNotifications.size)
        val notification = manager.activeNotifications.single().notification
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS,
            shadowOf(notification.contentIntent).savedIntent.action)
        manager.cancel(AccessibilityAlerts.NOTIFICATION_ID)
        // Dismissing the alert must not cause it to reappear every 15 minutes.
        AccessibilityAlerts.check(context)
        assertEquals(0, manager.activeNotifications.size)
        setEnabled(true)
        setEnabled(false)
        assertEquals(1, manager.activeNotifications.size)
        setEnabled(true)
        assertEquals(0, manager.activeNotifications.size)
    }

    @Test
    fun blockedNotificationsDoNotLosePendingAlert() {
        shadowOf(manager).setNotificationsEnabled(false)
        setEnabled(true)
        setEnabled(false)
        assertEquals(0, manager.activeNotifications.size)
        shadowOf(manager).setNotificationsEnabled(true)
        AccessibilityAlerts.check(context)
        assertEquals(1, manager.activeNotifications.size)
    }

    @Test
    @Config(sdk = [33], application = Application::class)
    fun android13WaitsForNotificationPermission() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        setEnabled(true)
        setEnabled(false)
        assertEquals(0, manager.activeNotifications.size)
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        AccessibilityAlerts.check(context)
        assertEquals(1, manager.activeNotifications.size)
    }
}
