// app/src/main/java/com/bottazzini/tiramisu/settings/GameStateRepository.kt
package com.bottazzini.tiramisu.settings

import android.content.Context
import android.util.Log
import com.bottazzini.tiramisu.utils.Difficulty
import com.bottazzini.tiramisu.utils.TiramisuGameState
import org.json.JSONArray
import org.json.JSONObject

class GameStateRepository(context: Context) {

    private val settingsHandler = SettingsHandler(context)

    fun hasSavedGame(): Boolean {
        val raw = settingsHandler.readValue(KEY) ?: return false
        return raw.isNotEmpty() && raw != SENTINEL_EMPTY
    }

    fun clear() {
        settingsHandler.updateSetting(KEY, SENTINEL_EMPTY)
    }

    fun save(state: TiramisuGameState) {
        val json = JSONObject().apply {
            put("v",              CURRENT_VERSION)
            put("difficulty",     state.difficulty.key)
            put("redealsLeft",    state.redealsLeft)
            put("gameStartMs",    state.gameStartTimeMillis)
            put("timerPausedMs",  state.timerPausedMs)
            put("isTimerPaused",  state.isTimerPaused)
            put("stock",          listToJson(state.stock))
            put("foundations",    listToJson(state.foundations))
            put("initialDeck",    listToJson(state.initialDeck))
            for (i in 0..3) put("pile$i", listToJson(state.piles[i]))
        }
        settingsHandler.updateSetting(KEY, json.toString())
    }

    fun load(): TiramisuGameState? {
        val raw = settingsHandler.readValue(KEY) ?: return null
        if (raw == SENTINEL_EMPTY || raw.isEmpty()) return null
        return try {
            val json    = JSONObject(raw)
            val version = json.optInt("v", 0)
            if (version != CURRENT_VERSION) return null
            val difficulty = Difficulty.fromKey(json.getString("difficulty"))
            TiramisuGameState(
                piles             = List(4) { i -> jsonToMutableList(json.getJSONArray("pile$i")) },
                stock             = jsonToMutableList(json.getJSONArray("stock")),
                foundations       = jsonToMutableList(json.getJSONArray("foundations")),
                redealsLeft       = json.getInt("redealsLeft"),
                difficulty        = difficulty,
                initialDeck       = jsonToMutableList(json.getJSONArray("initialDeck")),
                gameStartTimeMillis = json.getLong("gameStartMs"),
                timerPausedMs     = json.getLong("timerPausedMs"),
                isTimerPaused     = json.getBoolean("isTimerPaused"),
                hasActiveGame     = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load game state from JSON", e)
            null
        }
    }

    fun close() { settingsHandler.close() }

    companion object {
        private const val TAG              = "GameStateRepository"
        private const val KEY              = "savedTiramisuState"
        private const val CURRENT_VERSION  = 2
        private const val SENTINEL_EMPTY   = "__empty__"

        private fun listToJson(list: List<String>): JSONArray {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            return arr
        }

        private fun jsonToMutableList(arr: JSONArray): MutableList<String> {
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            return list
        }
    }
}
