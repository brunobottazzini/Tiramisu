// app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt
package com.bottazzini.tiramisu

import androidx.lifecycle.ViewModel
import com.bottazzini.tiramisu.utils.Difficulty
import com.bottazzini.tiramisu.utils.TiramisuGameState
import com.bottazzini.tiramisu.utils.TiramisuMoveValidator
import com.bottazzini.tiramisu.utils.TiramisuSolver

class TiramisuViewModel : ViewModel() {

    var state: TiramisuGameState? = null
        private set

    /** Index (0-3) of the currently selected pile, or null if nothing selected. */
    var selectedPileIndex: Int? = null
        private set

    /**
     * Snapshot of [state] taken right before the most recent state-changing
     * action (move, deal, redeal). One slot only — single-depth undo.
     * `null` means no undo is available (start of game or just after an undo).
     */
    private var previousState: TiramisuGameState? = null

    /**
     * Auto-ace moves performed by the last action. Callers (GameActivity) read
     * this with [consumeAutoAceMoves] right after the action to play the
     * ace-to-foundation animation, which also clears the slot.
     */
    private var _lastAutoAceMoves: List<AceMove> = emptyList()

    fun consumeAutoAceMoves(): List<AceMove> {
        val moves = _lastAutoAceMoves
        _lastAutoAceMoves = emptyList()
        return moves
    }

    // ---- Game lifecycle ----

    fun newGame(difficulty: Difficulty) {
        state = TiramisuGameState.newGame(difficulty)
        selectedPileIndex = null
        previousState = null
        _lastAutoAceMoves = emptyList()
    }

    fun newTutorialGame(difficulty: Difficulty = Difficulty.FACILE) {
        state = TiramisuGameState.tutorialGame(difficulty)
        selectedPileIndex = null
        previousState = null
        _lastAutoAceMoves = emptyList()
    }

    fun restoreState(restored: TiramisuGameState) {
        state = restored
        selectedPileIndex = null
        previousState = null
        _lastAutoAceMoves = emptyList()
    }

    // ---- Undo (1-depth) ----

    fun canUndo(): Boolean = previousState != null

    /** Restore the state captured before the last action. Returns true if undone. */
    fun undo(): Boolean {
        val prev = previousState ?: return false
        state = prev
        previousState = null
        selectedPileIndex = null
        _lastAutoAceMoves = emptyList()
        return true
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
        val snapshot = s.deepCopy()
        val toDeal = minOf(4, s.stock.size)
        for (i in 0 until toDeal) {
            s.piles[i].add(s.stock.removeAt(0))
        }
        autoMoveAces(AceSource.STOCK)
        selectedPileIndex = null
        previousState = snapshot
        return true
    }

    /**
     * Redistribute: collect piles 3→2→1→0 into new stock.
     * Returns true if redeal was performed.
     */
    fun redeal(): Boolean {
        val s = state ?: return false
        if (s.redealsLeft <= 0 || s.stock.isNotEmpty()) return false
        val snapshot = s.deepCopy()
        val newStock = mutableListOf<String>()
        for (i in 3 downTo 0) {
            newStock.addAll(s.piles[i])
            s.piles[i].clear()
        }
        s.stock.addAll(newStock)
        s.redealsLeft--
        selectedPileIndex = null
        previousState = snapshot
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
                val snapshot = s.deepCopy()
                val moved = movePileToPile(selected, pileIdx)
                selectedPileIndex = null
                if (moved) { previousState = snapshot; TapResult.MOVED } else TapResult.INVALID
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
                val snapshot = s.deepCopy()
                s.piles[pileIdx].removeAt(s.piles[pileIdx].size - 1)
                s.foundations[fIdx] = moving
                selectedPileIndex = null
                autoMoveAces(AceSource.PILE_TOP)
                previousState = snapshot
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

    // ---- Win / Lost check ----

    fun isWon(): Boolean = state?.isWon() ?: false

    /**
     * True when no progress is possible: stock empty, no redeals left,
     * and no valid move (foundation or tableau) from any pile top.
     */
    fun isLost(): Boolean {
        val s = state ?: return false
        if (s.isWon()) return false
        if (s.stock.isNotEmpty()) return false
        if (canRedeal()) return false
        return TiramisuSolver.findHint(s) == null
    }

    /** Restart the same game from scratch using the original shuffle. */
    fun retrySameGame() {
        val s = state ?: return
        state = TiramisuGameState.replay(s.difficulty, s.initialDeck)
        selectedPileIndex = null
        previousState = null
        _lastAutoAceMoves = emptyList()
    }

    // ---- Drag & drop entry points (no dependency on selectedPileIndex) ----

    /** True if the top card of [srcPileIdx] can legally move onto [dstPileIdx]. */
    fun canMoveBetweenPiles(srcPileIdx: Int, dstPileIdx: Int): Boolean {
        val s = state ?: return false
        if (srcPileIdx == dstPileIdx) return false
        val moving = s.topOfPile(srcPileIdx)
        if (moving == "zero") return false
        val dest = s.topOfPile(dstPileIdx)
        if (!TiramisuMoveValidator.canMoveToTableau(moving, dest)) return false
        if (s.difficulty.obbligato && obbligatoTargets().isNotEmpty()) return false
        return true
    }

    /** True if the top card of [srcPileIdx] can legally move onto some foundation. */
    fun canMoveTopToAnyFoundation(srcPileIdx: Int): Boolean {
        val s = state ?: return false
        val moving = s.topOfPile(srcPileIdx)
        if (moving == "zero") return false
        return s.foundations.any { TiramisuMoveValidator.canMoveToFoundation(moving, it) }
    }

    /** Drag-driven equivalent of [onPileTapped] that does not consult [selectedPileIndex]. */
    fun tryMoveBetweenPiles(srcPileIdx: Int, dstPileIdx: Int): Boolean {
        if (!canMoveBetweenPiles(srcPileIdx, dstPileIdx)) return false
        val snapshot = state?.deepCopy()
        val moved = movePileToPile(srcPileIdx, dstPileIdx)
        if (moved) {
            selectedPileIndex = null
            previousState = snapshot
        }
        return moved
    }

    // ---- Private helpers ----

    private fun movePileToPile(srcIdx: Int, dstIdx: Int): Boolean {
        val s = state ?: return false
        val moving = s.topOfPile(srcIdx)
        val dest   = s.topOfPile(dstIdx)
        if (!TiramisuMoveValidator.canMoveToTableau(moving, dest)) return false
        // In DIFFICILE, block non-foundation moves when obbligato targets exist
        if (s.difficulty.obbligato && obbligatoTargets().isNotEmpty()) return false
        s.piles[dstIdx].add(s.piles[srcIdx].removeAt(s.piles[srcIdx].size - 1))
        autoMoveAces(AceSource.PILE_TOP)
        return true
    }

    private fun canGoToAnyFoundation(card: String): Boolean {
        val s = state ?: return false
        return s.foundations.any { top ->
            TiramisuMoveValidator.canMoveToFoundation(card, top)
        }
    }

    /**
     * Auto-move any Aces (rank 1) from pile tops to foundations. Loops until stable.
     * Records every move in [_lastAutoAceMoves] for the animation layer to consume.
     * [defaultSource] tags each move with where the ace originally came from
     * visually: STOCK for dealFromStock (the ace just exited the stock), PILE_TOP
     * for moves where the ace was already sitting on a pile and got auto-moved.
     */
    private fun autoMoveAces(defaultSource: AceSource) {
        val s = state ?: return
        val moves = mutableListOf<AceMove>()
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
                            moves.add(AceMove(pileIdx, fIdx, card, defaultSource))
                            moved = true
                            break
                        }
                    }
                }
            }
        }
        _lastAutoAceMoves = moves
    }
}

enum class TapResult { SELECTED, DESELECTED, MOVED, INVALID, NOTHING }

enum class AceSource { STOCK, PILE_TOP }

data class AceMove(
    val fromPile: Int,
    val toFoundation: Int,
    val card: String,
    val source: AceSource
)
