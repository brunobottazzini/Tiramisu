package com.bottazzini.tiramisu.utils

object TiramisuDeckSetup {

    private val SUITS = listOf("b", "c", "d", "s")

    /** Returns a new ordered deck of 40 cards. */
    fun orderedDeck(): List<String> =
        SUITS.flatMap { suit -> (1..10).map { rank -> "$suit$rank" } }

    /** Returns a new shuffled deck of 40 cards (random order). */
    fun shuffledDeck(): List<String> = orderedDeck().shuffled()

    /**
     * Deterministic deck for the tutorial.
     *
     * First deal (indices 0-3):  b1, c5, c3, d8
     *   → b1 (Asso di Bastoni) auto-goes to foundation
     *   → c5, c3 land in piles 1 and 2 — both coppe → same-suit move available
     *
     * Second deal (indices 4-7): c7, c6, d3, s4
     *   → c7 and c6 land in piles 0 and 1 — both coppe → same-suit move available in second deal
     */
    fun tutorialDeck(): List<String> = listOf(
        // First 4 → initial deal to piles 0-3
        "b1", "c5", "c3", "d8",
        // Second 4 → second deal (c7 and c6 must share suit for tutorial)
        "c7", "c6", "d3", "s4",
        // Remaining 32 cards
        "b2", "b3", "b4", "b5", "b6", "b7", "b8", "b9", "b10",
        "c1", "c2", "c4", "c8", "c9", "c10",
        "d1", "d2", "d4", "d5", "d6", "d7", "d9", "d10",
        "s1", "s2", "s3", "s5", "s6", "s7", "s8", "s9", "s10"
    )
}
