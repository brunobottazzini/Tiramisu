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
     * Rule: same suit, any rank. Empty pile (zero) accepts anything.
     */
    fun canMoveToTableau(movingCard: String, destinationTop: String): Boolean {
        if (movingCard == "zero") return false
        if (destinationTop == "zero") return true  // empty pile accepts any card
        return suit(movingCard) == suit(destinationTop)
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
}
