package com.bottazzini.tiramisu.utils

import android.content.res.Resources
import com.bottazzini.tiramisu.R

/**
 * A single scripted step in the tutorial.
 * [instructionResId] — string resource for the instruction text.
 * [requiredMove]     — if non-null, the "Next" button is hidden; the user must perform the move.
 * [highlightSource]  — pile index (0-3) rendered in ORANGE: "drag this card".
 * [highlightTarget]  — pile index (0-3) rendered in GREEN:  "drop here" (works on empty piles too).
 *
 * During info steps (requiredMove == null) ALL pile / foundation moves are blocked,
 * so the game state cannot be accidentally corrupted before the user taps "Avanti".
 */
data class TiramisuTutorialStep(
    val instructionResId: Int,
    val requiredMove:     TiramisuTutorialMove? = null,
    val highlightSource:  Int?                  = null,
    val highlightTarget:  Int?                  = null
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
     *   pile 1 = s6     (spade — neutral, different suit from every other pile top)
     *   pile 2 = c3     (coppe)
     *   pile 3 = d8     (denari)
     *   stock  = [c7, c5, d3, s4]  (4 cards — one deal empties it)
     *
     * After step 1 (deal from stock):
     *   pile 0 = c7  (coppe)         ← only 1 card (was empty before deal)
     *   pile 1 = c5  (coppe) on s6
     *   pile 2 = d3  (denari) on c3
     *   pile 3 = s4  (spade) on d8
     *   stock  = EMPTY → canRedeal() becomes true (button appears but blocked until step 7)
     *
     * Step 3 same-suit: pile 0 (c7, coppe) → pile 1 (c5, coppe)
     *   → pile 0 becomes EMPTY (had only 1 card)
     *   → pile 1 = [s6, c5, c7] (c7 on top)
     *
     * Step 5 empty-pile: pile 3 (s4, spade) → pile 0 (empty)
     *   → demonstrates that any card / any suit can go to an empty pile
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

        // Step 3: Same-suit move — pile 0 (c7, coppe) → pile 1 (c5, coppe)
        // pile 0 had only 1 card → becomes EMPTY after this move
        TiramisuTutorialStep(
            instructionResId = R.string.tut_same_suit,
            requiredMove     = TiramisuTutorialMove(sourcePile = 0, targetPile = 1),
            highlightSource  = 0,   // orange: drag from here
            highlightTarget  = 1    // green:  drop here
        ),

        // Step 4: Confirmation — user taps "Avanti"
        TiramisuTutorialStep(
            instructionResId = R.string.tut_same_suit_confirm,
            requiredMove     = null
        ),

        // Step 5: Empty-pile move — pile 3 (s4, spade) → pile 0 (empty)
        // Teaches: an empty pile accepts any card of any suit
        TiramisuTutorialStep(
            instructionResId = R.string.tut_empty_pile,
            requiredMove     = TiramisuTutorialMove(sourcePile = 3, targetPile = 0),
            highlightSource  = 3,   // orange: drag from here
            highlightTarget  = 0    // green:  drop on empty pile
        ),

        // Step 6: Confirmation — user taps "Avanti"
        TiramisuTutorialStep(
            instructionResId = R.string.tut_empty_pile_confirm,
            requiredMove     = null
        ),

        // Step 7: Redeal — stock empty since step 1; button visible but blocked until now
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
