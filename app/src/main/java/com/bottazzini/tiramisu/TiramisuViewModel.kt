// app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt
package com.bottazzini.tiramisu

import androidx.lifecycle.ViewModel
import com.bottazzini.tiramisu.utils.Difficulty
import com.bottazzini.tiramisu.utils.TiramisuGameState
import com.bottazzini.tiramisu.utils.TiramisuMoveValidator

class TiramisuViewModel : ViewModel() {

    var state: TiramisuGameState? = null
        private set

    /** Index (0-3) of the currently selected pile, or null if nothing selected. */
    var selectedPileIndex: Int? = null
        private set

    // ---- Game lifecycle ----

    fun newGame(difficulty: Difficulty) {
        state = TiramisuGameState.newGame(difficulty)
        selectedPileIndex = null
    }

    fun newTutorialGame(difficulty: Difficulty = Difficulty.FACILE) {
        state = TiramisuGameState.tutorialGame(difficulty)
        selectedPileIndex = null
    }

    fun restoreState(restored: TiramisuGameState) {
        state = restored
        selectedPileIndex = null
    }

    // ---- Tallone interactions ----

    /**
     * Deal one card from the stock to each pile (left to right).
     * Returns true if at least one card was dealt.
     * After dealing, auto-moves any new Aces to foundations.
     */
    fun dealFromStock(): Boolean {
        val s = state ?: return false
        if (s.stock.isEmpty()) return false
        val toDeal = minOf(4, s.stock.size)
        for (i in 0 until toDeal) {
            s.piles[i].add(s.stock.removeAt(0))
        }
        autoMoveAces()
        selectedPileIndex = null
        return true
    }

    /**
     * Redistribute: collect piles 3→2→1→0 into new stock.
     * Returns true if redeal was performed.
     */
    fun redeal(): Boolean {
        val s = state ?: return false
        if (s.redealsLeft <= 0 || s.stock.isNotEmpty()) return false
        val newStock = mutableListOf<String>()
        for (i in 3 downTo 0) {
            newStock.addAll(s.piles[i])
            s.piles[i].clear()
        }
        s.stock.addAll(newStock)
        s.redealsLeft--
        selectedPileIndex = null
        return true
    }

    /** True when the stock is empty and a redeal is still available. */
    fun canRedeal(): Boolean {
        val s = state ?: return false
        return s.stock.isEmpty() && s.redealsLeft > 0
    }

    // ---- Pile interactions ----

    /**
     * Called when the player taps pile [pileIdx].
     * - If nothing is selected: select this pile (if it has a card).
     * - If this pile is already selected: deselect.
     * - If another pile is selected: try to move top card → this pile.
     *
     * Returns the outcome.
     */
    fun onPileTapped(pileIdx: Int): TapResult {
        val s = state ?: return TapResult.NOTHING
        val selected = selectedPileIndex

        return when {
            selected == null -> {
                if (s.topOfPile(pileIdx) == "zero") TapResult.NOTHING
                else { selectedPileIndex = pileIdx; TapResult.SELECTED }
            }
            selected == pileIdx -> {
                selectedPileIndex = null
                TapResult.DESELECTED
            }
            else -> {
                val moved = movePileToPile(selected, pileIdx)
                selectedPileIndex = null
                if (moved) TapResult.MOVED else TapResult.INVALID
            }
        }
    }

    /**
     * Attempt to move the top card of pile [pileIdx] to a matching foundation.
     * Returns true if the move was made.
     */
    fun onFoundationTapped(pileIdx: Int): Boolean {
        val s = state ?: return false
        val moving = s.topOfPile(pileIdx)
        if (moving == "zero") return false
        for (fIdx in 0..3) {
            if (TiramisuMoveValidator.canMoveToFoundation(moving, s.foundations[fIdx])) {
                s.piles[pileIdx].removeAt(s.piles[pileIdx].size - 1)
                s.foundations[fIdx] = moving
                selectedPileIndex = null
                autoMoveAces()
                return true
            }
        }
        return false
    }

    // ---- Obbligato ----

    /**
     * In DIFFICILE mode: returns the pile indices whose top card MUST go to
     * a foundation before any other move is made.
     * Empty list means no obligation (or not in DIFFICILE mode).
     */
    fun obbligatoTargets(): List<Int> {
        val s = state ?: return emptyList()
        if (!s.difficulty.obbligato) return emptyList()
        return (0..3).filter { pileIdx ->
            val card = s.topOfPile(pileIdx)
            card != "zero" && canGoToAnyFoundation(card)
        }
    }

    /** True if the obbligato constraint is currently blocking moves. */
    fun isObbligatoBlocking(): Boolean = obbligatoTargets().isNotEmpty()

    // ---- Win check ----

    fun isWon(): Boolean = state?.isWon() ?: false

    // ---- Private helpers ----

    private fun movePileToPile(srcIdx: Int, dstIdx: Int): Boolean {
        val s = state ?: return false
        val moving = s.topOfPile(srcIdx)
        val dest   = s.topOfPile(dstIdx)
        if (!TiramisuMoveValidator.canMoveToTableau(moving, dest)) return false
        // In DIFFICILE, block non-foundation moves when obbligato targets exist
        if (s.difficulty.obbligato && obbligatoTargets().isNotEmpty()) return false
        s.piles[dstIdx].add(s.piles[srcIdx].removeAt(s.piles[srcIdx].size - 1))
        autoMoveAces()
        return true
    }

    private fun canGoToAnyFoundation(card: String): Boolean {
        val s = state ?: return false
        return s.foundations.any { top ->
            TiramisuMoveValidator.canMoveToFoundation(card, top)
        }
    }

    /** Auto-move any Aces (rank 1) from pile tops to foundations. Loops until stable. */
    private fun autoMoveAces() {
        val s = state ?: return
        var moved = true
        while (moved) {
            moved = false
            for (pileIdx in 0..3) {
                val card = s.topOfPile(pileIdx)
                if (card != "zero" && TiramisuMoveValidator.rank(card) == 1) {
                    for (fIdx in 0..3) {
                        if (TiramisuMoveValidator.canMoveToFoundation(card, s.foundations[fIdx])) {
                            s.piles[pileIdx].removeAt(s.piles[pileIdx].size - 1)
                            s.foundations[fIdx] = card
                            moved = true
                            break
                        }
                    }
                }
            }
        }
    }
}

enum class TapResult { SELECTED, DESELECTED, MOVED, INVALID, NOTHING }
