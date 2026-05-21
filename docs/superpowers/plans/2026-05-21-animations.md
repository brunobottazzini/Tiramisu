# Tiramisù — Game Animations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aggiungere due animazioni alla `GameActivity`: redeal sequenziale (cards → tallone, pile 3→0) e auto-ace globale (aces → fondazione quando `autoMoveAces` muove qualcosa).

**Architecture:** Ghost `ImageView` aggiunti dinamicamente a `gameRoot` (ConstraintLayout root, non clippato dalle ScrollView delle pile). Animazioni via `ViewPropertyAnimator` su `translationX/Y`. UI lock condiviso tramite `isAnimating: Boolean` per evitare azioni durante animazioni.

**Tech Stack:** Kotlin, Android ViewPropertyAnimator (API 12+, ben supportato da minSdk 24), JUnit per i nuovi unit test del ViewModel.

**Spec di riferimento:** `docs/superpowers/specs/2026-05-21-animations-design.md`.

---

## File Structure

**Modificati:**
- `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt` — nuovi tipi `AceMove`/`AceSource`, field `_lastAutoAceMoves`, `consumeAutoAceMoves()`, `autoMoveAces(defaultSource)`
- `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt` — costanti timing, `isAnimating` flag + guards, `animateRedeal()`, `animateAutoAces()`, helper `locationOnScreen`
- `app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt` — 1 unit test per la nuova API auto-ace

**Non toccati:** layout XML, drawable, manifest, gradle, theme, strings, altri activity.

---

### Task 1: ViewModel API per auto-ace moves

**Files:**
- Modify: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt`
- Modify: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt`

- [ ] **Step 1: Aggiungi `AceMove`/`AceSource` e relative API in TiramisuViewModel.kt**

Nel file `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt`:

(a) Sotto la riga `enum class TapResult { SELECTED, DESELECTED, MOVED, INVALID, NOTHING }` (alla fine del file, fuori dalla classe `TiramisuViewModel`), aggiungi:

```kotlin
enum class AceSource { STOCK, PILE_TOP }

data class AceMove(
    val fromPile: Int,
    val toFoundation: Int,
    val card: String,
    val source: AceSource
)
```

(b) Dentro la classe `TiramisuViewModel`, dopo il blocco `private var previousState: TiramisuGameState? = null`, aggiungi:

```kotlin
    /**
     * Auto-ace moves performed by the last action. Callers (GameActivity) read
     * this with [consumeAutoAceMoves] right after the action to play the
     * ace-to-foundation animation, which also clears the slot.
     */
    private var _lastAutoAceMoves: List<AceMove> = emptyList()

    fun consumeAutoAceMoves(): List<AceMove> {
        val moves = _lastAutoAceMoves
        _lastAutoAceMoves = emptyList()
        return moves
    }
```

(c) Sostituisci la funzione `private fun autoMoveAces() { ... }` con la versione parametrizzata. Trova:

```kotlin
    /** Auto-move any Aces (rank 1) from pile tops to foundations. Loops until stable. */
    private fun autoMoveAces() {
        val s = state ?: return
        var moved = true
        while (moved) {
            moved = false
            for (pileIdx in 0..3) {
                val card = s.topOfPile(pileIdx)
                if (card != "zero" && TiramisuMoveValidator.rank(card) == 1) {
                    for (fIdx in 0..3) {
                        if (TiramisuMoveValidator.canMoveToFoundation(card, s.foundations[fIdx])) {
                            s.piles[pileIdx].removeAt(s.piles[pileIdx].size - 1)
                            s.foundations[fIdx] = card
                            moved = true
                            break
                        }
                    }
                }
            }
        }
    }
```

E sostituiscilo con:

```kotlin
    /**
     * Auto-move any Aces (rank 1) from pile tops to foundations. Loops until stable.
     * Records every move in [_lastAutoAceMoves] for the animation layer to consume.
     * [defaultSource] tags each move with where the ace originally came from
     * visually: STOCK for dealFromStock (the ace just exited the stock), PILE_TOP
     * for moves where the ace was already sitting on a pile and got auto-moved.
     */
    private fun autoMoveAces(defaultSource: AceSource) {
        val s = state ?: return
        val moves = mutableListOf<AceMove>()
        var moved = true
        while (moved) {
            moved = false
            for (pileIdx in 0..3) {
                val card = s.topOfPile(pileIdx)
                if (card != "zero" && TiramisuMoveValidator.rank(card) == 1) {
                    for (fIdx in 0..3) {
                        if (TiramisuMoveValidator.canMoveToFoundation(card, s.foundations[fIdx])) {
                            s.piles[pileIdx].removeAt(s.piles[pileIdx].size - 1)
                            s.foundations[fIdx] = card
                            moves.add(AceMove(pileIdx, fIdx, card, defaultSource))
                            moved = true
                            break
                        }
                    }
                }
            }
        }
        _lastAutoAceMoves = moves
    }
```

(d) Aggiorna i 3 chiamanti di `autoMoveAces()` per passare il source giusto. Trova e sostituisci:

In `dealFromStock()`:
```kotlin
        autoMoveAces()
```
→
```kotlin
        autoMoveAces(AceSource.STOCK)
```

In `onFoundationTapped()`:
```kotlin
                autoMoveAces()
```
→
```kotlin
                autoMoveAces(AceSource.PILE_TOP)
```

In `movePileToPile()`:
```kotlin
        autoMoveAces()
```
→
```kotlin
        autoMoveAces(AceSource.PILE_TOP)
```

(e) Reset di `_lastAutoAceMoves` in tutti i punti di reset di state. Trova:

```kotlin
    fun newGame(difficulty: Difficulty) {
        state = TiramisuGameState.newGame(difficulty)
        selectedPileIndex = null
        previousState = null
    }

    fun newTutorialGame(difficulty: Difficulty = Difficulty.FACILE) {
        state = TiramisuGameState.tutorialGame(difficulty)
        selectedPileIndex = null
        previousState = null
    }

    fun restoreState(restored: TiramisuGameState) {
        state = restored
        selectedPileIndex = null
        previousState = null
    }
```

E sostituisci con:

```kotlin
    fun newGame(difficulty: Difficulty) {
        state = TiramisuGameState.newGame(difficulty)
        selectedPileIndex = null
        previousState = null
        _lastAutoAceMoves = emptyList()
    }

    fun newTutorialGame(difficulty: Difficulty = Difficulty.FACILE) {
        state = TiramisuGameState.tutorialGame(difficulty)
        selectedPileIndex = null
        previousState = null
        _lastAutoAceMoves = emptyList()
    }

    fun restoreState(restored: TiramisuGameState) {
        state = restored
        selectedPileIndex = null
        previousState = null
        _lastAutoAceMoves = emptyList()
    }
```

Trova `fun undo()`:
```kotlin
    fun undo(): Boolean {
        val prev = previousState ?: return false
        state = prev
        previousState = null
        selectedPileIndex = null
        return true
    }
```

E sostituisci con:
```kotlin
    fun undo(): Boolean {
        val prev = previousState ?: return false
        state = prev
        previousState = null
        selectedPileIndex = null
        _lastAutoAceMoves = emptyList()
        return true
    }
```

Trova `fun retrySameGame()`:
```kotlin
    fun retrySameGame() {
        val s = state ?: return
        state = TiramisuGameState.replay(s.difficulty, s.initialDeck)
        selectedPileIndex = null
        previousState = null
    }
```

E sostituisci con:
```kotlin
    fun retrySameGame() {
        val s = state ?: return
        state = TiramisuGameState.replay(s.difficulty, s.initialDeck)
        selectedPileIndex = null
        previousState = null
        _lastAutoAceMoves = emptyList()
    }
```

- [ ] **Step 2: Aggiungi unit test in TiramisuViewModelTest.kt**

Nel file `/Users/bottazzini/Documents/misc/Tiramisu/app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt`, aggiungi prima della chiusura della classe (l'ultima `}` del file):

```kotlin
    @Test fun `dealFromStock auto-moves ace and records it for animation`() {
        stateWith(
            piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            stock = listOf("b1", "c5", "d7", "s2")
        )
        vm.dealFromStock()
        val moves = vm.consumeAutoAceMoves()
        assertEquals(1, moves.size)
        val m = moves[0]
        assertEquals("b1", m.card)
        assertEquals(0, m.fromPile)
        assertEquals(AceSource.STOCK, m.source)
        // Foundation now holds the ace
        assertEquals("b1", vm.state!!.foundations[m.toFoundation])
        // Consuming clears the slot
        assertTrue(vm.consumeAutoAceMoves().isEmpty())
    }
```

Il test esiste nella stessa package `com.bottazzini.tiramisu` quindi `AceMove`/`AceSource`/`consumeAutoAceMoves` sono accessibili senza import aggiuntivi.

- [ ] **Step 3: Compile + run tests**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`. Il nuovo test deve passare insieme agli esistenti.

- [ ] **Step 4: Commit**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt \
        app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt
git commit -m "feat(animations): expose autoMoveAces output as AceMove list

New AceMove data class + AceSource enum let GameActivity consume the
list of auto-ace moves after each user action and play an animation
from source (stock or pile top) to the destination foundation.
autoMoveAces() now takes a defaultSource parameter so dealFromStock
tags moves as STOCK and pile/foundation moves tag them as PILE_TOP."
```

---

### Task 2: GameActivity `isAnimating` flag + UI lock guards

**Files:**
- Modify: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`

- [ ] **Step 1: Aggiungi le costanti di timing nel companion object**

Trova in `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`:

```kotlin
    companion object {
        const val EXTRA_TUTORIAL_MODE = "tutorial_mode"
        /** Visible height of each non-top card in a pile (dp). Top card shows fully. */
        private const val CARD_PEEK_DP = 24
        /** Card image native dimensions (e.g. piacentine_b1.png is 200×364). */
        private const val CARD_ASPECT_W = 200f
        private const val CARD_ASPECT_H = 364f
        /** ClipData label used to identify our drag events. */
        private const val DRAG_LABEL = "tiramisu_pile_drag"
    }
```

E sostituisci con:

```kotlin
    companion object {
        const val EXTRA_TUTORIAL_MODE = "tutorial_mode"
        /** Visible height of each non-top card in a pile (dp). Top card shows fully. */
        private const val CARD_PEEK_DP = 24
        /** Card image native dimensions (e.g. piacentine_b1.png is 200×364). */
        private const val CARD_ASPECT_W = 200f
        private const val CARD_ASPECT_H = 364f
        /** ClipData label used to identify our drag events. */
        private const val DRAG_LABEL = "tiramisu_pile_drag"
        // Animation timing (ms)
        private const val REDEAL_CARD_DURATION_MS = 200L
        private const val REDEAL_CARD_STAGGER_MS  = 60L
        private const val REDEAL_PILE_GAP_MS      = 150L
        private const val ACE_DURATION_MS         = 250L
        private const val ACE_STAGGER_MS          = 80L
    }
```

- [ ] **Step 2: Aggiungi il flag `isAnimating`**

Trova nella sezione `// ---- State ----`:

```kotlin
    private var isTutorialMode = false
    private var tutorialEngine: TiramisuTutorialEngine? = null
    private var hintsUsedThisGame = 0
    private var cardType = "piacentine"
    private var cardBackKey = "bg2"
    private var hintedPileIdx: Int? = null
    private var mediaPlayer: MediaPlayer? = null
```

E sostituisci con:

```kotlin
    private var isTutorialMode = false
    private var tutorialEngine: TiramisuTutorialEngine? = null
    private var hintsUsedThisGame = 0
    private var cardType = "piacentine"
    private var cardBackKey = "bg2"
    private var hintedPileIdx: Int? = null
    private var mediaPlayer: MediaPlayer? = null
    /** True while a ghost animation (redeal or auto-ace) is in flight. Blocks all interactions except the pause/menu button. */
    private var isAnimating = false
```

- [ ] **Step 3: Aggiungi le guard `isAnimating` ai 7 entry point**

Per OGNUNO dei seguenti metodi, aggiungi `if (isAnimating) return` come PRIMA istruzione della funzione (dopo l'apertura `{`).

**`onStockTapped`** — trova:
```kotlin
    private fun onStockTapped() {
        if (isTutorialMode) {
```
Sostituisci con:
```kotlin
    private fun onStockTapped() {
        if (isAnimating) return
        if (isTutorialMode) {
```

**`onRedealTapped`** — trova:
```kotlin
    private fun onRedealTapped() {
        if (vm.redeal()) {
```
Sostituisci con:
```kotlin
    private fun onRedealTapped() {
        if (isAnimating) return
        if (vm.redeal()) {
```

**`onPileCardTapped`** — trova:
```kotlin
    private fun onPileCardTapped(pileIdx: Int) {
        if (isTutorialMode) {
            val eng  = tutorialEngine ?: return
            val card = vm.state?.topOfPile(pileIdx) ?: return
            if (!eng.isPileTapAllowed(pileIdx, card)) return
        }

        val result = vm.onPileTapped(pileIdx)
```
Sostituisci con:
```kotlin
    private fun onPileCardTapped(pileIdx: Int) {
        if (isAnimating) return
        if (isTutorialMode) {
            val eng  = tutorialEngine ?: return
            val card = vm.state?.topOfPile(pileIdx) ?: return
            if (!eng.isPileTapAllowed(pileIdx, card)) return
        }

        val result = vm.onPileTapped(pileIdx)
```

**`onFoundationViewTapped`** — trova:
```kotlin
    private fun onFoundationViewTapped(foundationIdx: Int) {
        val sel = vm.selectedPileIndex ?: return
```
Sostituisci con:
```kotlin
    private fun onFoundationViewTapped(foundationIdx: Int) {
        if (isAnimating) return
        val sel = vm.selectedPileIndex ?: return
```

**`onHintTapped`** — trova:
```kotlin
    private fun onHintTapped() {
        val s    = vm.state ?: return
```
Sostituisci con:
```kotlin
    private fun onHintTapped() {
        if (isAnimating) return
        val s    = vm.state ?: return
```

**`onUndoTapped`** — trova:
```kotlin
    private fun onUndoTapped() {
        if (isTutorialMode) return
        if (vm.undo()) {
```
Sostituisci con:
```kotlin
    private fun onUndoTapped() {
        if (isAnimating) return
        if (isTutorialMode) return
        if (vm.undo()) {
```

**`startCardDrag`** — trova:
```kotlin
    private fun startCardDrag(v: View, pileIdx: Int): Boolean {
        if (isTutorialMode) {
```
Sostituisci con:
```kotlin
    private fun startCardDrag(v: View, pileIdx: Int): Boolean {
        if (isAnimating) return false
        if (isTutorialMode) {
```

- [ ] **Step 4: Build verification**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt
git commit -m "feat(animations): add isAnimating flag + timing constants

isAnimating gates all 7 player-action entry points (onStockTapped,
onRedealTapped, onPileCardTapped, onFoundationViewTapped, onHintTapped,
onUndoTapped, startCardDrag). btnMenu (pause) stays unblocked.
Timing constants live in the companion object to keep both the redeal
and auto-ace animations tunable in one place."
```

---

### Task 3: Redeal animation

**Files:**
- Modify: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`

- [ ] **Step 1: Aggiungi import necessario**

Trova in cima al file `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`:

```kotlin
import android.annotation.SuppressLint
import android.content.ClipData
```

E sostituisci con:

```kotlin
import android.annotation.SuppressLint
import android.content.ClipData
import androidx.constraintlayout.widget.ConstraintLayout
```

- [ ] **Step 2: Riscrivi `onRedealTapped` per orchestrare l'animazione**

Trova:

```kotlin
    private fun onRedealTapped() {
        if (isAnimating) return
        if (vm.redeal()) {
            playSound(R.raw.flipcard)
            renderAll()
        }
    }
```

E sostituisci con:

```kotlin
    private fun onRedealTapped() {
        if (isAnimating) return
        if (!vm.canRedeal()) return
        animateRedeal()
    }
```

- [ ] **Step 3: Aggiungi helper `locationOnScreen` + `animateRedeal`**

Subito DOPO `private fun onRedealTapped() { ... }`, aggiungi:

```kotlin
    /** Convenience: 2-element [x, y] screen coords. */
    private fun locationOnScreen(view: View): IntArray =
        IntArray(2).also { view.getLocationOnScreen(it) }

    /**
     * Animates each pile's cards sliding to the stock area, pile 3→0 sequentially.
     * Mutates state via [vm.redeal] up-front; the visual catch-up happens after
     * the ghost animations finish.
     */
    private fun animateRedeal() {
        val gameRootContainer = gameRoot as ConstraintLayout
        val gameRootPos = locationOnScreen(gameRootContainer)
        val stockPos = locationOnScreen(stockArea)

        data class GhostTask(val pileIdx: Int, val ghost: ImageView)
        val tasks = mutableListOf<GhostTask>()

        for (pileIdx in 3 downTo 0) {
            val container = pileContainers[pileIdx] ?: continue
            for (childIdx in (container.childCount - 1) downTo 0) {
                val original = container.getChildAt(childIdx) as? ImageView ?: continue
                val origPos = locationOnScreen(original)
                val ghost = ImageView(this).apply {
                    setImageDrawable(original.drawable)
                    scaleType = original.scaleType
                    layoutParams = ConstraintLayout.LayoutParams(original.width, original.height)
                    translationX = (origPos[0] - gameRootPos[0]).toFloat()
                    translationY = (origPos[1] - gameRootPos[1]).toFloat()
                }
                gameRootContainer.addView(ghost)
                original.alpha = 0f
                tasks.add(GhostTask(pileIdx, ghost))
            }
        }

        if (tasks.isEmpty()) {
            vm.redeal()
            renderAll()
            return
        }

        isAnimating = true
        playSound(R.raw.flipcard)
        vm.redeal()

        val targetX = (stockPos[0] - gameRootPos[0]).toFloat()
        val targetY = (stockPos[1] - gameRootPos[1]).toFloat()

        var delay = 0L
        var prevPile = -1
        var lastStartDelay = 0L
        for (task in tasks) {
            if (prevPile != -1 && task.pileIdx != prevPile) {
                delay += REDEAL_PILE_GAP_MS
            }
            task.ghost.animate()
                .translationX(targetX)
                .translationY(targetY)
                .setDuration(REDEAL_CARD_DURATION_MS)
                .setStartDelay(delay)
                .start()
            lastStartDelay = delay
            prevPile = task.pileIdx
            delay += REDEAL_CARD_STAGGER_MS
        }

        val totalDuration = lastStartDelay + REDEAL_CARD_DURATION_MS
        gameRoot.postDelayed({
            for (task in tasks) gameRootContainer.removeView(task.ghost)
            renderAll()
            isAnimating = false
        }, totalDuration)
    }
```

- [ ] **Step 4: Build verification**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt
git commit -m "feat(animations): redeal cards slide pile 3→0 to the stock

onRedealTapped now spawns a ghost ImageView per pile card (pile 3 top
first, then bottom, then pile 2, etc.), hides the originals, mutates
state via vm.redeal(), and animates each ghost's translationX/Y to
the stockArea position with 60ms intra-pile stagger and 150ms gap
between piles. After the last animation, ghosts are removed and
renderAll() shows the new state."
```

---

### Task 4: Auto-ace animation

**Files:**
- Modify: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`

- [ ] **Step 1: Aggiungi `maybeAnimateAutoAces` + `animateAutoAces` dopo `animateRedeal`**

Subito DOPO la chiusura `}` di `animateRedeal`, aggiungi:

```kotlin
    /**
     * If the ViewModel's last action queued any auto-ace moves, animate them.
     * Idempotent: consuming the list clears it so the next render won't replay.
     */
    private fun maybeAnimateAutoAces() {
        val moves = vm.consumeAutoAceMoves()
        if (moves.isEmpty()) return
        animateAutoAces(moves)
    }

    private fun animateAutoAces(moves: List<AceMove>) {
        val gameRootContainer = gameRoot as ConstraintLayout
        val gameRootPos = locationOnScreen(gameRootContainer)
        val ghosts = mutableListOf<ImageView>()
        val hiddenFoundations = mutableListOf<ImageView>()

        for ((idx, move) in moves.withIndex()) {
            val destView = foundationViews[move.toFoundation] ?: continue
            val resId = resources.getIdentifier("${cardType}_${move.card}", "drawable", packageName)
            if (resId == 0) continue

            val sourceLoc = when (move.source) {
                AceSource.STOCK -> locationOnScreen(stockArea)
                AceSource.PILE_TOP -> {
                    val container = pileContainers[move.fromPile]
                    val topChild = container?.let { it.getChildAt(it.childCount - 1) }
                    if (topChild != null) locationOnScreen(topChild) else locationOnScreen(stockArea)
                }
            }
            val destLoc = locationOnScreen(destView)

            destView.alpha = 0f
            hiddenFoundations.add(destView)

            val ghost = ImageView(this).apply {
                setImageResource(resId)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = ConstraintLayout.LayoutParams(destView.width, destView.height)
                translationX = (sourceLoc[0] - gameRootPos[0]).toFloat()
                translationY = (sourceLoc[1] - gameRootPos[1]).toFloat()
            }
            gameRootContainer.addView(ghost)
            ghosts.add(ghost)

            ghost.animate()
                .translationX((destLoc[0] - gameRootPos[0]).toFloat())
                .translationY((destLoc[1] - gameRootPos[1]).toFloat())
                .setDuration(ACE_DURATION_MS)
                .setStartDelay(idx * ACE_STAGGER_MS)
                .start()
        }

        if (ghosts.isEmpty()) return

        isAnimating = true
        val totalDuration = (moves.size - 1) * ACE_STAGGER_MS + ACE_DURATION_MS
        gameRoot.postDelayed({
            for (ghost in ghosts) gameRootContainer.removeView(ghost)
            for (view in hiddenFoundations) view.alpha = 1f
            isAnimating = false
        }, totalDuration)
    }
```

- [ ] **Step 2: Hook nei 5 punti di azione (chiama `maybeAnimateAutoAces` dopo `renderAll`, prima di `checkWin/checkLost`)**

**onStockTapped** — trova:
```kotlin
        if (vm.dealFromStock()) {
            playSound(R.raw.flipcard)
            renderAll()
            checkWin()
            checkLost()
        }
```
Sostituisci con:
```kotlin
        if (vm.dealFromStock()) {
            playSound(R.raw.flipcard)
            renderAll()
            maybeAnimateAutoAces()
            checkWin()
            checkLost()
        }
```

**onPileCardTapped** — trova:
```kotlin
            TapResult.MOVED   -> {
                playSound(R.raw.flipcard)
                renderAll()
                checkWin()
                checkLost()
                if (isTutorialMode) advanceTutorial()
            }
```
Sostituisci con:
```kotlin
            TapResult.MOVED   -> {
                playSound(R.raw.flipcard)
                renderAll()
                maybeAnimateAutoAces()
                checkWin()
                checkLost()
                if (isTutorialMode) advanceTutorial()
            }
```

**onFoundationViewTapped** — trova:
```kotlin
        if (vm.onFoundationTapped(sel)) {
            playSound(R.raw.flipcard)
            renderAll()
            checkWin()
            checkLost()
            if (isTutorialMode) advanceTutorial()
        }
```
Sostituisci con:
```kotlin
        if (vm.onFoundationTapped(sel)) {
            playSound(R.raw.flipcard)
            renderAll()
            maybeAnimateAutoAces()
            checkWin()
            checkLost()
            if (isTutorialMode) advanceTutorial()
        }
```

**handlePileDrop e handleFoundationDrop** — entrambi hanno lo STESSO blocco. Trova e sostituisci ENTRAMBE le occorrenze:

Trova:
```kotlin
        playSound(R.raw.flipcard)
        renderAll()
        checkWin()
        checkLost()
        if (isTutorialMode) advanceTutorial()
        return true
    }
```

Sostituisci con (usa `replace_all=true` per applicare entrambe in una passata):
```kotlin
        playSound(R.raw.flipcard)
        renderAll()
        maybeAnimateAutoAces()
        checkWin()
        checkLost()
        if (isTutorialMode) advanceTutorial()
        return true
    }
```

- [ ] **Step 3: Build verification + run unit tests**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`. Tutti i test passano (i nuovi e gli esistenti).

- [ ] **Step 4: Commit**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt
git commit -m "feat(animations): aces fly to foundation on auto-move

After every player action that may trigger autoMoveAces (deal, pile/
foundation move via tap or drag), maybeAnimateAutoAces() consumes
vm.lastAutoAceMoves and spawns a ghost ImageView per move that flies
from the stock (for deals) or pile top (for plays) to the destination
foundation, with 80ms stagger between simultaneous aces. The
destination foundation card is alpha-hidden until the ghost arrives."
```

---

### Task 5: Verifica finale + gate visivo

**Files:** nessuna modifica.

- [ ] **Step 1: Stato git pulito + summary commits**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git status && echo "---" && git log --oneline -6
```

Expected: `nothing to commit, working tree clean`. 4 nuovi commit dal lavoro animations.

- [ ] **Step 2: Build completa**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`. Tutti i test verdi.

- [ ] **Step 3: Comunicazione gate visivo all'utente**

Comunica:

> Animazioni implementate. 4 commit lineari su master, build e unit test verdi.
>
> Installa l'apk con `./gradlew :app:installDebug` (richiede device/emulatore connesso) e verifica:
>
> 1. **Redeal**: tap "Ridistribuisci" → carte volano in sequenza dai mazzetti 3→2→1→0 al tallone (~1.5–2s)
> 2. **Auto-ace da deal**: tap stock dopo redeal → se appaiono assi, volano dal tallone alla fondazione corrispondente
> 3. **Auto-ace da gioco**: muovi una carta che scopre un asso → l'asso vola dalla sua pila alla fondazione
> 4. **UI lock**: durante un'animazione i bottoni/drag non rispondono. Tap pausa (⏸ in alto a destra) funziona sempre.
> 5. **Edge case**: undo dopo deal-con-ace → state ripristinato, niente animazione spuria

---

## Self-Review

**Spec coverage** (rispetto a `2026-05-21-animations-design.md`):
- §2 timing tokens → Task 2 Step 1 (companion object) ✓
- §3 Redeal animation → Task 3 (animateRedeal + sequencing pile 3→0) ✓
- §3.5 edge cases redeal (pile vuote, no carte) → Task 3 (`if (tasks.isEmpty()) { vm.redeal(); renderAll(); return }`) ✓
- §4.1 ViewModel API (AceMove, AceSource, _lastAutoAceMoves, consumeAutoAceMoves) → Task 1 Step 1 ✓
- §4.2 hook points (5 punti) → Task 4 Step 2 (onStockTapped, onPileCardTapped MOVED, onFoundationViewTapped, handlePileDrop, handleFoundationDrop) ✓
- §4.2 ordering pre checkWin/checkLost → Task 4 Step 2 (maybeAnimateAutoAces chiamato PRIMA di checkWin/checkLost) ✓
- §4.3 animation flow (source-resolution, hide foundation card, ghost, end callback) → Task 4 Step 1 ✓
- §4.4 consume pattern → Task 1 Step 1(b) + Task 4 Step 1 ✓
- §5 isAnimating + guards (7 entry point, btnMenu non bloccato) → Task 2 Step 3 ✓
- §6 test plan → Task 5 Step 3 ✓
- §7 file modified (2 main + 1 test) → tutte le task ✓

**Placeholder scan:** nessun TBD/TODO. Tutti i blocchi di codice sono completi e compilabili.

**Type/name consistency:** `AceMove`/`AceSource`/`consumeAutoAceMoves`/`_lastAutoAceMoves`/`maybeAnimateAutoAces` ortografati identicamente in Task 1 e Task 4. Constanti `REDEAL_*` e `ACE_*` consistenti tra spec §2 e Task 2 Step 1.
