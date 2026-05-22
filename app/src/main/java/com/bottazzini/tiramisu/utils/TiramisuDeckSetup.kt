package com.bottazzini.tiramisu.utils

object TiramisuDeckSetup {

    private val SUITS = listOf("b", "c", "d", "s")

    /** Returns a new ordered deck of 40 cards. */
    fun orderedDeck(): List<String> =
        SUITS.flatMap { suit -> (1..10).map { rank -> "$suit$rank" } }

    /** Returns a new shuffled deck of 40 cards (random order). */
    fun shuffledDeck(): List<String> = orderedDeck().shuffled()

    /**
     * Deterministic deck for the tutorial — exactly 8 cards.
     *
     * Initial deal (indices 0-3): b1, s6, c3, d8
     *   → b1 auto-moves to bastoni foundation (with animation) → pile 0 = EMPTY
     *   → pile 1 = s6  (spade — neutral, different suit from all other pile tops)
     *   → pile 2 = c3  (coppe)
     *   → pile 3 = d8  (denari)
     *
     * Stock (indices 4-7): c5, c7, d3, b2  — exactly 4 cards, one deal empties the stock
     *   After deal: pile 0 = c5 (coppe, 1 card)
     *              pile 1 = c7 (coppe) on s6
     *              pile 2 = d3 (denari) on c3
     *              pile 3 = b2 (bastoni) on d8   ← b2 for the foundation step
     *
     * Tutorial step sequence:
     *   same-suit  : pile 0 (c5, coppe) → pile 1 top (c7, coppe) → pile 0 = EMPTY  ← valid under STRICT (5 < 7)
     *   empty-pile : pile 2 top (d3, denari) → pile 0 (empty)      keeps b2 on pile 3
     *   foundation : pile 3 top (b2, bastoni) → bastoni foundation  2 follows the 1
     *   redeal     : stock empty since 2nd deal → canRedeal() true
     */
    fun tutorialDeck(): List<String> = listOf(
        // First 4 → initial deal to piles 0-3
        // b1 auto-moves to bastoni foundation → pile 0 = empty
        // pile 1 = s6  (spade — neutral, can't go to any foundation yet)
        // pile 2 = c3  (coppe)
        // pile 3 = d8  (denari)
        "b1", "s6", "c3", "d8",
        // Stock: exactly 4 cards — one deal empties the stock.
        // After deal: pile 0 = c5 (coppe), pile 1 = c7 (coppe) on s6
        //             pile 2 = d3 (denari) on c3, pile 3 = b2 (bastoni) on d8
        // pile 0 (c5) -> pile 1 top (c7) is valid under STRICT mode (5 < 7).
        "c5", "c7", "d3", "b2"
    )
}
