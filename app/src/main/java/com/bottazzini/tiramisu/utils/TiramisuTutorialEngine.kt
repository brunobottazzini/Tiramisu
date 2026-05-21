package com.bottazzini.tiramisu.utils

/**
 * Manages tutorial step progression for Tiramisù.
 *
 * The engine advances via [advanceToNext]; guards against wrong moves live in GameActivity.
 * Info steps (requiredMove == null) show the "Next" button immediately.
 */
class TiramisuTutorialEngine(private val steps: List<TiramisuTutorialStep>) {

    private var index = 0

    fun currentStep(): TiramisuTutorialStep = steps[index.coerceAtMost(steps.lastIndex)]

    fun isComplete(): Boolean = index >= steps.size

    /** Advance to the next step unconditionally. */
    fun advanceToNext() {
        if (isComplete()) return
        index++
    }

    /** True if the current step expects a tap on the stock (tallone). */
    fun isStockDealStep(): Boolean {
        if (isComplete()) return false
        val move = currentStep().requiredMove ?: return false
        return move.sourcePile == -1 && move.targetPile == -1
    }

    /** True if the current step expects a tap on the Ridistribuisci button. */
    fun isRedealStep(): Boolean {
        if (isComplete()) return false
        val move = currentStep().requiredMove ?: return false
        return move.sourcePile == -1 && move.targetPile == -2
    }

    /**
     * Returns true if tapping pile [pileIdx] is the expected action for the current step.
     * If the step has no requiredMove, any tap is allowed.
     */
    fun isPileTapAllowed(pileIdx: Int, card: String): Boolean {
        if (isComplete()) return false
        val move = currentStep().requiredMove ?: return true   // no restriction
        if (move.sourcePile == -1) return false                // stock or redeal step
        return move.sourcePile == pileIdx
    }

    /**
     * True if moving [srcPile] → [dstPile] is the correct move for the current step.
     * Returns true if the current step has no restriction (info step).
     */
    fun isCorrectPileMove(srcPile: Int, dstPile: Int): Boolean {
        if (isComplete()) return false
        val move = currentStep().requiredMove ?: return true
        return move.sourcePile == srcPile && move.targetPile == dstPile
    }

    /**
     * True if moving [srcPile] to the foundation is the correct move for the current step.
     * Returns true if the current step has no restriction (info step).
     */
    fun isCorrectFoundationMove(srcPile: Int): Boolean {
        if (isComplete()) return false
        val move = currentStep().requiredMove ?: return true
        return move.sourcePile == srcPile && move.targetPile == -1
    }
}
