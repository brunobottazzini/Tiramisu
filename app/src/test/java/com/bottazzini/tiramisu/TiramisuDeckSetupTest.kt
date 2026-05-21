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
    fun `tutorial deck has exactly 8 cards`() {
        assertEquals(8, TiramisuDeckSetup.tutorialDeck().size)
    }

    @Test
    fun `tutorial deck initial deal is b1 b2 c3 d8`() {
        val deck = TiramisuDeckSetup.tutorialDeck()
        assertEquals("b1", deck[0])
        assertEquals("b2", deck[1])
        assertEquals("c3", deck[2])
        assertEquals("d8", deck[3])
    }

    @Test
    fun `tutorial deck stock cards index 4 and 5 share suit for same-suit step`() {
        // After dealing initial 4: pile 0 is empty (b1 auto-moves), pile 1=b2, pile 2=c3, pile 3=d8
        // Stock: c7, c5, d3, s4. After 1 deal: pile 0=c7 (1 card), pile 1=c5+b2, pile 2=d3+c3, pile 3=s4+d8
        // Same-suit step: pile 0 (c7, coppe) → pile 1 (c5, coppe) — pile 0 had 1 card, becomes EMPTY
        // Indices 4 and 5 both must be coppe
        val deck = TiramisuDeckSetup.tutorialDeck()
        val stockCard0 = deck[4] // lands on pile 0 → c7
        val stockCard1 = deck[5] // lands on pile 1 → c5
        assertEquals("pile 0 e pile 1 dopo il deal devono condividere il seme (coppe) per il same-suit step",
                     stockCard0[0], stockCard1[0])
    }

    @Test
    fun `tutorial deck stock empties after one deal`() {
        // Only 4 cards in stock → 1 deal empties it → redeal step becomes available
        val deck = TiramisuDeckSetup.tutorialDeck()
        assertEquals("lo stock deve avere esattamente 4 carte (un solo deal)", 4, deck.size - 4)
    }
}
