package com.quickmsg.app.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.quickmsg.app.data.WidgetRepository
import com.quickmsg.app.service.MessageSender
import com.quickmsg.app.service.TemplateProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetClickActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val pickMessage = intent.getBooleanExtra("PICK_MESSAGE", false)

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repo = WidgetRepository(this)
        val activity = this

        CoroutineScope(Dispatchers.IO).launch {
            val config = repo.getByWidgetId(widgetId)
            if (config == null) {
                val configIntent = android.content.Intent(activity, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                runOnUiThread {
                    startActivity(configIntent)
                    finish()
                }
                return@launch
            }

            if (config.chatOnly) {
                MessageSender.openChat(activity, config.contactPhone)
                finish()
            } else if (pickMessage && config.messages.isNotEmpty()) {
                val pickerIntent = android.content.Intent(activity, MessagePickerActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                runOnUiThread {
                    startActivity(pickerIntent)
                    finish()
                }
            } else {
                val processed = TemplateProcessor.process(config.message)
                repo.logMessage(processed, config.contactPhone)
                MessageSender.sendWhatsAppMessage(activity, config.contactPhone, processed)
                finish()
            }
        }
    }
}
