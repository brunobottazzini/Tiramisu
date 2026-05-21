package com.bottazzini.tiramisu

import com.bottazzini.tiramisu.utils.TiramisuDeckSetup
import org.junit.Assert.*
import org.junit.Test

class TiramisuDeckSetupTest {

    @Test
    fun `shuffled deck has exactly 40 cards`() {
        assertEquals(40, TiramisuDeckSetup.shuffledDeck().size)
    }

    @Test
    fun `shuffled deck contains all 40 unique cards`() {
        val deck = TiramisuDeckSetup.shuffledDeck()
        assertEquals(40, deck.toSet().size)
        val suits = listOf("b", "c", "d", "s")
        for (suit in suits) {
            for (rank in 1..10) {
                assertTrue("$suit$rank missing", deck.contains("$suit$rank"))
            }
        }
    }

    @Test
    fun `tutorial deck has exactly 40 cards`() {
        assertEquals(40, TiramisuDeckSetup.tutorialDeck().size)
    }

    @Test
    fun `tutorial deck first card is ace of bastoni`() {
        assertEquals("b1", TiramisuDeckSetup.tutorialDeck()[0])
    }

    @Test
    fun `tutorial deck cards at index 4 and 5 are same suit`() {
        // Cards at positions 4 and 5 are dealt to pile 0 and pile 1 on second deal
        // They must share a suit for the same-suit move tutorial step
        val deck = TiramisuDeckSetup.tutorialDeck()
        val card4 = deck[4]
        val card5 = deck[5]
        assertEquals("positions 4 and 5 must share suit for tutorial",
                     card4[0], card5[0])
    }
}
