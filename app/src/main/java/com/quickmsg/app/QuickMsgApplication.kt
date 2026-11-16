package com.quickmsg.app

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.quickmsg.app.service.AccessibilityAlerts
import androidx.work.Configuration

class QuickMsgApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                AccessibilityAlerts.check(this@QuickMsgApplication)
            }
        }
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false, observer
        )
        AccessibilityAlerts.schedule(this)
        com.quickmsg.app.service.AppUpdates.schedule(this)
        AccessibilityAlerts.check(this)
    }
}
