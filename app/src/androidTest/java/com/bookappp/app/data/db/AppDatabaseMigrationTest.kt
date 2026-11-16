package com.bookappp.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_preservesBooksAndAddsIndex() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO books
                    (id, file_name, title, author, format, cover_path, added_at, last_opened_at)
                VALUES
                    (1, 'novel.epub', 'Novel', NULL, 'EPUB', NULL, 100, NULL)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            AppDatabase.MIGRATION_1_2,
        ).use { db ->
            db.query("SELECT title FROM books WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Novel", cursor.getString(0))
            }
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_books_added_at'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
