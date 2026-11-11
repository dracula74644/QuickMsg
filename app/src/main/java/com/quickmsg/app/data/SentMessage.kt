package com.quickmsg.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sent_messages")
data class SentMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val contactPhone: String,
    val timestamp: Long = System.currentTimeMillis()
)
