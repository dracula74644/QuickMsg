package com.quickmsg.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_configs")
    fun getAll(): Flow<List<WidgetConfig>>

    @Query("SELECT * FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun getByWidgetId(widgetId: Int): WidgetConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: WidgetConfig)

    @Update
    suspend fun update(config: WidgetConfig)

    @Delete
    suspend fun delete(config: WidgetConfig)

    @Query("DELETE FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun deleteByWidgetId(widgetId: Int)

    @Query("DELETE FROM widget_configs WHERE widgetId IN (:widgetIds)")
    suspend fun deleteByWidgetIds(widgetIds: List<Int>)
}
