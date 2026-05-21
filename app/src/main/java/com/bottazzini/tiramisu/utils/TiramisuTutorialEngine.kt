package com.bottazzini.tiramisu.utils

/**
 * Manages tutorial step progression for Tiramisù.
 *
 * The engine enforces that required moves are performed before "Next" is available.
 * Info steps (requiredMove == null) show the "Next" button immediately.
 */
class TiramisuTutorialEngine(private val steps: List<TiramisuTutorialStep>) {

    private var index           = 0
    private var moveExecuted    = false

    fun currentStep(): TiramisuTutorialStep = steps[index.coerceAtMost(steps.lastIndex)]

    fun isComplete(): Boolean = index >= steps.size

    /**
     * Advance to the next step.
     * For steps with requiredMove, only advances if the move has been executed.
     */
    fun advanceToNext() {
        if (isComplete()) return
        val step = currentStep()
        if (step.requiredMove != null && !moveExecuted) return   // not done yet
        index++
        moveExecuted = false
    }

    /**
     * Returns true if the current step expects a stock tap.
     */
    fun isStockDealStep(): Boolean {
        if (isComplete()) return false
        val move = currentStep().requiredMove ?: return false
        return move.sourcePile == -1
    }

    /**
     * Returns true if tapping pile [pileIdx] is the expected action for the current step.
     * If the step has no requiredMove, any tap is allowed.
     */
    fun isPileTapAllowed(pileIdx: Int, card: String): Boolean {
        if (isComplete()) return false
        val move = currentStep().requiredMove ?: return true   // no restriction
        if (move.sourcePile == -1) return false                // stock step, pile tap not expected
        return move.sourcePile == pileIdx
    }

    /**
     * Call after a pile-to-pile or pile-to-foundation move is executed.
     * Marks the current required move as done if it matches.
     */
    fun onMoveExecuted(srcPile: Int, dstPile: Int) {
        if (isComplete()) return
        val required = currentStep().requiredMove ?: return
        if (required.sourcePile == srcPile && required.targetPile == dstPile) {
            moveExecuted = true
        }
    }
}
