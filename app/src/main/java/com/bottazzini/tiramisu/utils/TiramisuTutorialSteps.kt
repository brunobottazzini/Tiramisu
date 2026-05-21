package com.bottazzini.tiramisu.utils

import android.content.res.Resources
import com.bottazzini.tiramisu.R

/**
 * A single scripted step in the tutorial.
 * [instructionResId] — string resource for the instruction text.
 * [requiredMove]     — if non-null, the "Next" button is hidden; the user must perform the move.
 * [highlightPiles]   — pile indices (0-3) to visually highlight in blue.
 */
data class TiramisuTutorialStep(
    val instructionResId: Int,
    val requiredMove:     TiramisuTutorialMove? = null,
    val highlightPiles:   List<Int>             = emptyList()
)

/**
 * Describes a move required to advance the tutorial.
 *
 * Conventions:
 *   sourcePile = -1, targetPile = -1  → tap tallone (stock deal)
 *   sourcePile = -1, targetPile = -2  → tap Ridistribuisci (redeal)
 *   sourcePile >= 0, targetPile >= 0  → pile → pile move
 *   sourcePile >= 0, targetPile = -1  → pile → foundation move
 */
data class TiramisuTutorialMove(
    val sourcePile: Int,
    val targetPile: Int
)

object TiramisuTutorialSteps {

    /**
     * Tutorial deck state after initial deal (TiramisuDeckSetup.tutorialDeck()):
     *   pile 0 = EMPTY  (b1 auto-moved to bastoni foundation)
     *   pile 1 = b2     (bastoni — next for foundation after b1)
     *   pile 2 = c3     (coppe)
     *   pile 3 = d8     (denari)
     *   stock  = [c7, c5, d3, s4]  (4 cards — one deal empties it)
     *
     * After step 1 (deal from stock):
     *   pile 0 = c7  (coppe)
     *   pile 1 = c5  (coppe) on b2
     *   pile 2 = d3  (denari) on c3
     *   pile 3 = s4  (spade) on d8
     *   stock  = EMPTY → canRedeal() becomes true, button appears (but blocked until step 7)
     *
     * Step 3 same-suit: pile 1 (c5, coppe) → pile 0 (c7, coppe) → b2 exposed in pile 1
     * Step 5 foundation: pile 1 (b2) → bastoni foundation (has b1) → pile 1 empty
     */
    fun steps(resources: Resources): List<TiramisuTutorialStep> = listOf(

        // Step 0: Introduction — user taps "Avanti"
        TiramisuTutorialStep(
            instructionResId = R.string.tut_intro,
            requiredMove     = null
        ),

        // Step 1: Deal from stock — user must tap the tallone
        TiramisuTutorialStep(
            instructionResId = R.string.tut_deal,
            requiredMove     = TiramisuTutorialMove(sourcePile = -1, targetPile = -1)
        ),

        // Step 2: Confirmation after deal — user taps "Avanti"
        TiramisuTutorialStep(
            instructionResId = R.string.tut_deal_confirm,
            requiredMove     = null
        ),

        // Step 3: Same-suit move — pile 1 (c5, coppe) → pile 0 (c7, coppe)
        // After this move: pile 0 = [c7, c5], pile 1 = b2 exposed
        TiramisuTutorialStep(
            instructionResId = R.string.tut_same_suit,
            requiredMove     = TiramisuTutorialMove(sourcePile = 1, targetPile = 0),
            highlightPiles   = listOf(0, 1)
        ),

        // Step 4: Confirmation — user taps "Avanti"
        TiramisuTutorialStep(
            instructionResId = R.string.tut_same_suit_confirm,
            requiredMove     = null
        ),

        // Step 5: Foundation move — pile 1 (b2) → bastoni foundation
        // b2 is exposed after step 3; bastoni foundation already has b1
        TiramisuTutorialStep(
            instructionResId = R.string.tut_foundation,
            requiredMove     = TiramisuTutorialMove(sourcePile = 1, targetPile = -1),
            highlightPiles   = listOf(1)
        ),

        // Step 6: Confirmation — user taps "Avanti"
        TiramisuTutorialStep(
            instructionResId = R.string.tut_foundation_confirm,
            requiredMove     = null
        ),

        // Step 7: Redeal — stock empty since step 1; button is visible but blocked until now
        // User must tap the "Ridistribuisci" button
        TiramisuTutorialStep(
            instructionResId = R.string.tut_redeal,
            requiredMove     = TiramisuTutorialMove(sourcePile = -1, targetPile = -2)
        ),

        // Step 8: Confirmation after redeal — user taps "Avanti"
        TiramisuTutorialStep(
            instructionResId = R.string.tut_redeal_confirm,
            requiredMove     = null
        ),

        // Step 9: Finish — user taps "Avanti" → endTutorial()
        TiramisuTutorialStep(
            instructionResId = R.string.tut_finish,
            requiredMove     = null
        )
    )
}
