package com.quickmsg.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SentMessageDao {
    @Query("SELECT * FROM sent_messages ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SentMessage>>

    @Query("SELECT * FROM sent_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SentMessage>>

    @Insert
    suspend fun insert(msg: SentMessage)

    @Query("DELETE FROM sent_messages")
    suspend fun deleteAll()
}
