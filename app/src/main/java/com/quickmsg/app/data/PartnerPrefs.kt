package com.quickmsg.app.data

import android.content.Context

data class PartnerInfo(
    val name: String,
    val phone: String,
    val emoji: String
)

class PartnerPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("partner", Context.MODE_PRIVATE)

    fun isSetUp(): Boolean = prefs.getString("phone", null)?.isNotBlank() == true

    fun save(name: String, phone: String, emoji: String = "❤️") {
        prefs.edit()
            .putString("name", name)
            .putString("phone", phone)
            .putString("emoji", emoji)
            .apply()
    }

    fun get(): PartnerInfo = PartnerInfo(
        name = prefs.getString("name", "") ?: "",
        phone = prefs.getString("phone", "") ?: "",
        emoji = prefs.getString("emoji", "❤️") ?: "❤️"
    )

    fun clear() = prefs.edit().clear().apply()
}
