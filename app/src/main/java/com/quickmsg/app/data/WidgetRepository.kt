package com.quickmsg.app.data

import android.content.Context

class WidgetRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.widgetConfigDao()
    private val sentDao = db.sentMessageDao()

    fun getAll() = dao.getAll()

    suspend fun getByWidgetId(widgetId: Int) = dao.getByWidgetId(widgetId)

    suspend fun save(config: WidgetConfig) = dao.insert(config)

    suspend fun delete(widgetId: Int) = dao.deleteByWidgetId(widgetId)

    suspend fun deleteMultiple(widgetIds: List<Int>) = dao.deleteByWidgetIds(widgetIds)

    fun sentMessageDao() = sentDao

    suspend fun logMessage(message: String, phone: String) {
        sentDao.insert(SentMessage(message = message, contactPhone = phone))
    }
}
