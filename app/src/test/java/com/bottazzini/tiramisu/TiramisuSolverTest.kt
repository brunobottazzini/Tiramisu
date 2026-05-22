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

    // ===== canProgress: progress detected =====

    @Test fun `canProgress true when foundation move available immediately`() {
        val s = state(
            piles       = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero")
        )
        assertTrue(TiramisuSolver.canProgress(s, 30))
    }

    @Test fun `canProgress true when ace is reachable in 2 moves`() {
        // pile 0 = [c1, c5], pile 1 = c7. Move c5 -> c7 (5<7 strict ok), c1 auto-promotes.
        val s = state(piles = listOf(listOf("c1", "c5"), listOf("c7"), emptyList(), emptyList()))
        assertTrue(TiramisuSolver.canProgress(s, 2))
        assertTrue(TiramisuSolver.canProgress(s, 30))
    }

    @Test fun `canProgress true when single card to empty pile unblocks foundation`() {
        // pile 0 = s5, pile 1 = [s1, s3], pile 2 = empty, pile 3 = empty.
        // Move s3 -> pile 2 (single -> empty), then s1 exposed -> foundation.
        val s = state(
            piles = listOf(listOf("s5"), listOf("s1", "s3"), emptyList(), emptyList())
        )
        assertTrue(TiramisuSolver.canProgress(s, 30))
    }

    @Test fun `canProgress true when foundation at 9 and 10 reachable`() {
        val s = state(
            piles       = listOf(listOf("b10"), emptyList(), emptyList(), emptyList()),
            foundations = listOf("b9", "zero", "zero", "zero")
        )
        assertTrue(TiramisuSolver.canProgress(s, 30))
    }

    @Test fun `canProgress true on freshly dealt state (sanity)`() {
        // Sanity: b1 already in foundation, b2 is on a pile → immediate foundation move.
        // Other piles contain unrelated cards. canProgress must find b2 → foundation.
        val s = state(
            piles       = listOf(listOf("b2"), listOf("s6"), listOf("c3"), listOf("d8")),
            foundations = listOf("b1", "zero", "zero", "zero")
        )
        assertTrue(TiramisuSolver.canProgress(s, 30))
    }

    // ===== canProgress: stall detected =====

    @Test fun `no progress in classic 2-pile cycle c3 c5 strict`() {
        val s = state(
            piles = listOf(listOf("c3"), listOf("c5"), emptyList(), emptyList())
        )
        assertFalse(TiramisuSolver.canProgress(s, 30))
    }

    @Test fun `no progress when only buried aces and high cards block them`() {
        // Aces are at the bottom of piles, covered by cards that can't be moved off:
        // pile 0 = [c1, c10] (under strict, c10 can't move on any pile top of same suit
        //                     and other suits = different suit = blocked)
        // pile 1 = [b1, b10]
        // pile 2 = [d1, d10]
        // pile 3 = [s1, s10]
        // No tableau moves possible (all tops different suits, and even if same-suit,
        // a 10 has no higher destination). No foundation moves. Stalled.
        val s = state(
            piles = listOf(
                listOf("c1", "c10"),
                listOf("b1", "b10"),
                listOf("d1", "d10"),
                listOf("s1", "s10")
            )
        )
        assertFalse(TiramisuSolver.canProgress(s, 30))
    }

    // ===== canProgress: difficulty awareness =====

    @Test fun `canProgress under FACILE reaches the ace in one move where strict needs more`() {
        // pile 0 = [c1, c7], pile 1 = c5, pile 2 = b3, pile 3 = d4.
        //
        // Under FACILE (lax): c7 -> c5 is legal in one move; c1 then auto-promotes.
        //   canProgress(s, 1) must already be true.
        //
        // Under strict NORMALE with run-moves enabled: c7 -> c5 is illegal (7 > 5), but
        // c5 -> c7 builds the run [c7, c5] which can then be shoveled onto pile 1 (empty),
        // exposing c1. That takes 2 moves, so canProgress(s, 1) is false and canProgress(s, 30)
        // is true.
        val sStrict = state(
            piles       = listOf(listOf("c1", "c7"), listOf("c5"), listOf("b3"), listOf("d4")),
            foundations = listOf("zero", "zero", "zero", "zero")
        )
        assertFalse(
            "strict should not find progress in a single move",
            TiramisuSolver.canProgress(sStrict, 1)
        )
        assertTrue(
            "strict should still eventually progress via run move",
            TiramisuSolver.canProgress(sStrict, 30)
        )

        val sLax = TiramisuGameState(
            piles       = listOf(
                mutableListOf("c1", "c7"),
                mutableListOf("c5"),
                mutableListOf("b3"),
                mutableListOf("d4")
            ),
            stock       = mutableListOf(),
            foundations = mutableListOf("zero", "zero", "zero", "zero"),
            redealsLeft = 0,
            difficulty  = Difficulty.FACILE,
            initialDeck = emptyList()
        )
        assertTrue(
            "lax should progress in one move",
            TiramisuSolver.canProgress(sLax, 1)
        )
    }

    // ===== Run-move support =====

    @Test fun `findHint suggests run move when single-card move is impossible (strict)`() {
        // pile 0 = [d8], pile 1 = [d7, d5, d3]. Strict: d3 alone can't go on d8 (foundation only),
        // and d3 onto d8 is invalid as a single card (3 < 8 but only the BASE of the run matters).
        // Actually d3 alone IS strict-valid onto d8 (same suit, 3 < 8). But topMovableRun returns
        // the full [d7, d5, d3] which also fits (7 < 8). Hint should fire on pile 1 -> pile 0.
        val s = state(
            piles = listOf(listOf("d8"), listOf("d7", "d5", "d3"), emptyList(), emptyList())
        )
        val hint = TiramisuSolver.findHint(s)
        assertNotNull(hint)
        assertFalse(hint!!.toFoundation)
        assertEquals(1, hint.fromPile)
        assertEquals(0, hint.toPile)
    }

    @Test fun `enumerateLegalMoves yields one move per src-dst pair when a run fits`() {
        // pile 0 = [d8], pile 1 = [d7, d5, d3]. (d5, d3 alone can also fit onto d8 in strict,
        // but enumerateLegalMoves returns ONE Move per (src,dst) — applyMove picks the longest.)
        val s = state(
            piles = listOf(listOf("d8"), listOf("d7", "d5", "d3"), emptyList(), emptyList())
        )
        val tableauMoves = TiramisuSolver.enumerateLegalMoves(s)
            .filter { !it.toFoundation && it.fromPile == 1 && it.toPile == 0 }
        assertEquals(1, tableauMoves.size)
    }

    @Test fun `applyMove tableau move transfers the entire movable run`() {
        // pile 0 = [d8], pile 1 = [d7, d5, d3]. Move (1 -> 0) should lift [d7, d5, d3].
        val s = state(
            piles = listOf(listOf("d8"), listOf("d7", "d5", "d3"), emptyList(), emptyList())
        )
        val move = TiramisuSolver.Move(fromPile = 1, toPile = 0, toFoundation = false)
        val next = TiramisuSolver.applyMove(s, move)
        assertEquals(listOf("d8", "d7", "d5", "d3"), next.piles[0])
        assertEquals(emptyList<String>(), next.piles[1])
    }

    @Test fun `applyMove tableau move transfers only the largest fitting sub-run (strict)`() {
        // pile 0 = [d4], pile 1 = [d7, d5, d3]. Strict: full run base d7 > d4 so doesn't fit;
        // sub-run [d3] fits (3 < 4). Move should lift just d3.
        val s = state(
            piles = listOf(listOf("d4"), listOf("d7", "d5", "d3"), emptyList(), emptyList())
        )
        val move = TiramisuSolver.Move(fromPile = 1, toPile = 0, toFoundation = false)
        val next = TiramisuSolver.applyMove(s, move)
        assertEquals(listOf("d4", "d3"), next.piles[0])
        assertEquals(listOf("d7", "d5"), next.piles[1])
    }

    @Test fun `canProgress under strict exposes buried ace via run move`() {
        // pile 0 = [c1, c5, c3], pile 1 = c8, pile 2 = b2, pile 3 = d4.
        // topRun(pile 0) = [c5, c3] (5 > 3, same suit).
        // Strict: src=0 -> dst=1 (c8) — topMovableRun([c1,c5,c3], c8, strict) = [c5, c3] (5 < 8).
        //         Result: pile 0 = [c1] → c1 auto-promotes. Progress in one move.
        val s = state(
            piles = listOf(listOf("c1", "c5", "c3"), listOf("c8"), listOf("b2"), listOf("d4"))
        )
        assertTrue(TiramisuSolver.canProgress(s, 1))
    }

    // ===== canProgress: boundary & performance =====

    @Test fun `canProgress respects maxDepth boundary`() {
        // Construct a state where progress requires exactly 3 moves.
        // pile 0 = [c1, c3, c5], pile 1 = c7, pile 2 = empty, pile 3 = empty.
        // Steps: c5 -> c7 (5<7 ok, depth 1) → top c3 -> pile 2 (single -> empty, depth 2) →
        //        c1 exposed → auto-promote in same applyMove (no separate move needed)
        // Actually: after c3 -> pile 2, c1 is on pile 0 top → autoPromoteAces runs in applyMove,
        // so progress is detected at depth 2.
        val s = state(
            piles = listOf(listOf("c1", "c3", "c5"), listOf("c7"), emptyList(), emptyList())
        )
        assertTrue(TiramisuSolver.canProgress(s, 2))
        // With maxDepth = 1, BFS can only do one move (c5 -> c7), which doesn't expose c1 alone.
        // After c5 -> c7, pile 0 = [c1, c3] (no progress yet). depth 1 nodes are not expanded
        // further because depth >= maxDepth. So canProgress(s, 1) = false.
        assertFalse(TiramisuSolver.canProgress(s, 1))
    }

    @Test fun `canProgress terminates within 200ms on dense state`() {
        // Dense state: 4 piles with 7 cards each (28 cards), foundations all empty.
        // No aces visible to short-circuit. Goal: confirm BFS doesn't blow up.
        val s = state(
            piles = listOf(
                listOf("c10","c9","c8","c7","c6","c5","c4"),
                listOf("b10","b9","b8","b7","b6","b5","b4"),
                listOf("d10","d9","d8","d7","d6","d5","d4"),
                listOf("s10","s9","s8","s7","s6","s5","s4")
            )
        )
        val startNs = System.nanoTime()
        val result = TiramisuSolver.canProgress(s, 30)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        assertTrue("canProgress took ${elapsedMs}ms (expected < 200)", elapsedMs < 200)
        // We don't assert on result — the test is purely a performance regression test.
        // (For the record, this particular state is stalled since no aces are visible
        // and no foundation can be started.)
        assertFalse(result)  // sanity, since no aces are visible and no progress is possible
    }
}
