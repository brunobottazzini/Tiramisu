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
     * Tutorial deck (TiramisuDeckSetup.tutorialDeck()) — NO pre-deal:
     *   pile 0-3 = EMPTY
     *   stock    = [b1, s6, c3, c10, c5, c7, d3, b2]  (8 cards)
     *
     * After step 1 (first deal — user taps tallone):
     *   pile 0 = EMPTY  (b1 auto-moved to bastoni foundation with animation)
     *   pile 1 = s6
     *   pile 2 = c3
     *   pile 3 = c10
     *   stock  = [c5, c7, d3, b2]
     *
     * After step 3 (second deal — user taps tallone again):
     *   pile 0 = c5
     *   pile 1 = [s6, c7]
     *   pile 2 = [c3, d3]
     *   pile 3 = [c10, b2]  ← b2 reserved for the foundation step
     *   stock  = EMPTY → canRedeal() becomes true (blocked until redeal step)
     *
     * Step 5 same-suit  : pile 0 (c5) → pile 1 top (c7), valid under strict (5 < 7).
     *   → pile 0 EMPTY, pile 1 = [s6, c7, c5]. Creates a same-suit run [c7, c5].
     *
     * Step 7 foundation : pile 3 top (b2) → bastoni foundation (2 follows the 1).
     *   → pile 3 = [c10]. Exposes c10 as a landing spot for the upcoming run.
     *
     * Step 9 run move   : pile 1 → pile 3 top (c10). topMovableRun = [c7, c5] (5 < 10 strict),
     *   the entire run slides as one move.
     *   → pile 1 = [s6], pile 3 = [c10, c7, c5].
     *
     * Step 11 empty-pile (PoC D): pile 3 → pile 0 (empty). At Normale the empty pile
     *   only accepts the top card, so only c5 lands.
     *   → pile 0 = [c5], pile 3 = [c10, c7]. Demonstrates the empty-pile single-card rule.
     */
    fun steps(resources: Resources): List<TiramisuTutorialStep> = listOf(

        // Step 0: Introduction — user taps "Avanti"
        TiramisuTutorialStep(
            instructionResId = R.string.tut_intro,
            requiredMove     = null
        ),

        // Step 1: First deal — b1 auto-moves to foundation with animation
        TiramisuTutorialStep(
            instructionResId = R.string.tut_deal,
            requiredMove     = TiramisuTutorialMove(sourcePile = -1, targetPile = -1)
        ),

        // Step 2: Confirmation after first deal
        TiramisuTutorialStep(
            instructionResId = R.string.tut_deal_confirm,
            requiredMove     = null
        ),

        // Step 3: Second deal — c5, c7, d3, b2 land on piles 0-3
        TiramisuTutorialStep(
            instructionResId = R.string.tut_deal2,
            requiredMove     = TiramisuTutorialMove(sourcePile = -1, targetPile = -1)
        ),

        // Step 4: Confirmation after second deal
        TiramisuTutorialStep(
            instructionResId = R.string.tut_deal2_confirm,
            requiredMove     = null
        ),

        // Step 5: Same-suit move — pile 0 (c5) → pile 1 top (c7)
        TiramisuTutorialStep(
            instructionResId = R.string.tut_same_suit,
            requiredMove     = TiramisuTutorialMove(sourcePile = 0, targetPile = 1),
            highlightSource  = 0,
            highlightTarget  = 1
        ),

        // Step 6: Confirmation — note that a same-suit descending run was just created
        TiramisuTutorialStep(
            instructionResId = R.string.tut_same_suit_confirm,
            requiredMove     = null
        ),

        // Step 7: Foundation move — b2 → bastoni foundation, exposes c10 on pile 3
        TiramisuTutorialStep(
            instructionResId = R.string.tut_foundation,
            requiredMove     = TiramisuTutorialMove(sourcePile = 3, targetPile = -1),
            highlightSource  = 3
        ),

        // Step 8: Confirmation after foundation move
        TiramisuTutorialStep(
            instructionResId = R.string.tut_foundation_confirm,
            requiredMove     = null
        ),

        // Step 9: Run move — pile 1 (top c5, run [c7, c5]) → pile 3 (top c10).
        // The whole run slides as one unit because the base (c7) fits c10 under strict.
        TiramisuTutorialStep(
            instructionResId = R.string.tut_run_move,
            requiredMove     = TiramisuTutorialMove(sourcePile = 1, targetPile = 3),
            highlightSource  = 1,
            highlightTarget  = 3
        ),

        // Step 10: Confirmation after the run move
        TiramisuTutorialStep(
            instructionResId = R.string.tut_run_move_confirm,
            requiredMove     = null
        ),

        // Step 11: Empty-pile (PoC D) — pile 3 → pile 0 (empty).
        // At Normale, only the top card of pile 3 (c5) lands; the rest stays put.
        TiramisuTutorialStep(
            instructionResId = R.string.tut_empty_pile_single,
            requiredMove     = TiramisuTutorialMove(sourcePile = 3, targetPile = 0),
            highlightSource  = 3,
            highlightTarget  = 0
        ),

        // Step 12: Confirmation — only the top card moved
        TiramisuTutorialStep(
            instructionResId = R.string.tut_empty_pile_single_confirm,
            requiredMove     = null
        ),

        // Step 13: Redeal — stock empty since step 3
        TiramisuTutorialStep(
            instructionResId = R.string.tut_redeal,
            requiredMove     = TiramisuTutorialMove(sourcePile = -1, targetPile = -2)
        ),

        // Step 14: Confirmation after redeal
        TiramisuTutorialStep(
            instructionResId = R.string.tut_redeal_confirm,
            requiredMove     = null
        ),

        // Step 15: Easy-mode note — covers both strict-rule and empty-pile differences
        TiramisuTutorialStep(
            instructionResId = R.string.tut_easy_mode,
            requiredMove     = null
        ),

        // Step 16: Finish — user taps "Avanti" → endTutorial()
        TiramisuTutorialStep(
            instructionResId = R.string.tut_finish,
            requiredMove     = null
        )
    )
}
