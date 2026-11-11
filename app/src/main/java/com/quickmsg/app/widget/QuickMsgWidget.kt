package com.quickmsg.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class QuickMsgWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val repo = com.quickmsg.app.data.WidgetRepository(context)
        val config = repo.getByWidgetId(appWidgetId)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    label = config?.label ?: "Not configured",
                    contactName = config?.contactName ?: "",
                    message = config?.message ?: "",
                    colorHex = config?.colorHex ?: "#25D366",
                    widgetSize = config?.widgetSize ?: "medium",
                    hasMultipleMessages = (config?.messages?.size ?: 0) > 0,
                    appWidgetId = appWidgetId
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        label: String,
        contactName: String,
        message: String,
        colorHex: String,
        widgetSize: String,
        hasMultipleMessages: Boolean,
        appWidgetId: Int
    ) {
        val context = LocalContext.current
        val bgColor = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF25D366)
        }

        val intent = Intent(context, WidgetClickActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra("PICK_MESSAGE", hasMultipleMessages)
            data = android.net.Uri.parse("quickmsg://widget/$appWidgetId")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(bgColor)
                .clickable(actionStartActivity(intent))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (widgetSize) {
                "small" -> {
                    Text(
                        text = label.take(2).uppercase(),
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                "large" -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (contactName.isNotEmpty()) {
                            Text(
                                text = contactName,
                                style = TextStyle(
                                    color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                    fontSize = 12.sp
                                )
                            )
                        }
                        if (message.isNotEmpty()) {
                            Text(
                                text = if (message.length > 30) message.take(30) + "..." else message,
                                style = TextStyle(
                                    color = ColorProvider(Color.White.copy(alpha = 0.6f)),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (contactName.isNotEmpty()) {
                            Text(
                                text = contactName,
                                style = TextStyle(
                                    color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
