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

    // ===== Stall-detection primitives =====

    const val MAX_LOOKAHEAD = 30

    data class Move(
        val fromPile:     Int,
        val toPile:       Int,        // -1 when toFoundation
        val toFoundation: Boolean
    )

    /** Sum of ranks on the 4 foundation slots ("zero" counts as 0). */
    fun foundationCardCount(s: TiramisuGameState): Int =
        s.foundations.sumOf { top ->
            if (top == "zero") 0 else TiramisuMoveValidator.rank(top)
        }

    /** Stable string representation of the state for the BFS visited set. */
    fun canonicalKey(s: TiramisuGameState): String =
        s.piles.joinToString("|") { pile -> pile.joinToString(",") } +
        ";" + s.foundations.joinToString(",")

    /**
     * Every legal move from [s] — foundation moves AND tableau-to-tableau,
     * including single-card-to-empty-pile (findHint skips these as hints,
     * but the BFS must consider them since they can unblock progress).
     */
    fun enumerateLegalMoves(s: TiramisuGameState): List<Move> {
        val out = mutableListOf<Move>()
        val strict = s.difficulty.strictTableau
        for (src in 0..3) {
            val card = s.topOfPile(src)
            if (card == "zero") continue
            // Foundation moves
            for (fIdx in 0..3) {
                if (TiramisuMoveValidator.canMoveToFoundation(card, s.foundations[fIdx])) {
                    out.add(Move(fromPile = src, toPile = -1, toFoundation = true))
                    break  // only one foundation lands the card; no need to enumerate all
                }
            }
            // Tableau-to-tableau moves
            for (dst in 0..3) {
                if (src == dst) continue
                if (TiramisuMoveValidator.canMoveToTableau(card, s.topOfPile(dst), strict = strict)) {
                    out.add(Move(fromPile = src, toPile = dst, toFoundation = false))
                }
            }
        }
        return out
    }

    /**
     * Returns a NEW state with [move] applied. Does not mutate [s].
     * After the move, any aces exposed on pile tops are auto-promoted to foundations,
     * mirroring TiramisuViewModel.autoMoveAces().
     */
    fun applyMove(s: TiramisuGameState, move: Move): TiramisuGameState {
        val next = s.deepCopy()
        val card = next.piles[move.fromPile].removeAt(next.piles[move.fromPile].size - 1)
        if (move.toFoundation) {
            val fIdx = next.foundations.indexOfFirst { f ->
                TiramisuMoveValidator.canMoveToFoundation(card, f)
            }
            // fIdx should always be >= 0 because the move was enumerated as legal
            next.foundations[fIdx] = card
        } else {
            next.piles[move.toPile].add(card)
        }
        autoPromoteAces(next)
        return next
    }

    /** Repeatedly promote any pile-top ace to a foundation slot. Mutates [s]. */
    private fun autoPromoteAces(s: TiramisuGameState) {
        var moved = true
        while (moved) {
            moved = false
            for (pileIdx in 0..3) {
                val top = s.topOfPile(pileIdx)
                if (top == "zero" || TiramisuMoveValidator.rank(top) != 1) continue
                for (fIdx in 0..3) {
                    if (TiramisuMoveValidator.canMoveToFoundation(top, s.foundations[fIdx])) {
                        s.piles[pileIdx].removeAt(s.piles[pileIdx].size - 1)
                        s.foundations[fIdx] = top
                        moved = true
                        break
                    }
                }
            }
        }
    }

    /**
     * Returns true if SOME sequence of legal moves of length <= [maxDepth] produces
     * a state with higher [foundationCardCount] than [start]. Returns false if no such
     * sequence exists within the lookahead — the state is considered stalled.
     *
     * BFS with a `visited` set keyed by [canonicalKey] to prevent cycle re-exploration.
     */
    fun canProgress(start: TiramisuGameState, maxDepth: Int): Boolean {
        val initialCount = foundationCardCount(start)
        val visited = hashSetOf(canonicalKey(start))
        val queue = ArrayDeque<Pair<TiramisuGameState, Int>>()
        queue.addLast(start to 0)

        while (queue.isNotEmpty()) {
            val (s, depth) = queue.removeFirst()
            if (depth >= maxDepth) continue
            for (move in enumerateLegalMoves(s)) {
                val next = applyMove(s, move)
                if (foundationCardCount(next) > initialCount) return true
                val key = canonicalKey(next)
                if (visited.add(key)) {
                    queue.addLast(next to depth + 1)
                }
            }
        }
        return false
    }
}
