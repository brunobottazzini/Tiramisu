package com.bottazzini.tiramisu.utils

import android.content.res.Resources
import com.bottazzini.tiramisu.R

/**
 * A single scripted step in the tutorial.
 * [instructionResId] — string resource for the instruction text.
 * [requiredMove]     — if non-null, the "Next" button is hidden; the user must perform the move.
 * [highlightPiles]   — pile indices (0-3) to visually highlight.
 */
data class TiramisuTutorialStep(
    val instructionResId: Int,
    val requiredMove:     TiramisuTutorialMove? = null,
    val highlightPiles:   List<Int>             = emptyList()
)

/**
 * Describes a move required to advance the tutorial.
 * [sourcePile] = 0-3 (pile index), or -1 for "stock tap"
 * [targetPile] = 0-3 (pile index), or -1 for "to foundation", or -2 for "redeal"
 */
data class TiramisuTutorialMove(
    val sourcePile: Int,
    val targetPile: Int
)

object TiramisuTutorialSteps {

    /**
     * Returns the scripted tutorial steps for the Tiramisù tutorial.
     *
     * Tutorial deck (from TiramisuDeckSetup.tutorialDeck()):
     *   First deal (piles 0-3): b1, c5, c3, d8
     *     → b1 auto-moves to foundation → pile 0 becomes empty
     *     → pile 1 has c5, pile 2 has c3 (both coppe — same-suit move possible: pile1→pile2)
     *     → pile 3 has d8
     */
    fun steps(resources: Resources): List<TiramisuTutorialStep> = listOf(

        // Step 0: Introduction info — user taps "Avanti"
        TiramisuTutorialStep(
            instructionResId = R.string.tut_intro,
            requiredMove     = null
        ),

        // Step 1: Deal from stock — user must tap the tallone
        TiramisuTutorialStep(
            instructionResId = R.string.tut_deal,
            requiredMove     = TiramisuTutorialMove(sourcePile = -1, targetPile = -1)
        ),

        // Step 2: Confirmation after deal (Next button)
        TiramisuTutorialStep(
            instructionResId = R.string.tut_deal_confirm,
            requiredMove     = null
        ),

        // Step 3: Same-suit move — pile 1 (c5) → pile 2 (c3)
        TiramisuTutorialStep(
            instructionResId = R.string.tut_same_suit,
            requiredMove     = TiramisuTutorialMove(sourcePile = 1, targetPile = 2),
            highlightPiles   = listOf(1, 2)
        ),

        // Step 4: Confirmation (Next button)
        TiramisuTutorialStep(
            instructionResId = R.string.tut_same_suit_confirm,
            requiredMove     = null
        ),

        // Step 5: Foundation move — pile 1 → foundation (-1)
        // After step 3, pile 1 might be empty; pile 0 is empty (ace auto-moved).
        // The tutorial proceeds to a general instruction about moving to foundation.
        TiramisuTutorialStep(
            instructionResId = R.string.tut_foundation,
            requiredMove     = null   // info step — user learns concept, not forced move
        ),

        // Step 6: Finish
        TiramisuTutorialStep(
            instructionResId = R.string.tut_finish,
            requiredMove     = null
        )
    )
}
