// app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuGameState.kt
package com.bottazzini.tiramisu.utils

/**
 * Full mutable game state for Tiramisù.
 *
 * piles:       4 card stacks. Last element = top card. Empty pile top = "zero".
 * stock:       Remaining cards in tallone. Index 0 = next card to be dealt.
 * foundations: 4 foundation tops. "zero" = empty foundation.
 * redealsLeft: How many redistributions remain.
 */
class TiramisuGameState(
    val piles:       List<MutableList<String>>,  // indices 0-3
    val stock:       MutableList<String>,
    val foundations: MutableList<String>,        // indices 0-3, "zero" if empty
    var redealsLeft: Int,
    val difficulty:  Difficulty,
    var gameStartTimeMillis: Long   = 0L,
    var timerPausedMs:       Long   = 0L,
    var isTimerPaused:       Boolean = false,
    var hasActiveGame:       Boolean = false
) {
    /** Top card of pile [idx], or "zero" if empty. */
    fun topOfPile(idx: Int): String = piles[idx].lastOrNull() ?: "zero"

    /** True when all 4 foundations are complete (top card = rank 10). */
    fun isWon(): Boolean = foundations.all { top ->
        top != "zero" && TiramisuMoveValidator.rank(top) == 10
    }

    /** Deep clone — independent copies of piles/stock/foundations. */
    fun deepCopy(): TiramisuGameState = TiramisuGameState(
        piles               = piles.map { it.toMutableList() },
        stock               = stock.toMutableList(),
        foundations         = foundations.toMutableList(),
        redealsLeft         = redealsLeft,
        difficulty          = difficulty,
        gameStartTimeMillis = gameStartTimeMillis,
        timerPausedMs       = timerPausedMs,
        isTimerPaused       = isTimerPaused,
        hasActiveGame       = hasActiveGame
    )

    companion object {
        /** Create a fresh game state with a shuffled stock. */
        fun newGame(difficulty: Difficulty): TiramisuGameState = TiramisuGameState(
            piles       = List(4) { mutableListOf() },
            stock       = TiramisuDeckSetup.shuffledDeck().toMutableList(),
            foundations = MutableList(4) { "zero" },
            redealsLeft = difficulty.redeals,
            difficulty  = difficulty,
            hasActiveGame = true
        )

        /** Create a fresh game state with the tutorial deck. */
        fun tutorialGame(difficulty: Difficulty = Difficulty.FACILE): TiramisuGameState =
            TiramisuGameState(
                piles       = List(4) { mutableListOf() },
                stock       = TiramisuDeckSetup.tutorialDeck().toMutableList(),
                foundations = MutableList(4) { "zero" },
                redealsLeft = difficulty.redeals,
                difficulty  = difficulty,
                hasActiveGame = true
            )
    }
}
