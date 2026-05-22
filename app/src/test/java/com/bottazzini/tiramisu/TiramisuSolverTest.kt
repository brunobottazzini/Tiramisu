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
        // Under default NORMALE (strict), c5 -> c3 is invalid (5 > 3) but c3 -> c5 is valid.
        // Solver must suggest pile 1 -> pile 0.
        val s = state(piles = listOf(listOf("c5"), listOf("c3"), emptyList(), emptyList()))
        val hint = TiramisuSolver.findHint(s)
        assertNotNull(hint)
        assertFalse(hint!!.toFoundation)
        assertEquals(1, hint.fromPile)
        assertEquals(0, hint.toPile)
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

    @Test fun `under strict mode hint suggests same-suit move with lower rank as source`() {
        // pile 0 top = c7, pile 1 top = c5. difficulty = NORMALE (strict).
        // Under STRICT, only c5 -> c7 (5 < 7) is valid, NOT c7 -> c5.
        // Solver MUST suggest pile 1 -> pile 0.
        val s = state(piles = listOf(listOf("c7"), listOf("c5"), emptyList(), emptyList()))
        val hint = TiramisuSolver.findHint(s)
        assertNotNull(hint)
        assertFalse(hint!!.toFoundation)
        assertEquals(1, hint.fromPile)
        assertEquals(0, hint.toPile)
    }

    // ===== Stall-detection primitives =====

    @Test fun `foundationCardCount sums ranks across slots`() {
        val s = state(
            piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            foundations = listOf("b3", "c1", "zero", "s10")
        )
        assertEquals(3 + 1 + 0 + 10, TiramisuSolver.foundationCardCount(s))
    }

    @Test fun `foundationCardCount zero on empty foundations`() {
        val s = state(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()))
        assertEquals(0, TiramisuSolver.foundationCardCount(s))
    }

    @Test fun `canonicalKey equal for equivalent states`() {
        val s1 = state(
            piles       = listOf(listOf("c3"), listOf("c5"), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero")
        )
        val s2 = state(
            piles       = listOf(listOf("c3"), listOf("c5"), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero")
        )
        assertEquals(TiramisuSolver.canonicalKey(s1), TiramisuSolver.canonicalKey(s2))
    }

    @Test fun `canonicalKey differs when pile content differs`() {
        val s1 = state(piles = listOf(listOf("c3"), listOf("c5"), emptyList(), emptyList()))
        val s2 = state(piles = listOf(listOf("c3"), listOf("c7"), emptyList(), emptyList()))
        assertNotEquals(TiramisuSolver.canonicalKey(s1), TiramisuSolver.canonicalKey(s2))
    }

    @Test fun `enumerateLegalMoves includes foundation moves`() {
        // pile 0 top = b2, foundation bastoni already at b1 → b2 can go to foundation
        val s = state(
            piles       = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero")
        )
        val moves = TiramisuSolver.enumerateLegalMoves(s)
        assertTrue(moves.any { it.toFoundation && it.fromPile == 0 })
    }

    @Test fun `enumerateLegalMoves under strict excludes higher-rank same-suit`() {
        // pile 0 = c7, pile 1 = c5. Under strict NORMALE, c7 -> c5 is invalid, c5 -> c7 is valid.
        val s = state(piles = listOf(listOf("c7"), listOf("c5"), emptyList(), emptyList()))
        val moves = TiramisuSolver.enumerateLegalMoves(s)
        val tableauMoves = moves.filter { !it.toFoundation }
        assertTrue(tableauMoves.any { it.fromPile == 1 && it.toPile == 0 })
        assertFalse(tableauMoves.any { it.fromPile == 0 && it.toPile == 1 })
    }

    @Test fun `enumerateLegalMoves includes single-card to empty pile`() {
        // Unlike findHint(), the BFS must consider these — they can unblock other moves.
        val s = state(piles = listOf(listOf("c3"), emptyList(), emptyList(), emptyList()))
        val moves = TiramisuSolver.enumerateLegalMoves(s)
        val toEmpty = moves.filter { !it.toFoundation && it.fromPile == 0 }
        assertTrue(toEmpty.isNotEmpty())
    }

    @Test fun `applyMove tableau move moves the top card`() {
        val s = state(piles = listOf(listOf("c5"), listOf("c7"), emptyList(), emptyList()))
        val move = TiramisuSolver.Move(fromPile = 0, toPile = 1, toFoundation = false)
        val next = TiramisuSolver.applyMove(s, move)
        assertEquals(listOf<String>(), next.piles[0])
        assertEquals(listOf("c7", "c5"), next.piles[1])
    }

    @Test fun `applyMove foundation move puts card on foundation`() {
        val s = state(
            piles       = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero")
        )
        val move = TiramisuSolver.Move(fromPile = 0, toPile = -1, toFoundation = true)
        val next = TiramisuSolver.applyMove(s, move)
        assertEquals(listOf<String>(), next.piles[0])
        assertEquals("b2", next.foundations[0])
    }

    @Test fun `applyMove auto-promotes exposed ace`() {
        // pile 0 = [c1, c5]. Move c5 to pile 1 → c1 is exposed → auto-promoted to foundation.
        val s = state(piles = listOf(listOf("c1", "c5"), listOf("c7"), emptyList(), emptyList()))
        val move = TiramisuSolver.Move(fromPile = 0, toPile = 1, toFoundation = false)
        val next = TiramisuSolver.applyMove(s, move)
        assertEquals(listOf<String>(), next.piles[0])
        // c1 (ace of coppe) auto-moved to foundation
        assertTrue(next.foundations.any { it == "c1" })
    }

    @Test fun `applyMove does not mutate input state`() {
        val s = state(piles = listOf(listOf("c5"), listOf("c7"), emptyList(), emptyList()))
        val move = TiramisuSolver.Move(fromPile = 0, toPile = 1, toFoundation = false)
        TiramisuSolver.applyMove(s, move)
        // Original state must remain unchanged
        assertEquals(listOf("c5"), s.piles[0])
        assertEquals(listOf("c7"), s.piles[1])
    }
}
