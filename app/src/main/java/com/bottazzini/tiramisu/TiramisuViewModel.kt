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
     * When true, [autoMoveToFoundation] will auto-move ANY top-pile card that can
     * go to a foundation, not just Aces. Set by [GameActivity] in onResume from the
     * AUTO_MOVE setting. Default false preserves the pre-feature behavior.
     */
    var autoCompleteEnabled: Boolean = false

    /**
     * Snapshot of [state] taken right before the most recent state-changing
     * action (move, deal, redeal). One slot only — single-depth undo.
     * `null` means no undo is available (start of game or just after an undo).
     */
    private var previousState: TiramisuGameState? = null

    /**
     * Auto-ace moves performed by the last action. Callers (GameActivity) read
     * this with [consumeAutoFoundationMoves] right after the action to play the
     * ace-to-foundation animation, which also clears the slot.
     */
    private var _lastAutoFoundationMoves: List<AutoFoundationMove> = emptyList()

    fun consumeAutoFoundationMoves(): List<AutoFoundationMove> {
        val moves = _lastAutoFoundationMoves
        _lastAutoFoundationMoves = emptyList()
        return moves
    }

    // ---- Game lifecycle ----

    fun newGame(difficulty: Difficulty) {
        state = TiramisuGameState.newGame(difficulty)
        selectedPileIndex = null
        previousState = null
        _lastAutoFoundationMoves = emptyList()
    }

    fun newTutorialGame(difficulty: Difficulty = Difficulty.NORMALE) {
        state = TiramisuGameState.tutorialGame(difficulty)
        selectedPileIndex = null
        previousState = null
        _lastAutoFoundationMoves = emptyList()
    }

    fun restoreState(restored: TiramisuGameState) {
        state = restored
        selectedPileIndex = null
        previousState = null
        _lastAutoFoundationMoves = emptyList()
    }

    // ---- Undo (1-depth) ----

    fun canUndo(): Boolean = previousState != null

    /** Restore the state captured before the last action. Returns true if undone. */
    fun undo(): Boolean {
        val prev = previousState ?: return false
        state = prev
        previousState = null
        selectedPileIndex = null
        _lastAutoFoundationMoves = emptyList()
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
        autoMoveToFoundation(AutoFoundationSource.STOCK)
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
                autoMoveToFoundation(AutoFoundationSource.PILE_TOP)
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
     * True when the game is stuck: stock empty, no redeals left, and no sequence
     * of legal moves up to [TiramisuSolver.MAX_LOOKAHEAD] depth can increase the
     * foundation card count. Legal-but-cyclic tableau moves do NOT prevent this
     * from returning true.
     */
    fun isLost(): Boolean {
        val s = state ?: return false
        if (s.isWon()) return false
        if (s.stock.isNotEmpty()) return false
        if (canRedeal()) return false
        return !TiramisuSolver.canProgress(s, TiramisuSolver.MAX_LOOKAHEAD)
    }

    /** Restart the same game from scratch using the original shuffle. */
    fun retrySameGame() {
        val s = state ?: return
        state = TiramisuGameState.replay(s.difficulty, s.initialDeck)
        selectedPileIndex = null
        previousState = null
        _lastAutoFoundationMoves = emptyList()
    }

    // ---- Drag & drop entry points (no dependency on selectedPileIndex) ----

    /**
     * True if the top of [srcPileIdx] (or the top-anchored run that starts there)
     * can legally move onto [dstPileIdx]. Under the PoC rules, a multi-card run
     * may move as a unit — see [TiramisuMoveValidator.topMovableRun].
     */
    fun canMoveBetweenPiles(srcPileIdx: Int, dstPileIdx: Int): Boolean {
        val s = state ?: return false
        if (srcPileIdx == dstPileIdx) return false
        val srcPile = s.piles[srcPileIdx]
        if (srcPile.isEmpty()) return false
        val movable = TiramisuMoveValidator.topMovableRun(
            srcPile,
            s.topOfPile(dstPileIdx),
            strict = s.difficulty.strictTableau
        )
        if (movable.isEmpty()) return false
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
        val srcPile = s.piles[srcIdx]
        if (srcPile.isEmpty()) return false
        val movable = TiramisuMoveValidator.topMovableRun(
            srcPile,
            s.topOfPile(dstIdx),
            strict = s.difficulty.strictTableau
        )
        if (movable.isEmpty()) return false
        // In DIFFICILE, block non-foundation moves when obbligato targets exist
        if (s.difficulty.obbligato && obbligatoTargets().isNotEmpty()) return false
        // Detach the top `n` cards from src in order, then append to dst.
        val n = movable.size
        val moving = srcPile.subList(srcPile.size - n, srcPile.size).toList()
        repeat(n) { srcPile.removeAt(srcPile.size - 1) }
        s.piles[dstIdx].addAll(moving)
        autoMoveToFoundation(AutoFoundationSource.PILE_TOP)
        return true
    }

    private fun canGoToAnyFoundation(card: String): Boolean {
        val s = state ?: return false
        return s.foundations.any { top ->
            TiramisuMoveValidator.canMoveToFoundation(card, top)
        }
    }

    /**
     * Auto-move top-pile cards to foundations. When [autoCompleteEnabled] is false, only
     * Aces (rank 1) move; when true, any card whose rank matches a foundation's expected
     * next card is sent up. Loops until stable. Records every move in
     * [_lastAutoFoundationMoves] for the animation layer to consume.
     *
     * [defaultSource] tags each move visually: STOCK if the card just exited the stock
     * (e.g. after [dealFromStock]); PILE_TOP otherwise.
     */
    private fun autoMoveToFoundation(defaultSource: AutoFoundationSource) {
        val s = state ?: return
        val moves = mutableListOf<AutoFoundationMove>()
        var moved = true
        while (moved) {
            moved = false
            for (pileIdx in 0..3) {
                val card = s.topOfPile(pileIdx)
                if (card == "zero") continue
                val isAce = TiramisuMoveValidator.rank(card) == 1
                if (!autoCompleteEnabled && !isAce) continue
                for (fIdx in 0..3) {
                    if (TiramisuMoveValidator.canMoveToFoundation(card, s.foundations[fIdx])) {
                        s.piles[pileIdx].removeAt(s.piles[pileIdx].size - 1)
                        s.foundations[fIdx] = card
                        moves.add(AutoFoundationMove(pileIdx, fIdx, card, defaultSource))
                        moved = true
                        break
                    }
                }
            }
        }
        _lastAutoFoundationMoves = moves
    }
}

enum class TapResult { SELECTED, DESELECTED, MOVED, INVALID, NOTHING }

enum class AutoFoundationSource { STOCK, PILE_TOP }

data class AutoFoundationMove(
    val fromPile: Int,
    val toFoundation: Int,
    val card: String,
    val source: AutoFoundationSource
)
