package com.bottazzini.tiramisu.settings

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import com.bottazzini.tiramisu.db.DatabaseHandler
import com.bottazzini.tiramisu.db.columns.GameLogColumns.GameLogEntry

class GameLogRepository(context: Context) {
    private val dbHandler = DatabaseHandler(context)

    fun insert(log: GameLog) {
        val db = dbHandler.writableDatabase
        val values = ContentValues().apply {
            put(GameLogEntry.COLUMN_TIMESTAMP,    log.timestamp)
            put(GameLogEntry.COLUMN_DURATION_MS,  log.durationMs)
            put(GameLogEntry.COLUMN_WON,          if (log.won) 1 else 0)
            put(GameLogEntry.COLUMN_HINTS_USED,   log.hintsUsed)
            put(GameLogEntry.COLUMN_DIFFICULTY,   log.difficulty)
            put(GameLogEntry.COLUMN_REDEALS_USED, log.redealsUsed)
        }
        db.insert(GameLogEntry.TABLE_NAME, null, values)
        trimIfNeeded(db)
    }

    fun countAll(): Long = rawCountQuery(
        "SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME}", emptyArray())

    fun countWins(): Long = rawCountQuery(
        "SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1",
        emptyArray())

    fun countByDifficulty(difficulty: String): Long = rawCountQuery(
        "SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_DIFFICULTY}=?",
        arrayOf(difficulty))

    fun countWinsByDifficulty(difficulty: String): Long = rawCountQuery(
        "SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1 AND ${GameLogEntry.COLUMN_DIFFICULTY}=?",
        arrayOf(difficulty))

    fun bestWinTimeMs(difficulty: String? = null): Long? {
        val (sql, args) = if (difficulty == null) {
            "SELECT MIN(${GameLogEntry.COLUMN_DURATION_MS}) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1" to emptyArray()
        } else {
            "SELECT MIN(${GameLogEntry.COLUMN_DURATION_MS}) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1 AND ${GameLogEntry.COLUMN_DIFFICULTY}=?" to arrayOf(difficulty)
        }
        val cursor = dbHandler.readableDatabase.rawQuery(sql, args)
        return try {
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        } finally {
            cursor.close()
        }
    }

    fun avgWinDurationMs(difficulty: String? = null): Long? {
        val (sql, args) = if (difficulty == null) {
            "SELECT AVG(${GameLogEntry.COLUMN_DURATION_MS}) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1" to emptyArray()
        } else {
            "SELECT AVG(${GameLogEntry.COLUMN_DURATION_MS}) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1 AND ${GameLogEntry.COLUMN_DIFFICULTY}=?" to arrayOf(difficulty)
        }
        val cursor = dbHandler.readableDatabase.rawQuery(sql, args)
        return try {
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        } finally {
            cursor.close()
        }
    }

    /** Returns up to [n] most-recent rows, newest first. */
    fun getLastN(n: Int): List<GameLog> {
        val db = dbHandler.readableDatabase
        val cursor = db.query(
            GameLogEntry.TABLE_NAME, null, null, null, null, null,
            "${BaseColumns._ID} DESC", n.toString()
        )
        val result = mutableListOf<GameLog>()
        while (cursor.moveToNext()) {
            result.add(cursor.toGameLog())
        }
        cursor.close()
        return result
    }

    fun close() { dbHandler.close() }

    private fun rawCountQuery(sql: String, args: Array<String>): Long {
        val cursor = dbHandler.readableDatabase.rawQuery(sql, args)
        return try {
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        } finally {
            cursor.close()
        }
    }

    private fun trimIfNeeded(db: android.database.sqlite.SQLiteDatabase) {
        val count = rawCountQuery(
            "SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME}", emptyArray())
        if (count > 500) {
            db.execSQL(
                "DELETE FROM ${GameLogEntry.TABLE_NAME} WHERE ${BaseColumns._ID} NOT IN " +
                "(SELECT ${BaseColumns._ID} FROM ${GameLogEntry.TABLE_NAME} ORDER BY ${BaseColumns._ID} DESC LIMIT 500)"
            )
        }
    }

    private fun android.database.Cursor.toGameLog() = GameLog(
        id = getLong(getColumnIndexOrThrow(BaseColumns._ID)),
        timestamp = getLong(getColumnIndexOrThrow(GameLogEntry.COLUMN_TIMESTAMP)),
        durationMs = getLong(getColumnIndexOrThrow(GameLogEntry.COLUMN_DURATION_MS)),
        won = getLong(getColumnIndexOrThrow(GameLogEntry.COLUMN_WON)) == 1L,
        hintsUsed = getInt(getColumnIndexOrThrow(GameLogEntry.COLUMN_HINTS_USED)),
        difficulty = getString(getColumnIndexOrThrow(GameLogEntry.COLUMN_DIFFICULTY)),
        redealsUsed = getInt(getColumnIndexOrThrow(GameLogEntry.COLUMN_REDEALS_USED))
    )
}
