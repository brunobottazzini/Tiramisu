package com.bottazzini.tiramisu

import com.bottazzini.tiramisu.utils.Difficulty
import com.bottazzini.tiramisu.utils.TiramisuGameState
import com.bottazzini.tiramisu.utils.TiramisuSolver
import org.junit.Assert.*
import org.junit.Test

class TiramisuSolverTest {

    private fun state(
        piles: List<List<String>>,
        foundations: List<String> = listOf("zero","zero","zero","zero"),
        stock: List<String> = emptyList()
    ) = TiramisuGameState(
        piles       = piles.map { it.toMutableList() },
        stock       = stock.toMutableList(),
        foundations = foundations.toMutableList(),
        redealsLeft = 1,
        difficulty  = Difficulty.NORMALE,
        initialDeck = emptyList()
    )

    @Test fun `finds foundation hint when ace available`() {
        val s = state(piles = listOf(listOf("b1"), emptyList(), emptyList(), emptyList()))
        val hint = TiramisuSolver.findHint(s)
        assertNotNull(hint)
        assertEquals(0, hint!!.fromPile)
        assertTrue(hint.toFoundation)
    }

    @Test fun `finds pile-to-pile hint for same suit`() {
        val s = state(piles = listOf(listOf("c5"), listOf("c3"), emptyList(), emptyList()))
        val hint = TiramisuSolver.findHint(s)
        assertNotNull(hint)
        assertFalse(hint!!.toFoundation)
    }

    @Test fun `returns null when no moves available`() {
        val s = state(piles = listOf(listOf("b5"), listOf("c3"), listOf("d7"), listOf("s2")))
        // No same-suit pairs, no aces, no stock
        val hint = TiramisuSolver.findHint(s)
        assertNull(hint)
    }

    @Test fun `prefers foundation over tableau move`() {
        val s = state(
            piles       = listOf(listOf("b2"), listOf("b5"), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero")
        )
        val hint = TiramisuSolver.findHint(s)
        assertNotNull(hint)
        assertTrue(hint!!.toFoundation) // foundation preferred
    }

    @Test fun `skips unhelpful single card to empty pile move`() {
        val s = state(piles = listOf(listOf("c5"), emptyList(), emptyList(), emptyList()))
        // Only possible move would be c5 → empty pile, which is unhelpful (no tableau building)
        val hint = TiramisuSolver.findHint(s)
        assertNull(hint)
    }
}
