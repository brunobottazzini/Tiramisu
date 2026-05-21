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
     * Initial deal (indices 0-3): b1, b2, c3, d8
     *   → b1 auto-moves to bastoni foundation → pile 0 = EMPTY
     *   → pile 1 = b2  (bastoni — exposed after same-suit step, goes to foundation)
     *   → pile 2 = c3  (coppe)
     *   → pile 3 = d8  (denari)
     *
     * Stock (indices 4-7): c7, c5, d3, s4  — exactly 4 cards, one deal empties the stock
     *   After deal: pile 0 = c7 (coppe), pile 1 = c5 (coppe) on b2, pile 2 = d3, pile 3 = s4
     *   → same-suit step: pile 1 (c5, coppe) → pile 0 (c7, coppe) → b2 exposed
     *   → foundation step: pile 1 (b2) → bastoni foundation (has b1)
     *   → stock empty → canRedeal() true → redeal step
     */
    fun tutorialDeck(): List<String> = listOf(
        // First 4 → initial deal to piles 0-3
        // b1 auto-moves to bastoni foundation → pile 0 = empty
        // pile 1 = b2  (bastoni — exposed after same-suit step, goes to foundation)
        // pile 2 = c3  (coppe)
        // pile 3 = d8  (denari)
        "b1", "b2", "c3", "d8",
        // Stock: exactly 4 cards — one deal empties the stock, triggering the redeal step
        // After deal: pile 0 = c7 (coppe), pile 1 = c5 (coppe) on b2, pile 2 = d3, pile 3 = s4
        // Same-suit step: pile 1 (c5, coppe) → pile 0 (c7, coppe)
        "c7", "c5", "d3", "s4"
    )
}
