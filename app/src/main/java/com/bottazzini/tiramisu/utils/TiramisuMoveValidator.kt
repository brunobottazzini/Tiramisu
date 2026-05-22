package com.bottazzini.tiramisu.utils

/**
 * Pure validation logic for Tiramisù.
 * Cards: "<suit><rank>" e.g. "c5", "b1", "d10", "s3".
 * "zero" = empty slot.
 * Suits: b=bastoni, c=coppe, d=denari, s=spade.
 */
object TiramisuMoveValidator {

    fun suit(card: String): String = card.substring(0, 1)
    fun rank(card: String): Int    = card.substring(1).toInt()

    /**
     * Can [movingCard] be placed on top of [destinationTop] in the tableau?
     *
     * Rule (lax, [strict] = false): same suit, any rank.
     * Rule (strict, [strict] = true): same suit AND rank(movingCard) < rank(destinationTop).
     * Empty pile (zero) accepts anything in both modes.
     */
    fun canMoveToTableau(
        movingCard: String,
        destinationTop: String,
        strict: Boolean
    ): Boolean {
        if (movingCard == "zero") return false
        if (destinationTop == "zero") return true  // empty pile accepts any card
        if (suit(movingCard) != suit(destinationTop)) return false
        return if (strict) {
            rank(movingCard) < rank(destinationTop)
        } else {
            true
        }
    }

    /**
     * Can [movingCard] be placed on [foundationTop]?
     * Rule: same suit, rank = foundationTop.rank + 1.
     * Empty foundation (zero) accepts only Aces (rank 1).
     */
    fun canMoveToFoundation(movingCard: String, foundationTop: String): Boolean {
        if (movingCard == "zero") return false
        val movingRank = rank(movingCard)
        if (foundationTop == "zero") return movingRank == 1
        return suit(movingCard) == suit(foundationTop) &&
               movingRank == rank(foundationTop) + 1
    }

    // -------------------------------------------------------------------------
    // Run support (PoC: multi-card moves)
    //
    // A "run" is a contiguous sequence of cards in a pile that:
    //  - all share the same suit, and
    //  - are strictly descending in rank from base (lower index) to top (last).
    //
    // Lists representing runs use the same orientation as a pile:
    //   index 0  = base   (closer to the bottom of the pile, will land first)
    //   last idx = top    (visible / picked-up card)
    // -------------------------------------------------------------------------

    /**
     * Returns the maximum run anchored at the top of [pile] (same suit,
     * strictly descending toward the top). Length is at least 1 for any
     * non-empty pile, and empty when [pile] is empty.
     */
    fun topRun(pile: List<String>): List<String> {
        if (pile.isEmpty()) return emptyList()
        val out = ArrayDeque<String>()
        out.addFirst(pile.last())
        var i = pile.size - 2
        while (i >= 0) {
            val below = pile[i]
            val above = out.first()
            if (below == "zero") break
            if (suit(below) != suit(above)) break
            if (rank(below) <= rank(above)) break  // need strict descent toward top
            out.addFirst(below)
            i--
        }
        return out.toList()
    }

    /**
     * True if [run] (base → top) is internally a valid run: non-empty,
     * all same suit, strictly descending in rank, and contains no "zero".
     */
    fun isValidRun(run: List<String>): Boolean {
        if (run.isEmpty()) return false
        if (run.any { it == "zero" }) return false
        val s = suit(run.first())
        for (i in 0 until run.size - 1) {
            if (suit(run[i + 1]) != s) return false
            if (rank(run[i]) <= rank(run[i + 1])) return false
        }
        return true
    }

    /**
     * Can [run] (base → top) be placed on top of [destinationTop]?
     * The run must be internally valid; the base card (run[0]) must satisfy
     * [canMoveToTableau] against [destinationTop] under the same [strict] mode.
     */
    fun canMoveRunToTableau(
        run: List<String>,
        destinationTop: String,
        strict: Boolean
    ): Boolean {
        if (!isValidRun(run)) return false
        return canMoveToTableau(run[0], destinationTop, strict)
    }

    /**
     * Longest top-anchored sub-run of [pile] that can legally land on
     * [destinationTop] under [strict]. Tries the full top-run first, then
     * drops the lowest card progressively, returning the first sub-run that
     * fits. Empty result means no legal multi- or single-card move exists
     * between this pile and that destination.
     */
    fun topMovableRun(
        pile: List<String>,
        destinationTop: String,
        strict: Boolean
    ): List<String> {
        val run = topRun(pile)
        if (run.isEmpty()) return emptyList()
        for (start in run.indices) {
            val candidate = run.subList(start, run.size)
            if (canMoveRunToTableau(candidate, destinationTop, strict)) return candidate
        }
        return emptyList()
    }
}
