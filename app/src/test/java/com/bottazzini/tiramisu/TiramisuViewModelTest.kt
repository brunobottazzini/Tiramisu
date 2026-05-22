package com.bottazzini.tiramisu

import com.bottazzini.tiramisu.utils.Difficulty
import com.bottazzini.tiramisu.utils.TiramisuGameState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TiramisuViewModelTest {

    private lateinit var vm: TiramisuViewModel

    @Before
    fun setup() {
        vm = TiramisuViewModel()
    }

    private fun stateWith(
        piles: List<List<String>>,
        stock: List<String> = emptyList(),
        foundations: List<String> = listOf("zero", "zero", "zero", "zero"),
        redeals: Int = 1,
        difficulty: Difficulty = Difficulty.NORMALE
    ): TiramisuGameState {
        val gs = TiramisuGameState(
            piles       = piles.map { it.toMutableList() },
            stock       = stock.toMutableList(),
            foundations = foundations.toMutableList(),
            redealsLeft = redeals,
            difficulty  = difficulty,
            initialDeck = emptyList(),
            hasActiveGame = true
        )
        vm.restoreState(gs)
        return gs
    }

    @Test fun `dealFromStock deals one card per pile`() {
        stateWith(
            piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            stock = listOf("b3", "c5", "d7", "s2")
        )
        val dealt = vm.dealFromStock()
        assertTrue(dealt)
        val s = vm.state!!
        assertEquals("b3", s.topOfPile(0))
        assertEquals("c5", s.topOfPile(1))
        assertEquals("d7", s.topOfPile(2))
        assertEquals("s2", s.topOfPile(3))
        assertTrue(s.stock.isEmpty())
    }

    @Test fun `dealFromStock returns false when stock is empty`() {
        stateWith(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()))
        assertFalse(vm.dealFromStock())
    }

    @Test fun `dealFromStock auto-moves ace to foundation`() {
        stateWith(
            piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            stock = listOf("b1", "c5", "d7", "s2")
        )
        vm.dealFromStock()
        val s = vm.state!!
        assertTrue("ace not auto-moved", s.foundations.any { it == "b1" })
        assertEquals("zero", s.topOfPile(0))
    }

    @Test fun `tapping pile with card selects it`() {
        stateWith(piles = listOf(listOf("c5"), emptyList(), emptyList(), emptyList()))
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.SELECTED, result)
        assertEquals(0, vm.selectedPileIndex)
    }

    @Test fun `tapping empty pile does nothing`() {
        stateWith(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()))
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.NOTHING, result)
        assertNull(vm.selectedPileIndex)
    }

    @Test fun `tapping selected pile deselects`() {
        stateWith(piles = listOf(listOf("c5"), emptyList(), emptyList(), emptyList()))
        vm.onPileTapped(0)
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.DESELECTED, result)
        assertNull(vm.selectedPileIndex)
    }

    @Test fun `moving same-suit card succeeds`() {
        stateWith(piles = listOf(listOf("c5"), listOf("c3"), emptyList(), emptyList()), difficulty = Difficulty.FACILE)
        vm.onPileTapped(0)
        val result = vm.onPileTapped(1)
        assertEquals(TapResult.MOVED, result)
        val s = vm.state!!
        assertEquals("c5", s.topOfPile(1))
        assertEquals("zero", s.topOfPile(0))
    }

    @Test fun `moving different-suit card is invalid`() {
        stateWith(piles = listOf(listOf("c5"), listOf("b3"), emptyList(), emptyList()))
        vm.onPileTapped(0)
        val result = vm.onPileTapped(1)
        assertEquals(TapResult.INVALID, result)
        val s = vm.state!!
        assertEquals("c5", s.topOfPile(0))
        assertEquals("b3", s.topOfPile(1))
    }

    @Test fun `redeal moves piles right-to-left into stock`() {
        stateWith(
            piles = listOf(listOf("b2"), listOf("c5"), listOf("d7"), listOf("s9")),
            stock = emptyList(),
            redeals = 1
        )
        val ok = vm.redeal()
        assertTrue(ok)
        val s = vm.state!!
        assertEquals(listOf("s9", "d7", "c5", "b2"), s.stock.toList())
        assertEquals(0, s.redealsLeft)
        assertTrue(s.piles.all { it.isEmpty() })
    }

    @Test fun `redeal fails when stock is not empty`() {
        stateWith(
            piles = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
            stock = listOf("c5"),
            redeals = 1
        )
        assertFalse(vm.redeal())
    }

    @Test fun `redeal fails when redealsLeft is 0`() {
        stateWith(
            piles = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
            stock = emptyList(),
            redeals = 0
        )
        assertFalse(vm.redeal())
    }

    @Test fun `obbligato blocks pile-to-pile when foundation move available`() {
        stateWith(
            piles = listOf(listOf("b2"), listOf("b3"), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero"),
            difficulty = Difficulty.DIFFICILE
        )
        // b2 can go to foundation (b1+1=b2), so obbligato blocks other moves
        vm.onPileTapped(1) // select b3
        val result = vm.onPileTapped(0) // try to move b3 onto b2
        assertEquals(TapResult.INVALID, result)
    }

    @Test fun `obbligato not active in NORMALE mode`() {
        stateWith(
            piles = listOf(listOf("b3"), listOf("b2"), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero"),
            difficulty = Difficulty.NORMALE
        )
        // Select pile 1 (b2), then tap pile 0 → b2 moves onto b3 (strict-valid: 2 < 3).
        // b2 could also go to the bastoni foundation (b1 → b2), but obbligato is OFF in NORMALE
        // so the user is free to move it to pile 0 instead.
        vm.onPileTapped(1)
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.MOVED, result)
    }

    @Test fun `isWon returns true when all foundations complete`() {
        stateWith(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
                  foundations = listOf("b10", "c10", "d10", "s10"))
        assertTrue(vm.isWon())
    }

    @Test fun `isWon returns false when foundations incomplete`() {
        stateWith(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
                  foundations = listOf("b10", "c9", "zero", "s10"))
        assertFalse(vm.isWon())
    }

    @Test fun `dealFromStock auto-moves ace and records it for animation`() {
        stateWith(
            piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            stock = listOf("b1", "c5", "d7", "s2")
        )
        vm.dealFromStock()
        val moves = vm.consumeAutoFoundationMoves()
        assertEquals(1, moves.size)
        val m = moves[0]
        assertEquals("b1", m.card)
        assertEquals(0, m.fromPile)
        assertEquals(AutoFoundationSource.STOCK, m.source)
        // Foundation now holds the ace
        assertEquals("b1", vm.state!!.foundations[m.toFoundation])
        // Consuming clears the slot
        assertTrue(vm.consumeAutoFoundationMoves().isEmpty())
    }

    @Test fun `isLost true in classic c3 c5 cycle stall under NORMALE`() {
        // Stock empty, no redeal, piles only have a cyclic c3 <-> c5 layout.
        stateWith(
            piles       = listOf(listOf("c3"), listOf("c5"), emptyList(), emptyList()),
            foundations = listOf("zero","zero","zero","zero"),
            stock       = emptyList(),
            redeals     = 0,
            difficulty  = Difficulty.NORMALE
        )
        assertTrue(vm.isLost())
    }

    @Test fun `isLost false when foundation move available despite cycle alternative`() {
        // pile 0 = b2 (can go to foundation since b1 is in foundation slot 0).
        // pile 1 / pile 2 form a c3 <-> c5 cyclic pair as a distractor.
        stateWith(
            piles       = listOf(listOf("b2"), listOf("c3"), listOf("c5"), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero"),
            stock       = emptyList(),
            redeals     = 0,
            difficulty  = Difficulty.NORMALE
        )
        assertFalse(vm.isLost())
    }

    @Test fun `auto-complete disabled keeps only aces auto-moving`() {
        stateWith(
            piles = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero"),
            stock = listOf("c1", "d5", "s7", "b8")
        )
        vm.autoCompleteEnabled = false
        vm.dealFromStock()
        val s = vm.state!!
        assertTrue("ace c1 must auto-move", s.foundations.any { it == "c1" })
        assertEquals("b2", s.topOfPile(0))
    }

    @Test fun `auto-complete enabled moves any foundation-eligible top to foundation`() {
        stateWith(
            piles = listOf(listOf("b3", "b2"), emptyList(), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero")
        )
        vm.autoCompleteEnabled = true
        val moved = vm.onFoundationTapped(0)
        assertTrue("b2 went to foundation", moved)
        val s = vm.state!!
        // b2 was sent up by onFoundationTapped, then auto-move chained b3 on top of it,
        // overwriting foundations[0]. Final state shows b3 (the chain endpoint).
        assertEquals("b3", s.foundations[0])
        assertEquals("zero", s.topOfPile(0))
    }

    // --- PoC: run-based pile-to-pile moves -----------------------------------

    @Test fun `NORMALE moves the full top run when base fits destination`() {
        // pile 0 = [d8] ; pile 1 = [s9, d7, d5, d3] (top = d3, top run = d7→d5→d3)
        // Tap pile 1 then pile 0 → all three denari should slide onto d8.
        stateWith(
            piles = listOf(
                listOf("d8"),
                listOf("s9", "d7", "d5", "d3"),
                emptyList(),
                emptyList()
            ),
            difficulty = Difficulty.NORMALE
        )
        vm.onPileTapped(1)
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.MOVED, result)
        val s = vm.state!!
        assertEquals(listOf("d8", "d7", "d5", "d3"), s.piles[0])
        assertEquals(listOf("s9"), s.piles[1])
    }

    @Test fun `NORMALE moves only the sub-run that fits the destination`() {
        // pile 0 = [d4] ; pile 1 = [d7, d5, d3]
        // Strict: full run base d7 does not fit d4 (7 > 4) but [d3] does (3 < 4).
        stateWith(
            piles = listOf(
                listOf("d4"),
                listOf("d7", "d5", "d3"),
                emptyList(),
                emptyList()
            ),
            difficulty = Difficulty.NORMALE
        )
        vm.onPileTapped(1)
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.MOVED, result)
        val s = vm.state!!
        assertEquals(listOf("d4", "d3"), s.piles[0])
        assertEquals(listOf("d7", "d5"), s.piles[1])
    }

    @Test fun `NORMALE rejects the move when no sub-run fits`() {
        // pile 0 = [d2] ; pile 1 = [d7, d5, d3]. Nothing under d2 in denari.
        stateWith(
            piles = listOf(
                listOf("d2"),
                listOf("d7", "d5", "d3"),
                emptyList(),
                emptyList()
            ),
            difficulty = Difficulty.NORMALE
        )
        vm.onPileTapped(1)
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.INVALID, result)
        val s = vm.state!!
        assertEquals(listOf("d2"), s.piles[0])
        assertEquals(listOf("d7", "d5", "d3"), s.piles[1])
    }

    @Test fun `NORMALE drop on empty pile moves only the top card`() {
        // pile 1 = [s9, d7, d5, d3]; pile 0 empty. Under Normale (emptyPileSingleCard=true),
        // dragging from pile 1 onto pile 0 should move ONLY d3, leaving the run base behind.
        stateWith(
            piles = listOf(
                emptyList(),
                listOf("s9", "d7", "d5", "d3"),
                emptyList(),
                emptyList()
            ),
            difficulty = Difficulty.NORMALE
        )
        vm.onPileTapped(1)
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.MOVED, result)
        val s = vm.state!!
        assertEquals(listOf("d3"), s.piles[0])
        assertEquals(listOf("s9", "d7", "d5"), s.piles[1])
    }

    @Test fun `FACILE drop on empty pile moves the full run`() {
        // Same layout, Difficulty.FACILE — the full top run d7,d5,d3 should land on the empty pile.
        stateWith(
            piles = listOf(
                emptyList(),
                listOf("s9", "d7", "d5", "d3"),
                emptyList(),
                emptyList()
            ),
            difficulty = Difficulty.FACILE
        )
        vm.onPileTapped(1)
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.MOVED, result)
        val s = vm.state!!
        assertEquals(listOf("d7", "d5", "d3"), s.piles[0])
        assertEquals(listOf("s9"), s.piles[1])
    }

    @Test fun `FACILE lax rule accepts run whose base is higher than destination`() {
        // pile 0 = [d4] ; pile 1 = [d7, d5, d3]. Lax: same suit any rank → run base d7 onto d4 OK.
        stateWith(
            piles = listOf(
                listOf("d4"),
                listOf("d7", "d5", "d3"),
                emptyList(),
                emptyList()
            ),
            difficulty = Difficulty.FACILE
        )
        vm.onPileTapped(1)
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.MOVED, result)
        val s = vm.state!!
        assertEquals(listOf("d4", "d7", "d5", "d3"), s.piles[0])
        assertEquals(emptyList<String>(), s.piles[1])
    }
}
