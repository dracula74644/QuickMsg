package com.quickmsg.app.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickmsg.app.data.WidgetRepository
import com.quickmsg.app.service.MessageSender
import com.quickmsg.app.service.TemplateProcessor
import com.quickmsg.app.ui.theme.QuickMsgTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessagePickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repo = WidgetRepository(this)
        val activity = this

        CoroutineScope(Dispatchers.IO).launch {
            val config = repo.getByWidgetId(widgetId)
            if (config == null) {
                finish()
                return@launch
            }

            val allMessages = buildList {
                add(config.message)
                addAll(config.messages.filter { it != config.message })
            }.distinct()

            runOnUiThread {
                setContent {
                    QuickMsgTheme {
                        Surface(
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        ) {
                            MessagePickerScreen(
                                contactName = config.contactName,
                                messages = allMessages,
                                colorHex = config.colorHex,
                                onSelect = { message ->
                                    val processed = TemplateProcessor.process(message)
                                    MessageSender.sendWhatsAppMessage(
                                        activity, config.contactPhone, processed
                                    )
                                    finish()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessagePickerScreen(
    contactName: String,
    messages: List<String>,
    colorHex: String,
    onSelect: (String) -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF25D366)
    }

    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = "Send to $contactName",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pick a message",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(messages) { message ->
                val preview = if (TemplateProcessor.hasTemplateVars(message)) {
                    TemplateProcessor.previewTemplate(message)
                } else message

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelect(message) },
                    colors = CardDefaults.cardColors(
                        containerColor = accentColor.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = preview)
                            if (preview != message) {
                                Text(
                                    text = "Template: $message",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
