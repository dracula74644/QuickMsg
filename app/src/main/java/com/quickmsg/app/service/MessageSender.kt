package com.quickmsg.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MessageSender {

    fun sendWhatsAppMessage(context: Context, phoneNumber: String, message: String) {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val processedMessage = TemplateProcessor.process(message)

        if (!isAccessibilityServiceEnabled(context)) {
            Toast.makeText(
                context,
                "Enable QuickMsg Accessibility Service in Settings to auto-send",
                Toast.LENGTH_LONG
            ).show()
            openAccessibilitySettings(context)
            return
        }

        WhatsAppAccessibilityService.pendingSend.set(true)

        val uri = Uri.parse(
            "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(processedMessage)}"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                Intent.FLAG_ACTIVITY_NO_HISTORY
            )
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            WhatsAppAccessibilityService.pendingSend.set(false)
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun openChat(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val serviceName =
            "${context.packageName}/${WhatsAppAccessibilityService::class.java.canonicalName}"
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
