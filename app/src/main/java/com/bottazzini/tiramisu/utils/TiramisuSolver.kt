package com.bottazzini.tiramisu.utils

/**
 * Finds the first available hint move in a Tiramisù game state.
 * Priority: foundation moves first, then tableau-to-tableau.
 */
object TiramisuSolver {

    data class Hint(
        val fromPile:    Int,
        val toPile:      Int?,    // null if toFoundation is true
        val toFoundation: Boolean
    )

    /** Returns the first available move, or null if none exists. */
    fun findHint(state: TiramisuGameState): Hint? {
        // 1. Foundation moves (highest priority)
        for (pileIdx in 0..3) {
            val card = state.topOfPile(pileIdx)
            if (card == "zero") continue
            for (fIdx in 0..3) {
                if (TiramisuMoveValidator.canMoveToFoundation(card, state.foundations[fIdx])) {
                    return Hint(fromPile = pileIdx, toPile = null, toFoundation = true)
                }
            }
        }

        // 2. Tableau-to-tableau moves
        for (srcIdx in 0..3) {
            val srcCard = state.topOfPile(srcIdx)
            if (srcCard == "zero") continue
            for (dstIdx in 0..3) {
                if (srcIdx == dstIdx) continue
                if (TiramisuMoveValidator.canMoveToTableau(
                        srcCard,
                        state.topOfPile(dstIdx),
                        strict = state.difficulty.strictTableau
                    )) {
                    // Skip moves that are just shuffling single card to empty pile (unhelpful)
                    if (state.topOfPile(dstIdx) == "zero" && state.piles[srcIdx].size == 1) continue
                    return Hint(fromPile = srcIdx, toPile = dstIdx, toFoundation = false)
                }
            }
        }

        return null
    }
}
