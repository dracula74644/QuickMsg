package com.quickmsg.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.quickmsg.app.data.WidgetRepository
import com.quickmsg.app.service.MessageSender
import com.quickmsg.app.service.TemplateProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickMsgWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickMsgWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val repo = WidgetRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            repo.deleteMultiple(appWidgetIds.toList())
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (intent.action) {
            ACTION_SEND_MESSAGE -> {
                val repo = WidgetRepository(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val config = repo.getByWidgetId(widgetId) ?: return@launch
                    val processed = TemplateProcessor.process(config.message)
                    MessageSender.sendWhatsAppMessage(context, config.contactPhone, processed)
                }
            }
            ACTION_PICK_MESSAGE -> {
                val pickerIntent = Intent(context, MessagePickerActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(pickerIntent)
            }
        }
    }

    companion object {
        const val ACTION_SEND_MESSAGE = "com.quickmsg.app.ACTION_SEND_MESSAGE"
        const val ACTION_PICK_MESSAGE = "com.quickmsg.app.ACTION_PICK_MESSAGE"
    }
}
