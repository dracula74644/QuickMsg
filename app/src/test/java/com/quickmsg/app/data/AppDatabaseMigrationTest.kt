package com.quickmsg.app.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppDatabaseMigrationTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val databaseName = "migration-test"

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    private fun openDatabase(): AppDatabase = Room.databaseBuilder(
        context, AppDatabase::class.java, databaseName
    ).addMigrations(AppDatabase.MIGRATION_3_4).build()

    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val db = openDatabase()
        try {
            block(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun version3UpgradePreservesWidgetsAndHistory() = runBlocking {
        // Schema from the initial release (version 3), before chatOnly existed.
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("""
                CREATE TABLE widget_configs (
                    widgetId INTEGER NOT NULL PRIMARY KEY,
                    contactName TEXT NOT NULL, contactPhone TEXT NOT NULL,
                    message TEXT NOT NULL, messages TEXT NOT NULL, label TEXT NOT NULL,
                    colorHex TEXT NOT NULL, widgetSize TEXT NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE sent_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    message TEXT NOT NULL, contactPhone TEXT NOT NULL, timestamp INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO widget_configs VALUES
                (42, 'Alex', '+15551234567', 'On my way!', '["Reached safely"]',
                 'Home', '#128C7E', 'large')
            """.trimIndent())
            db.execSQL("INSERT INTO sent_messages VALUES (7, 'Hello', '+15551234567', 123456789)")
            db.version = 3
        }

        withDatabase { db ->
            val widget = db.widgetConfigDao().getByWidgetId(42)!!
            assertEquals(WidgetConfig(
                widgetId = 42, contactName = "Alex", contactPhone = "+15551234567",
                message = "On my way!", messages = listOf("Reached safely"),
                label = "Home", colorHex = "#128C7E", widgetSize = "large"
            ), widget)
            assertFalse(widget.chatOnly)
            assertEquals(listOf(SentMessage(7, "Hello", "+15551234567", 123456789)),
                db.sentMessageDao().getAll().first())
            // Verify the upgraded database remains writable with the new mode.
            db.widgetConfigDao().insert(widget.copy(chatOnly = true))
        }
        withDatabase { db ->
            assertTrue(db.widgetConfigDao().getByWidgetId(42)!!.chatOnly)
        }
    }

    @Test
    fun currentVersionReopenPreservesExistingWidgets() = runBlocking {
        val widget = WidgetConfig(99, "Sam", "+15559876543", "", label = "Chat", chatOnly = true)
        withDatabase { db -> db.widgetConfigDao().insert(widget) }
        withDatabase { db ->
            assertEquals(widget, db.widgetConfigDao().getByWidgetId(99))
        }
    }
}
