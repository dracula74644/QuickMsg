package com.quickmsg.app.service

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.runBlocking
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AppUpdatesTest {
    private fun manifest(code: Long = 4, url: String = "https://github.com/dracula74644/QuickMsg/releases/download/v1.3/QuickMsg-v1.3.apk") =
        """{"versionCode":$code,"versionName":"1.3","apkUrl":"$url"}"""

    @Test fun newerReleaseIsOffered() {
        assertEquals(4L, AppUpdates.parseManifest(manifest(), 3)!!.versionCode)
    }

    @Test fun sameOrOlderReleaseIsNotOffered() {
        assertNull(AppUpdates.parseManifest(manifest(3), 3))
        assertNull(AppUpdates.parseManifest(manifest(2), 3))
    }

    @Test fun comparesNumericVersionCodesInsteadOfVersionNames() {
        assertNotNull(AppUpdates.parseManifest(manifest(10), 9))
    }

    @Test fun rejectsDownloadsOutsideOfficialReleaseAssets() {
        listOf(
            "http://github.com/dracula74644/QuickMsg/releases/download/v1.3/app.apk",
            "https://evil.example/app.apk",
            "https://github.com/another/repo/releases/download/v1.3/app.apk",
            "https://github.com/dracula74644/QuickMsg/releases/download/../../other/app.apk",
            "https://github.com/dracula74644/QuickMsg/releases/download/v1.3/app.zip"
        ).forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { AppUpdates.parseManifest(manifest(url = url), 3) }
        }
    }

    @Test fun malformedManifestIsNotTreatedAsUpToDate() {
        assertThrows(Exception::class.java) { AppUpdates.parseManifest("{}", 3) }
    }

    @Test fun recentCachedUpdateIsAvailableWithoutNetwork() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val installed = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
        context.getSharedPreferences("app_updates", Context.MODE_PRIVATE).edit()
            .putString("manifest", manifest(installed + 1))
            .putLong("last_check", System.currentTimeMillis()).commit()
        assertEquals(installed + 1, AppUpdates.check(context)!!.versionCode)
    }

    @Test fun updateNotificationIsNotRepeatedAfterDismissal() {
        val context = RuntimeEnvironment.getApplication()
        val manager = context.getSystemService(NotificationManager::class.java)
        val update = AppUpdates.parseManifest(manifest(), 3)!!
        AppUpdates.notifyAvailable(context, update)
        assertEquals(1, manager.activeNotifications.size)
        manager.cancelAll()
        AppUpdates.notifyAvailable(context, update)
        assertEquals(0, manager.activeNotifications.size)
    }
}
