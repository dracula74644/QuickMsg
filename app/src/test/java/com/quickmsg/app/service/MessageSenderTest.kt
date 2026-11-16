package com.quickmsg.app.service

import android.app.Application
import android.provider.Settings
import android.os.Looper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MessageSenderTest {
    private val context: Application = RuntimeEnvironment.getApplication()

    private fun setServices(value: String) {
        Settings.Secure.putString(context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, value)
    }

    @Test
    fun recognizesFullAndShortComponentNames() {
        setServices("other.app/.Service:com.quickmsg.app/com.quickmsg.app.service.WhatsAppAccessibilityService")
        assertTrue(MessageSender.isAccessibilityServiceEnabled(context))
        setServices("com.quickmsg.app/.service.WhatsAppAccessibilityService:other.app/.Service")
        assertTrue(MessageSender.isAccessibilityServiceEnabled(context))
    }

    @Test
    fun rejectsMissingOrDifferentService() {
        setServices("")
        assertFalse(MessageSender.isAccessibilityServiceEnabled(context))
        setServices("com.quickmsg.app/com.quickmsg.app.service.WhatsAppAccessibilityServiceOther")
        assertFalse(MessageSender.isAccessibilityServiceEnabled(context))
    }

    @Test
    fun backgroundSendWithDisabledServiceOpensSettingsWithoutCrashing() = runBlocking {
        setServices("")
        withContext(Dispatchers.IO) {
            MessageSender.sendWhatsAppMessage(context, "+15551234567", "Hello")
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS,
            shadowOf(context).nextStartedActivity.action)
    }
}
