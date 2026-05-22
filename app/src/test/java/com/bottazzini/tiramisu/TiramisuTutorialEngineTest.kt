package com.bottazzini.tiramisu

import com.bottazzini.tiramisu.utils.TiramisuTutorialEngine
import com.bottazzini.tiramisu.utils.TiramisuTutorialMove
import com.bottazzini.tiramisu.utils.TiramisuTutorialStep
import org.junit.Assert.*
import org.junit.Test

class TiramisuTutorialEngineTest {

    private fun infoStep() = TiramisuTutorialStep(instructionResId = 0, requiredMove = null)
    private fun stockStep() = TiramisuTutorialStep(
        instructionResId = 0,
        requiredMove = TiramisuTutorialMove(sourcePile = -1, targetPile = -1)
    )
    private fun redealStep() = TiramisuTutorialStep(
        instructionResId = 0,
        requiredMove = TiramisuTutorialMove(sourcePile = -1, targetPile = -2)
    )
    private fun pileStep(src: Int, dst: Int) = TiramisuTutorialStep(
        instructionResId = 0,
        requiredMove = TiramisuTutorialMove(sourcePile = src, targetPile = dst)
    )
    private fun foundationStep(src: Int) = TiramisuTutorialStep(
        instructionResId = 0,
        requiredMove = TiramisuTutorialMove(sourcePile = src, targetPile = -1)
    )

    @Test
    fun `starts at step 0`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep(), infoStep()))
        assertFalse(eng.isComplete())
    }

    @Test
    fun `isComplete after all steps`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep()))
        eng.advanceToNext()
        assertTrue(eng.isComplete())
    }

    @Test
    fun `advanceToNext increments step`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep(), infoStep(), infoStep()))
        eng.advanceToNext()
        // currentStep should now be second step
        assertFalse(eng.isComplete())
        eng.advanceToNext()
        assertFalse(eng.isComplete())
        eng.advanceToNext()
        assertTrue(eng.isComplete())
    }

    @Test
    fun `advanceToNext does nothing when complete`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep()))
        eng.advanceToNext()
        eng.advanceToNext() // called again on complete — must not crash
        assertTrue(eng.isComplete())
    }

    @Test
    fun `isStockDealStep true when sourcePile -1 targetPile -1`() {
        val eng = TiramisuTutorialEngine(listOf(stockStep()))
        assertTrue(eng.isStockDealStep())
    }

    @Test
    fun `isStockDealStep false for info step`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep()))
        assertFalse(eng.isStockDealStep())
    }

    @Test
    fun `isStockDealStep false for redeal step`() {
        val eng = TiramisuTutorialEngine(listOf(redealStep()))
        assertFalse(eng.isStockDealStep())
    }

    @Test
    fun `isRedealStep true when sourcePile -1 targetPile -2`() {
        val eng = TiramisuTutorialEngine(listOf(redealStep()))
        assertTrue(eng.isRedealStep())
    }

    @Test
    fun `isRedealStep false for stock deal step`() {
        val eng = TiramisuTutorialEngine(listOf(stockStep()))
        assertFalse(eng.isRedealStep())
    }

    @Test
    fun `isRedealStep false when complete`() {
        val eng = TiramisuTutorialEngine(listOf(redealStep()))
        eng.advanceToNext()
        assertFalse(eng.isRedealStep())
    }

    @Test
    fun `isPileTapAllowed true for correct source pile`() {
        val eng = TiramisuTutorialEngine(listOf(pileStep(src = 1, dst = 0)))
        assertTrue(eng.isPileTapAllowed(1, "c7"))
    }

    @Test
    fun `isPileTapAllowed false for wrong source pile`() {
        val eng = TiramisuTutorialEngine(listOf(pileStep(src = 1, dst = 0)))
        assertFalse(eng.isPileTapAllowed(2, "c3"))
    }

    @Test
    fun `isPileTapAllowed false on info step (moves blocked to prevent state corruption)`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep()))
        assertFalse(eng.isPileTapAllowed(0, "c7"))
        assertFalse(eng.isPileTapAllowed(3, "d8"))
    }

    @Test
    fun `isPileTapAllowed false on stock step`() {
        val eng = TiramisuTutorialEngine(listOf(stockStep()))
        assertFalse(eng.isPileTapAllowed(0, "c7"))
    }

    @Test
    fun `isCorrectPileMove true when src and dst match step`() {
        val eng = TiramisuTutorialEngine(listOf(pileStep(src = 1, dst = 0)))
        assertTrue(eng.isCorrectPileMove(srcPile = 1, dstPile = 0))
    }

    @Test
    fun `isCorrectPileMove false when dst is wrong`() {
        val eng = TiramisuTutorialEngine(listOf(pileStep(src = 1, dst = 0)))
        assertFalse(eng.isCorrectPileMove(srcPile = 1, dstPile = 2))
    }

    @Test
    fun `isCorrectPileMove false on info step (moves blocked to prevent state corruption)`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep()))
        assertFalse(eng.isCorrectPileMove(srcPile = 0, dstPile = 3))
    }

    @Test
    fun `isCorrectFoundationMove true when src matches foundation step`() {
        val eng = TiramisuTutorialEngine(listOf(foundationStep(src = 1)))
        assertTrue(eng.isCorrectFoundationMove(srcPile = 1))
    }

    @Test
    fun `isCorrectFoundationMove false when src is wrong pile`() {
        val eng = TiramisuTutorialEngine(listOf(foundationStep(src = 1)))
        assertFalse(eng.isCorrectFoundationMove(srcPile = 2))
    }

    @Test
    fun `isCorrectFoundationMove false on info step (moves blocked to prevent state corruption)`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep()))
        assertFalse(eng.isCorrectFoundationMove(srcPile = 0))
    }
}
