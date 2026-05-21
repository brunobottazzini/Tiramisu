# Tutorial Fix & Completamento — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correggere 4 bug nel tutorial di Tiramisù e aggiungere passi interattivi per fondazione e ridistribuzione.

**Architecture:** Il tutorial usa un mazzo da 8 carte (invece di 40) che si svuota dopo un solo deal, un motore semplificato senza dead code, e 10 passi scriptati con mosse obbligate per deal, same-suit, fondazione e ridistribuzione.

**Tech Stack:** Kotlin, Android Views, JUnit 4 per unit test

---

## File Overview

| File | Modifica |
|------|----------|
| `app/src/test/java/com/bottazzini/tiramisu/TiramisuDeckSetupTest.kt` | Fix test `tutorialDeck size` (ora 8 carte) + aggiunta nuovi assertion |
| `app/src/test/java/com/bottazzini/tiramisu/TiramisuTutorialEngineTest.kt` | **Nuovo** — test per tutti i metodi del motore |
| `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuDeckSetup.kt` | `tutorialDeck()` — 8 carte invece di 40 |
| `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialEngine.kt` | Rimozione dead code (`moveExecuted`, `onMoveExecuted`), aggiunta `isRedealStep()`, `isCorrectPileMove()`, `isCorrectFoundationMove()` |
| `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialSteps.kt` | Nuova sequenza 10 passi; convention `sourcePile=-1,targetPile=-2` per redeal |
| `app/src/main/res/values/strings.xml` | Aggiornamento stringhe `tut_*` |
| `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt` | 4 fix: advance dopo stock deal, blocco/advance per redeal, validazione drag, highlight pile |

---

## Task 1: Fix test esistente per `tutorialDeck()` + aggiungi assertion

**Files:**
- Modify: `app/src/test/java/com/bottazzini/tiramisu/TiramisuDeckSetupTest.kt`

- [ ] **Step 1.1: Aggiorna il test che aspetta 40 carte**

Il test `tutorial deck has exactly 40 cards` fallirà dopo il Task 2 (cambieremo il mazzo a 8 carte). Aggiornalo ora, prima di cambiare il sorgente:

Sostituire in `TiramisuDeckSetupTest.kt`:
```kotlin
@Test
fun `tutorial deck has exactly 40 cards`() {
    assertEquals(40, TiramisuDeckSetup.tutorialDeck().size)
}

@Test
fun `tutorial deck first card is ace of bastoni`() {
    assertEquals("b1", TiramisuDeckSetup.tutorialDeck()[0])
}

@Test
fun `tutorial deck cards at index 4 and 5 are same suit`() {
    // Cards at positions 4 and 5 are dealt to pile 0 and pile 1 on second deal
    // They must share a suit for the same-suit move tutorial step
    val deck = TiramisuDeckSetup.tutorialDeck()
    val card4 = deck[4]
    val card5 = deck[5]
    assertEquals("positions 4 and 5 must share suit for tutorial",
                 card4[0], card5[0])
}
```

Con:
```kotlin
@Test
fun `tutorial deck has exactly 8 cards`() {
    assertEquals(8, TiramisuDeckSetup.tutorialDeck().size)
}

@Test
fun `tutorial deck initial deal is b1 b2 c3 d8`() {
    val deck = TiramisuDeckSetup.tutorialDeck()
    assertEquals("b1", deck[0])
    assertEquals("b2", deck[1])
    assertEquals("c3", deck[2])
    assertEquals("d8", deck[3])
}

@Test
fun `tutorial deck stock cards index 4 and 5 share suit for same-suit step`() {
    // After dealing initial 4: pile 0 is empty (b1 auto-moves), pile 1=b2, pile 2=c3, pile 3=d8
    // Stock: c7, c5, d3, s4. After 1 deal: pile 0=c7, pile 1=c5, pile 2=d3, pile 3=s4
    // Same-suit step requires pile 1 (c5, coppe) → pile 0 (c7, coppe) — both index 4 and 5 are coppe
    val deck = TiramisuDeckSetup.tutorialDeck()
    val stockCard0 = deck[4] // lands on pile 0 → c7
    val stockCard1 = deck[5] // lands on pile 1 → c5
    assertEquals("pile 0 e pile 1 dopo il deal devono condividere il seme (coppe)",
                 stockCard0[0], stockCard1[0])
}

@Test
fun `tutorial deck second card enables foundation step`() {
    // b2 is at position 1 → goes to pile 1 on initial deal
    // After same-suit move (c5 off pile 1), b2 is exposed and can go to bastoni foundation (has b1)
    val deck = TiramisuDeckSetup.tutorialDeck()
    assertEquals("b2", deck[1])
}
```

- [ ] **Step 1.2: Verifica che i test FALLISCANO (mazzo attuale è da 40 carte)**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew testDebugUnitTest --tests "com.bottazzini.tiramisu.TiramisuDeckSetupTest" 2>&1 | grep -E "PASSED|FAILED|ERROR"
```

Atteso: `tutorial deck has exactly 8 cards` FAILED, gli altri nuovi test variano.

---

## Task 2: Aggiorna `tutorialDeck()` — nuovo mazzo da 8 carte

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuDeckSetup.kt`

- [ ] **Step 2.1: Sostituire l'intero contenuto di `tutorialDeck()`**

```kotlin
fun tutorialDeck(): List<String> = listOf(
    // First 4 → initial deal to piles 0-3
    // b1 auto-moves to bastoni foundation → pile 0 = empty
    // pile 1 = b2  (bastoni — exposed after same-suit step, goes to foundation)
    // pile 2 = c3  (coppe)
    // pile 3 = d8  (denari)
    "b1", "b2", "c3", "d8",
    // Stock: exactly 4 cards — one deal empties the stock, triggering the redeal step
    // After deal: pile 0 = c7 (coppe), pile 1 = c5 (coppe) on b2, pile 2 = d3, pile 3 = s4
    // Same-suit step: pile 1 (c5, coppe) → pile 0 (c7, coppe)
    "c7", "c5", "d3", "s4"
)
```

- [ ] **Step 2.2: Verifica che i test passino**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew testDebugUnitTest --tests "com.bottazzini.tiramisu.TiramisuDeckSetupTest" 2>&1 | grep -E "PASSED|FAILED|BUILD"
```

Atteso: `BUILD SUCCESSFUL`, tutti i test PASSED.

- [ ] **Step 2.3: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuDeckSetup.kt \
        app/src/test/java/com/bottazzini/tiramisu/TiramisuDeckSetupTest.kt
git commit -m "feat(tutorial): tutorial deck ridotto a 8 carte (4 deal + 4 stock)

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 3: Test per `TiramisuTutorialEngine` (nuovo API)

**Files:**
- Create: `app/src/test/java/com/bottazzini/tiramisu/TiramisuTutorialEngineTest.kt`

- [ ] **Step 3.1: Crea il file di test**

```kotlin
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
        assertTrue(eng.isPileTapAllowed(1, "c5"))
    }

    @Test
    fun `isPileTapAllowed false for wrong source pile`() {
        val eng = TiramisuTutorialEngine(listOf(pileStep(src = 1, dst = 0)))
        assertFalse(eng.isPileTapAllowed(2, "c3"))
    }

    @Test
    fun `isPileTapAllowed true for any pile on info step`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep()))
        assertTrue(eng.isPileTapAllowed(0, "c7"))
        assertTrue(eng.isPileTapAllowed(3, "d8"))
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
    fun `isCorrectPileMove true on info step (no restriction)`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep()))
        assertTrue(eng.isCorrectPileMove(srcPile = 0, dstPile = 3))
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
    fun `isCorrectFoundationMove true on info step (no restriction)`() {
        val eng = TiramisuTutorialEngine(listOf(infoStep()))
        assertTrue(eng.isCorrectFoundationMove(srcPile = 0))
    }
}
```

- [ ] **Step 3.2: Verifica che i test FALLISCANO (metodi `isRedealStep`, `isCorrectPileMove`, `isCorrectFoundationMove` non esistono ancora)**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew testDebugUnitTest --tests "com.bottazzini.tiramisu.TiramisuTutorialEngineTest" 2>&1 | grep -E "error:|FAILED|BUILD"
```

Atteso: errore di compilazione — `isRedealStep` non trovato.

---

## Task 4: Riscrivere `TiramisuTutorialEngine`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialEngine.kt`

- [ ] **Step 4.1: Sostituire l'intero file**

```kotlin
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
```

- [ ] **Step 4.2: Verifica che i test passino**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew testDebugUnitTest --tests "com.bottazzini.tiramisu.TiramisuTutorialEngineTest" 2>&1 | grep -E "PASSED|FAILED|BUILD"
```

Atteso: `BUILD SUCCESSFUL`, tutti i test PASSED.

- [ ] **Step 4.3: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialEngine.kt \
        app/src/test/java/com/bottazzini/tiramisu/TiramisuTutorialEngineTest.kt
git commit -m "feat(tutorial): semplifica TiramisuTutorialEngine, aggiunge isRedealStep/isCorrectPileMove/isCorrectFoundationMove

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 5: Riscrivere `TiramisuTutorialSteps` — 10 passi

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialSteps.kt`

- [ ] **Step 5.1: Sostituire l'intero file**

```kotlin
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
```

- [ ] **Step 5.2: Verifica compilazione**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew compileDebugKotlin 2>&1 | grep -E "error:|warning:|BUILD"
```

Atteso: `BUILD SUCCESSFUL` (nessun errore Kotlin — le stringhe non ancora aggiunte potrebbero dare errore R; se sì, il fix delle stringhe va prima).

- [ ] **Step 5.3: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialSteps.kt
git commit -m "feat(tutorial): nuova sequenza 10 passi con fondazione e ridistribuzione

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 6: Aggiorna stringhe tutorial in `strings.xml`

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 6.1: Aggiorna le stringhe `tut_*`**

Trovare e sostituire il blocco delle stringhe tutorial (righe 34-43 del file attuale):

```xml
    <string name="tut_intro">Benvenuto in Tiramisù! Obiettivo: porta tutte le carte alle 4 basi, per seme dall\'Asso al 10.</string>
    <string name="tut_deal">Tocca il Tallone in basso per distribuire le carte.</string>
    <string name="tut_deal_confirm">✓ Distribuito! L\'Asso è andato automaticamente alla base bastoni.</string>
    <string name="tut_same_suit">Carte dello stesso seme si spostano liberamente tra mazzetti. Sposta la carta evidenziata sul mazzetto vicino.</string>
    <string name="tut_same_suit_confirm">✓ Ottimo! Stesso seme, qualsiasi valore.</string>
    <string name="tut_foundation">Quella carta può andare alla base! Toccala per selezionarla, poi tocca la base in alto.</string>
    <string name="tut_foundation_confirm">✓ Perfetto! Porta tutte le carte alle basi per vincere.</string>
    <string name="tut_redeal">Lo stock è vuoto. Tocca il pulsante Ridistribuisci per raccogliere i mazzetti e continuare!</string>
    <string name="tut_redeal_confirm">✓ Puoi ridistribuire un numero limitato di volte, in base alla difficoltà.</string>
    <string name="tut_finish">Bravo! Ora conosci le regole di Tiramisù. Buon gioco! 🎉</string>
```

- [ ] **Step 6.2: Verifica compilazione**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Atteso: `BUILD SUCCESSFUL`.

- [ ] **Step 6.3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat(tutorial): aggiorna stringhe tutorial per i 10 nuovi passi

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 7: Fix `GameActivity` — `onStockTapped()` avanza il tutorial

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`

- [ ] **Step 7.1: Aggiungere `advanceTutorial()` in `onStockTapped()`**

Trovare (righe 205-218):
```kotlin
    private fun onStockTapped() {
        if (isAnimating) return
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return
            if (!eng.isStockDealStep()) return
        }
        if (vm.dealFromStock()) {
            playSound(R.raw.flipcard)
            renderAll()
            maybeAnimateAutoAces()
            checkWin()
            checkLost()
        }
    }
```

Sostituire con:
```kotlin
    private fun onStockTapped() {
        if (isAnimating) return
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return
            if (!eng.isStockDealStep()) return
        }
        if (vm.dealFromStock()) {
            playSound(R.raw.flipcard)
            renderAll()
            maybeAnimateAutoAces()
            checkWin()
            checkLost()
            if (isTutorialMode) advanceTutorial()
        }
    }
```

- [ ] **Step 7.2: Verifica compilazione**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Atteso: `BUILD SUCCESSFUL`.

---

## Task 8: Fix `GameActivity` — `onRedealTapped()` + callback in `animateRedeal()`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`

- [ ] **Step 8.1: Blocca il redeal quando non è lo step corretto e avanza il tutorial dopo**

Trovare (righe 220-224):
```kotlin
    private fun onRedealTapped() {
        if (isAnimating) return
        if (!vm.canRedeal()) return
        animateRedeal()
    }
```

Sostituire con:
```kotlin
    private fun onRedealTapped() {
        if (isAnimating) return
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return
            if (!eng.isRedealStep()) return
        }
        if (!vm.canRedeal()) return
        animateRedeal()
    }
```

- [ ] **Step 8.2: Aggiungere `advanceTutorial()` nel postDelayed di cleanup di `animateRedeal()`**

Trovare il blocco `postDelayed` alla fine di `animateRedeal()` (righe ~293-297):
```kotlin
        val totalDuration = lastStartDelay + REDEAL_CARD_DURATION_MS
        gameRoot.postDelayed({
            for (task in tasks) gameRootContainer.removeView(task.ghost)
            renderAll()
            isAnimating = false
        }, totalDuration)
```

Sostituire con:
```kotlin
        val totalDuration = lastStartDelay + REDEAL_CARD_DURATION_MS
        gameRoot.postDelayed({
            for (task in tasks) gameRootContainer.removeView(task.ghost)
            renderAll()
            isAnimating = false
            if (isTutorialMode) advanceTutorial()
        }, totalDuration)
```

- [ ] **Step 8.3: Verifica compilazione**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Atteso: `BUILD SUCCESSFUL`.

---

## Task 9: Fix `GameActivity` — validazione drag per pile e fondazione

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`

- [ ] **Step 9.1: Blocca drop su pile sbagliata in `handlePileDrop()`**

Trovare (righe 634-646):
```kotlin
    private fun handlePileDrop(srcPile: Int, dstPile: Int): Boolean {
        if (!vm.tryMoveBetweenPiles(srcPile, dstPile)) {
            showInvalidMoveToast()
            return false
        }
        playSound(R.raw.flipcard)
        renderAll()
        maybeAnimateAutoAces()
        checkWin()
        checkLost()
        if (isTutorialMode) advanceTutorial()
        return true
    }
```

Sostituire con:
```kotlin
    private fun handlePileDrop(srcPile: Int, dstPile: Int): Boolean {
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return false
            if (!eng.isCorrectPileMove(srcPile, dstPile)) {
                showInvalidMoveToast()
                return false
            }
        }
        if (!vm.tryMoveBetweenPiles(srcPile, dstPile)) {
            showInvalidMoveToast()
            return false
        }
        playSound(R.raw.flipcard)
        renderAll()
        maybeAnimateAutoAces()
        checkWin()
        checkLost()
        if (isTutorialMode) advanceTutorial()
        return true
    }
```

- [ ] **Step 9.2: Blocca drop su fondazione sbagliata in `handleFoundationDrop()`**

Trovare (righe 648-660):
```kotlin
    private fun handleFoundationDrop(srcPile: Int): Boolean {
        if (!vm.onFoundationTapped(srcPile)) {
            showInvalidMoveToast()
            return false
        }
        playSound(R.raw.flipcard)
        renderAll()
        maybeAnimateAutoAces()
        checkWin()
        checkLost()
        if (isTutorialMode) advanceTutorial()
        return true
    }
```

Sostituire con:
```kotlin
    private fun handleFoundationDrop(srcPile: Int): Boolean {
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return false
            if (!eng.isCorrectFoundationMove(srcPile)) {
                showInvalidMoveToast()
                return false
            }
        }
        if (!vm.onFoundationTapped(srcPile)) {
            showInvalidMoveToast()
            return false
        }
        playSound(R.raw.flipcard)
        renderAll()
        maybeAnimateAutoAces()
        checkWin()
        checkLost()
        if (isTutorialMode) advanceTutorial()
        return true
    }
```

- [ ] **Step 9.3: Verifica compilazione**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Atteso: `BUILD SUCCESSFUL`.

---

## Task 10: Fix `GameActivity` — evidenziazione pile tutorial

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`

- [ ] **Step 10.1: Aggiungere `isTutorialHighlight` nel rendering della pila**

Trovare in `renderPile()` (righe ~474-505), il blocco che costruisce l'ImageView dell'ultima carta:
```kotlin
        val isSelected  = vm.selectedPileIndex == pileIdx
        val isObbligato = vm.obbligatoTargets().contains(pileIdx)
        val isHinted    = hintedPileIdx == pileIdx
```

Sostituire con:
```kotlin
        val isSelected  = vm.selectedPileIndex == pileIdx
        val isObbligato = vm.obbligatoTargets().contains(pileIdx)
        val isHinted    = hintedPileIdx == pileIdx
        val isTutorialHighlight = isTutorialMode &&
            (tutorialEngine?.currentStep()?.highlightPiles?.contains(pileIdx) == true)
```

- [ ] **Step 10.2: Usare `isTutorialHighlight` nel `when` del colore**

Trovare (righe ~498-505):
```kotlin
                when {
                    isSelected  -> imageView.alpha = 0.7f
                    isObbligato -> imageView.setColorFilter(
                        0x88FF0000.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    isHinted    -> imageView.setColorFilter(
                        0x8800FF00.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    else        -> { imageView.alpha = 1f; imageView.clearColorFilter() }
                }
```

Sostituire con:
```kotlin
                when {
                    isSelected           -> imageView.alpha = 0.7f
                    isTutorialHighlight  -> imageView.setColorFilter(
                        0x880000FF.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    isObbligato          -> imageView.setColorFilter(
                        0x88FF0000.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    isHinted             -> imageView.setColorFilter(
                        0x8800FF00.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    else                 -> { imageView.alpha = 1f; imageView.clearColorFilter() }
                }
```

- [ ] **Step 10.3: Verifica compilazione completa + unit test**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew testDebugUnitTest 2>&1 | grep -E "PASSED|FAILED|BUILD"
```

Atteso: `BUILD SUCCESSFUL`, 0 test FAILED.

- [ ] **Step 10.4: Commit finale di GameActivity**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt
git commit -m "fix(tutorial): correggi avanzamento step stock/redeal, validazione drag, evidenziazione pile

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 11: Verifica integrazione manuale

> Questo task richiede un dispositivo Android o emulatore.

- [ ] **Step 11.1: Build debug e installa**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew installDebug 2>&1 | tail -5
```

- [ ] **Step 11.2: Eseguire la checklist manuale**

Aprire l'app → Menu → Tutorial (o lanciare direttamente la GameActivity con `EXTRA_TUTORIAL_MODE = true`).

| Azione | Atteso |
|--------|--------|
| Schermata iniziale | Testo intro, pulsante "Avanti" visibile, stock pieno (4 carte) |
| Tocco "Avanti" (step 0) | Passa a step 1: testo "Tocca il Tallone", nessun pulsante Avanti |
| Toccare una pila durante step 1 | Bloccato, nessuna selezione |
| Toccare il tallone | Carte distribuite, Asso va in fondazione, tutorial avanza a step 2 |
| Tocco "Avanti" (step 2) | Passa a step 3: pili 0 e 1 evidenziate in blu |
| Drag pile 1 → pile 2 (mossa sbagliata) | Toast errore, tutorial NON avanza |
| Drag pile 1 → pile 0 (mossa corretta) | Carta spostata, tutorial avanza a step 4 |
| Tocco "Avanti" (step 4) | Passa a step 5: pila 1 evidenziata in blu, b2 visibile |
| Tap pila 1 → tap fondazione | b2 va in fondazione, tutorial avanza a step 6 |
| Tocco "Avanti" (step 6) | Passa a step 7: testo "Tocca Ridistribuisci" |
| Tap "Ridistribuisci" prima step 7 | Bloccato (non succede nulla) |
| Tap "Ridistribuisci" a step 7 | Animazione redeal, tutorial avanza a step 8 |
| Tocco "Avanti" (step 8) | Passa a step 9: testo finale |
| Tocco "Avanti" (step 9) | Tutorial termina, toast "Tutorial completato! Buon gioco!" |

- [ ] **Step 11.3: Commit tag release (opzionale)**

```bash
git tag tutorial-fix-complete
```
