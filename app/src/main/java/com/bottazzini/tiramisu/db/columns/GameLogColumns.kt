package com.bottazzini.tiramisu.db.columns

import android.provider.BaseColumns

object GameLogColumns {
    object GameLogEntry : BaseColumns {
        const val TABLE_NAME          = "game_log"
        const val COLUMN_TIMESTAMP    = "timestamp"
        const val COLUMN_DURATION_MS  = "duration_ms"
        const val COLUMN_WON          = "won"
        const val COLUMN_HINTS_USED   = "hints_used"
        const val COLUMN_DIFFICULTY   = "difficulty"
        const val COLUMN_REDEALS_USED = "redeals_used"
    }
}
