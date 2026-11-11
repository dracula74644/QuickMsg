package com.quickmsg.app.service

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TemplateProcessor {
    fun process(template: String): String {
        var result = template
        val now = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

        result = result.replace("{time}", timeFormat.format(now.time))
        result = result.replace("{date}", dateFormat.format(now.time))

        val plus15 = (now.clone() as Calendar).apply { add(Calendar.MINUTE, 15) }
        result = result.replace("{time_plus_15}", timeFormat.format(plus15.time))

        val plus30 = (now.clone() as Calendar).apply { add(Calendar.MINUTE, 30) }
        result = result.replace("{time_plus_30}", timeFormat.format(plus30.time))

        val plus60 = (now.clone() as Calendar).apply { add(Calendar.HOUR, 1) }
        result = result.replace("{time_plus_60}", timeFormat.format(plus60.time))

        // {location} left as-is if no location available — needs GPS permission for full support
        if (result.contains("{location}")) {
            result = result.replace("{location}", "(location unavailable)")
        }

        return result
    }

    fun hasTemplateVars(text: String): Boolean {
        return text.contains("{") && text.contains("}")
    }

    fun previewTemplate(template: String): String {
        return process(template)
    }
}
