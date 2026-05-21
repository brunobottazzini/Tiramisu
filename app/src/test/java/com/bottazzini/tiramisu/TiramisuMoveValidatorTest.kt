package com.bottazzini.tiramisu

import com.bottazzini.tiramisu.utils.TiramisuMoveValidator
import org.junit.Assert.*
import org.junit.Test

class TiramisuMoveValidatorTest {

    @Test fun `suit extracts single character`() {
        assertEquals("c", TiramisuMoveValidator.suit("c5"))
        assertEquals("b", TiramisuMoveValidator.suit("b10"))
    }

    @Test fun `rank extracts integer correctly for two-digit rank`() {
        assertEquals(10, TiramisuMoveValidator.rank("c10"))
        assertEquals(1, TiramisuMoveValidator.rank("b1"))
    }

    @Test fun `same suit any rank allows tableau move`() {
        assertTrue(TiramisuMoveValidator.canMoveToTableau("c5", "c3"))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("b1", "b10"))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("s7", "s2"))
    }

    @Test fun `different suit blocks tableau move`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("c5", "b3"))
        assertFalse(TiramisuMoveValidator.canMoveToTableau("d1", "s1"))
    }

    @Test fun `empty pile (zero) accepts any card`() {
        assertTrue(TiramisuMoveValidator.canMoveToTableau("s7", "zero"))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("b1", "zero"))
    }

    @Test fun `zero as moving card is rejected`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("zero", "c5"))
        assertFalse(TiramisuMoveValidator.canMoveToTableau("zero", "zero"))
    }

    @Test fun `ace goes to empty foundation`() {
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("b1", "zero"))
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("s1", "zero"))
    }

    @Test fun `non-ace blocked from empty foundation`() {
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("b2", "zero"))
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("c5", "zero"))
    }

    @Test fun `ascending same suit allowed in foundation`() {
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("c2", "c1"))
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("b10", "b9"))
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("d5", "d4"))
    }

    @Test fun `skipping ranks blocked in foundation`() {
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("c3", "c1"))
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("b5", "b3"))
    }

    @Test fun `different suit blocked in foundation`() {
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("b2", "c1"))
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("d3", "s2"))
    }

    @Test fun `zero moving card rejected in foundation`() {
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("zero", "c1"))
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("zero", "zero"))
    }
}
