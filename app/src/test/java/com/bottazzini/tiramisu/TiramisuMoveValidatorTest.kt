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
        assertTrue(TiramisuMoveValidator.canMoveToTableau("c5", "c3", strict = false))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("b1", "b10", strict = false))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("s7", "s2", strict = false))
    }

    @Test fun `different suit blocks tableau move`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("c5", "b3", strict = false))
        assertFalse(TiramisuMoveValidator.canMoveToTableau("d1", "s1", strict = false))
    }

    @Test fun `empty pile (zero) accepts any card`() {
        assertTrue(TiramisuMoveValidator.canMoveToTableau("s7", "zero", strict = false))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("b1", "zero", strict = false))
    }

    @Test fun `zero as moving card is rejected`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("zero", "c5", strict = false))
        assertFalse(TiramisuMoveValidator.canMoveToTableau("zero", "zero", strict = false))
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

    // --- Strict mode (regola severa: same suit + strictly lower rank) ---

    @Test fun `strict allows same suit with strictly lower rank`() {
        assertTrue(TiramisuMoveValidator.canMoveToTableau("c3", "c5", strict = true))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("b1", "b10", strict = true))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("s2", "s7", strict = true))
    }

    @Test fun `strict rejects same suit with equal rank`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("c5", "c5", strict = true))
    }

    @Test fun `strict rejects same suit with higher rank`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("c7", "c5", strict = true))
        assertFalse(TiramisuMoveValidator.canMoveToTableau("b10", "b1", strict = true))
    }

    @Test fun `strict rejects different suit regardless of rank`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("c3", "b5", strict = true))
        assertFalse(TiramisuMoveValidator.canMoveToTableau("d2", "s7", strict = true))
    }

    @Test fun `strict still accepts any card on empty pile`() {
        assertTrue(TiramisuMoveValidator.canMoveToTableau("s10", "zero", strict = true))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("b1",  "zero", strict = true))
    }

    @Test fun `strict still rejects zero as moving card`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("zero", "c5",   strict = true))
        assertFalse(TiramisuMoveValidator.canMoveToTableau("zero", "zero", strict = true))
    }
}
