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
     *   → b1 auto-moves to bastoni foundation → pile 0 = EMPTY
     *   → pile 1 = s6  (spade — neutral, different suit from all other pile tops)
     *   → pile 2 = c3  (coppe)
     *   → pile 3 = d8  (denari)
     *
     * s6 was chosen over b2 because:
     *   - b2 is rank 2 of bastoni: with b1 already in the foundation, users wonder
     *     why b2 doesn't auto-move there too (only aces auto-move).
     *   - s6 (spade) has no same-suit match on any other pile top in the initial state,
     *     so users can't accidentally move it during info steps.
     *
     * Stock (indices 4-7): c7, c5, d3, s4  — exactly 4 cards, one deal empties the stock
     *   After deal: pile 0 = c7 (coppe), pile 1 = c5 (coppe) on s6, pile 2 = d3, pile 3 = s4
     *   → same-suit step: pile 0 (c7, coppe) → pile 1 top (c5, coppe) → pile 0 = EMPTY
     *   → empty-pile step: pile 3 top (s4) → pile 0 (empty)
     *   → stock empty → canRedeal() true → redeal step
     */
    fun tutorialDeck(): List<String> = listOf(
        // First 4 → initial deal to piles 0-3
        // b1 auto-moves to bastoni foundation → pile 0 = empty
        // pile 1 = s6  (spade — neutral, can't go to any foundation yet)
        // pile 2 = c3  (coppe)
        // pile 3 = d8  (denari)
        "b1", "s6", "c3", "d8",
        // Stock: exactly 4 cards — one deal empties the stock, triggering the redeal step
        // After deal: pile 0 = c7 (coppe, 1 card), pile 1 = c5 (coppe) on s6, pile 2 = d3, pile 3 = s4
        // Same-suit step: pile 0 (c7) → pile 1 (c5, same suit coppe) → pile 0 EMPTY
        "c7", "c5", "d3", "s4"
    )
}
