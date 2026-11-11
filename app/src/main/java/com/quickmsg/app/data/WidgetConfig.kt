package com.quickmsg.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_configs")
data class WidgetConfig(
    @PrimaryKey val widgetId: Int,
    val contactName: String,
    val contactPhone: String,
    val message: String,
    val messages: List<String> = emptyList(),
    val label: String,
    val colorHex: String = "#25D366",
    val widgetSize: String = "medium",
    val chatOnly: Boolean = false
)
