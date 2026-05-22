# Tiramisù Stall Detection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add `TiramisuSolver.canProgress(state, maxDepth)` that performs a BFS to detect whether *any* sequence of legal moves of length ≤ `maxDepth` produces a state with a higher foundation card count. Wire it into `TiramisuViewModel.isLost()` so the "Hai perso" dialog appears even when legal-but-cyclic moves remain.

**Architecture:** Pure functions in `TiramisuSolver`: state mutation, move enumeration, ace auto-promotion, canonical hashing, and BFS with `visited` set. `isLost()` switches from `findHint() == null` to `!canProgress(s, MAX_LOOKAHEAD)`. `MAX_LOOKAHEAD = 30`.

**Tech Stack:** Kotlin · JUnit4 unit tests.

**Spec:** `docs/superpowers/specs/2026-05-22-stall-detection-design.md`.

---

## File Structure

**Files modified:**
- `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt` — adds `canProgress`, internal `enumerateLegalMoves`, `applyMove`, `autoPromoteAces`, `canonicalKey`, `foundationCardCount`, `Move` data class, `MAX_LOOKAHEAD` const.
- `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt:204` — `isLost()` uses `canProgress`.

**Files added:** none.

**Test files modified:**
- `app/src/test/java/com/bottazzini/tiramisu/TiramisuSolverTest.kt` — adds 12 new tests (§5.1-§5.12 in the spec).
- `app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt` — adds 2 new integration tests (§5.13-§5.14).

---

## Task 1: Add primitives to `TiramisuSolver` (data class + helpers + tests)

This task lays down the helper infrastructure that the BFS needs: `Move` data class, `enumerateLegalMoves`, `applyMove`, `autoPromoteAces`, `foundationCardCount`, `canonicalKey`. Each helper has its own focused test.

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt`
- Modify: `app/src/test/java/com/bottazzini/tiramisu/TiramisuSolverTest.kt`

### Step 1: Add primitive tests in `TiramisuSolverTest.kt`

Append these tests at the bottom of the class:

```kotlin
// ===== Stall-detection primitives =====

@Test fun `foundationCardCount sums ranks across slots`() {
    val s = state(
        piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
        foundations = listOf("b3", "c1", "zero", "s10")
    )
    assertEquals(3 + 1 + 0 + 10, TiramisuSolver.foundationCardCount(s))
}

@Test fun `foundationCardCount zero on empty foundations`() {
    val s = state(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()))
    assertEquals(0, TiramisuSolver.foundationCardCount(s))
}

@Test fun `canonicalKey equal for equivalent states`() {
    val s1 = state(
        piles       = listOf(listOf("c3"), listOf("c5"), emptyList(), emptyList()),
        foundations = listOf("b1", "zero", "zero", "zero")
    )
    val s2 = state(
        piles       = listOf(listOf("c3"), listOf("c5"), emptyList(), emptyList()),
        foundations = listOf("b1", "zero", "zero", "zero")
    )
    assertEquals(TiramisuSolver.canonicalKey(s1), TiramisuSolver.canonicalKey(s2))
}

@Test fun `canonicalKey differs when pile content differs`() {
    val s1 = state(piles = listOf(listOf("c3"), listOf("c5"), emptyList(), emptyList()))
    val s2 = state(piles = listOf(listOf("c3"), listOf("c7"), emptyList(), emptyList()))
    assertNotEquals(TiramisuSolver.canonicalKey(s1), TiramisuSolver.canonicalKey(s2))
}

@Test fun `enumerateLegalMoves includes foundation moves`() {
    // pile 0 top = b2, foundation bastoni already at b1 → b2 can go to foundation
    val s = state(
        piles       = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
        foundations = listOf("b1", "zero", "zero", "zero")
    )
    val moves = TiramisuSolver.enumerateLegalMoves(s)
    assertTrue(moves.any { it.toFoundation && it.fromPile == 0 })
}

@Test fun `enumerateLegalMoves under strict excludes higher-rank same-suit`() {
    // pile 0 = c7, pile 1 = c5. Under strict NORMALE, c7 -> c5 is invalid, c5 -> c7 is valid.
    val s = state(piles = listOf(listOf("c7"), listOf("c5"), emptyList(), emptyList()))
    val moves = TiramisuSolver.enumerateLegalMoves(s)
    val tableauMoves = moves.filter { !it.toFoundation }
    assertTrue(tableauMoves.any { it.fromPile == 1 && it.toPile == 0 })
    assertFalse(tableauMoves.any { it.fromPile == 0 && it.toPile == 1 })
}

@Test fun `enumerateLegalMoves includes single-card to empty pile`() {
    // Unlike findHint(), the BFS must consider these — they can unblock other moves.
    val s = state(piles = listOf(listOf("c3"), emptyList(), emptyList(), emptyList()))
    val moves = TiramisuSolver.enumerateLegalMoves(s)
    val toEmpty = moves.filter { !it.toFoundation && it.fromPile == 0 }
    assertTrue(toEmpty.isNotEmpty())
}

@Test fun `applyMove tableau move moves the top card`() {
    val s = state(piles = listOf(listOf("c5"), listOf("c7"), emptyList(), emptyList()))
    val move = TiramisuSolver.Move(fromPile = 0, toPile = 1, toFoundation = false)
    val next = TiramisuSolver.applyMove(s, move)
    assertEquals(listOf<String>(), next.piles[0])
    assertEquals(listOf("c7", "c5"), next.piles[1])
}

@Test fun `applyMove foundation move puts card on foundation`() {
    val s = state(
        piles       = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
        foundations = listOf("b1", "zero", "zero", "zero")
    )
    val move = TiramisuSolver.Move(fromPile = 0, toPile = -1, toFoundation = true)
    val next = TiramisuSolver.applyMove(s, move)
    assertEquals(listOf<String>(), next.piles[0])
    assertEquals("b2", next.foundations[0])
}

@Test fun `applyMove auto-promotes exposed ace`() {
    // pile 0 = [c1, c5]. Move c5 to pile 1 → c1 is exposed → auto-promoted to foundation.
    val s = state(piles = listOf(listOf("c1", "c5"), listOf("c7"), emptyList(), emptyList()))
    val move = TiramisuSolver.Move(fromPile = 0, toPile = 1, toFoundation = false)
    val next = TiramisuSolver.applyMove(s, move)
    assertEquals(listOf<String>(), next.piles[0])
    // c1 (ace of coppe) auto-moved to foundation
    assertTrue(next.foundations.any { it == "c1" })
}

@Test fun `applyMove does not mutate input state`() {
    val s = state(piles = listOf(listOf("c5"), listOf("c7"), emptyList(), emptyList()))
    val move = TiramisuSolver.Move(fromPile = 0, toPile = 1, toFoundation = false)
    TiramisuSolver.applyMove(s, move)
    // Original state must remain unchanged
    assertEquals(listOf("c5"), s.piles[0])
    assertEquals(listOf("c7"), s.piles[1])
}
```

Note: the import `assertNotEquals` is from `org.junit.Assert.*` which is already wildcarded in the file.

### Step 2: Run new tests — they must fail

Run: `cd /Users/bottazzini/Documents/misc/Tiramisu && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.tiramisu.TiramisuSolverTest"`

Expected: compilation errors (`TiramisuSolver.Move` doesn't exist, `enumerateLegalMoves`/`applyMove`/`autoPromoteAces`/`foundationCardCount`/`canonicalKey` don't exist).

### Step 3: Implement the primitives in `TiramisuSolver.kt`

Add inside the existing `object TiramisuSolver`, after the existing `findHint` block:

```kotlin
// ===== Stall-detection primitives =====

const val MAX_LOOKAHEAD = 30

data class Move(
    val fromPile:     Int,
    val toPile:       Int,        // -1 when toFoundation
    val toFoundation: Boolean
)

/** Sum of ranks on the 4 foundation slots ("zero" counts as 0). */
fun foundationCardCount(s: TiramisuGameState): Int =
    s.foundations.sumOf { top ->
        if (top == "zero") 0 else TiramisuMoveValidator.rank(top)
    }

/** Stable string representation of the state for the BFS visited set. */
fun canonicalKey(s: TiramisuGameState): String =
    s.piles.joinToString("|") { pile -> pile.joinToString(",") } +
    ";" + s.foundations.joinToString(",")

/**
 * Every legal move from [s] — foundation moves AND tableau-to-tableau,
 * including single-card-to-empty-pile (findHint skips these as hints,
 * but the BFS must consider them since they can unblock progress).
 */
fun enumerateLegalMoves(s: TiramisuGameState): List<Move> {
    val out = mutableListOf<Move>()
    val strict = s.difficulty.strictTableau
    for (src in 0..3) {
        val card = s.topOfPile(src)
        if (card == "zero") continue
        // Foundation moves
        for (fIdx in 0..3) {
            if (TiramisuMoveValidator.canMoveToFoundation(card, s.foundations[fIdx])) {
                out.add(Move(fromPile = src, toPile = -1, toFoundation = true))
                break  // only one foundation lands the card; no need to enumerate all
            }
        }
        // Tableau-to-tableau moves
        for (dst in 0..3) {
            if (src == dst) continue
            if (TiramisuMoveValidator.canMoveToTableau(card, s.topOfPile(dst), strict = strict)) {
                out.add(Move(fromPile = src, toPile = dst, toFoundation = false))
            }
        }
    }
    return out
}

/**
 * Returns a NEW state with [move] applied. Does not mutate [s].
 * After the move, any aces exposed on pile tops are auto-promoted to foundations,
 * mirroring TiramisuViewModel.autoMoveAces().
 */
fun applyMove(s: TiramisuGameState, move: Move): TiramisuGameState {
    val next = s.deepCopy()
    val card = next.piles[move.fromPile].removeAt(next.piles[move.fromPile].size - 1)
    if (move.toFoundation) {
        val fIdx = next.foundations.indexOfFirst { f ->
            TiramisuMoveValidator.canMoveToFoundation(card, f)
        }
        // fIdx should always be >= 0 because the move was enumerated as legal
        next.foundations[fIdx] = card
    } else {
        next.piles[move.toPile].add(card)
    }
    autoPromoteAces(next)
    return next
}

/** Repeatedly promote any pile-top ace to a foundation slot. Mutates [s]. */
private fun autoPromoteAces(s: TiramisuGameState) {
    var moved = true
    while (moved) {
        moved = false
        for (pileIdx in 0..3) {
            val top = s.topOfPile(pileIdx)
            if (top == "zero" || TiramisuMoveValidator.rank(top) != 1) continue
            for (fIdx in 0..3) {
                if (TiramisuMoveValidator.canMoveToFoundation(top, s.foundations[fIdx])) {
                    s.piles[pileIdx].removeAt(s.piles[pileIdx].size - 1)
                    s.foundations[fIdx] = top
                    moved = true
                    break
                }
            }
        }
    }
}
```

### Step 4: Run the tests — they must pass

Run: `cd /Users/bottazzini/Documents/misc/Tiramisu && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.tiramisu.TiramisuSolverTest"`

Expected: PASS.

### Step 5: Commit

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt \
        app/src/test/java/com/bottazzini/tiramisu/TiramisuSolverTest.kt
git commit -m "feat(solver): primitives for stall detection BFS"
```

---

## Task 2: Implement `canProgress` BFS + comprehensive tests

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt`
- Modify: `app/src/test/java/com/bottazzini/tiramisu/TiramisuSolverTest.kt`

### Step 1: Add the `canProgress` tests

Append at the bottom of `TiramisuSolverTest`:

```kotlin
// ===== canProgress: progress detected =====

@Test fun `canProgress true when foundation move available immediately`() {
    val s = state(
        piles       = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
        foundations = listOf("b1", "zero", "zero", "zero")
    )
    assertTrue(TiramisuSolver.canProgress(s, 30))
}

@Test fun `canProgress true when ace is reachable in 2 moves`() {
    // pile 0 = [c1, c5], pile 1 = c7. Move c5 -> c7 (5<7 strict ok), c1 auto-promotes.
    val s = state(piles = listOf(listOf("c1", "c5"), listOf("c7"), emptyList(), emptyList()))
    assertTrue(TiramisuSolver.canProgress(s, 2))
    assertTrue(TiramisuSolver.canProgress(s, 30))
}

@Test fun `canProgress true when single card to empty pile unblocks foundation`() {
    // pile 0 = s5, pile 1 = [s1, s3], pile 2 = empty, pile 3 = empty.
    // Move s3 -> pile 2 (single -> empty), then s1 exposed -> foundation.
    val s = state(
        piles = listOf(listOf("s5"), listOf("s1", "s3"), emptyList(), emptyList())
    )
    assertTrue(TiramisuSolver.canProgress(s, 30))
}

@Test fun `canProgress true when foundation at 9 and 10 reachable`() {
    val s = state(
        piles       = listOf(listOf("b10"), emptyList(), emptyList(), emptyList()),
        foundations = listOf("b9", "zero", "zero", "zero")
    )
    assertTrue(TiramisuSolver.canProgress(s, 30))
}

@Test fun `canProgress true on freshly dealt state (sanity)`() {
    // Initial deal scenario from the tutorial deck: pile 0 empty, pile 1 = s6,
    // pile 2 = c3, pile 3 = d8, foundation bastoni = b1 (auto-promoted).
    // Plenty of options to progress (the tutorial deck is solvable).
    val s = state(
        piles       = listOf(emptyList(), listOf("s6"), listOf("c3"), listOf("d8")),
        foundations = listOf("b1", "zero", "zero", "zero"),
        stock       = listOf("c5", "c7", "d3", "b2")
    )
    assertTrue(TiramisuSolver.canProgress(s, 30))
}

// ===== canProgress: stall detected =====

@Test fun `no progress in classic 2-pile cycle c3 c5 strict`() {
    val s = state(
        piles = listOf(listOf("c3"), listOf("c5"), emptyList(), emptyList())
    )
    assertFalse(TiramisuSolver.canProgress(s, 30))
}

@Test fun `no progress when only buried aces and high cards block them`() {
    // Aces are at the bottom of piles, covered by cards that can't be moved off:
    // pile 0 = [c1, c10] (under strict, c10 can't move on any pile top of same suit
    //                     and other suits = different suit = blocked)
    // pile 1 = [b1, b10]
    // pile 2 = [d1, d10]
    // pile 3 = [s1, s10]
    // No tableau moves possible (all tops different suits, and even if same-suit,
    // a 10 has no higher destination). No foundation moves. Stalled.
    val s = state(
        piles = listOf(
            listOf("c1", "c10"),
            listOf("b1", "b10"),
            listOf("d1", "d10"),
            listOf("s1", "s10")
        )
    )
    assertFalse(TiramisuSolver.canProgress(s, 30))
}

// ===== canProgress: difficulty awareness =====

@Test fun `canProgress under FACILE finds progress that strict NORMALE blocks`() {
    // pile 0 = [c1, c3], pile 1 = c5. Under FACILE (lax) we can move c3 onto c5
    // (same suit, any rank), exposing c1 → foundation. Under NORMALE (strict)
    // c3 -> c5 is valid (3<5) so this state ALSO progresses under strict.
    // To make the test meaningful we need c3 -> c5 to be the ONLY path and we need
    // strict to disallow it. Use ranks where strict blocks: pile 0 = [c1, c7], pile 1 = c5.
    // Under FACILE: c7 -> c5 (same suit) ok → c1 exposed → foundation.
    // Under NORMALE: c7 -> c5 invalid (7>5), c5 -> c7 invalid alone (c1 still buried).
    // pile 1 has c5, after c5 -> c7 (if it were the move) pile 1 = empty, pile 0 = [c1,c7,c5],
    // then top is c5, no foundation (need rank 1 -> need to expose c1 by moving c7 and c5).
    // Hmm — under strict NORMALE there is no path; under FACILE there is.
    val sStrict = state(
        piles       = listOf(listOf("c1", "c7"), listOf("c5"), emptyList(), emptyList()),
        foundations = listOf("zero", "zero", "zero", "zero")
    )
    assertFalse(TiramisuSolver.canProgress(sStrict, 30))

    // Same layout but FACILE — c7 -> c5 ok, c1 exposed, c1 promotes.
    val sLax = TiramisuGameState(
        piles       = listOf(mutableListOf("c1","c7"), mutableListOf("c5"), mutableListOf(), mutableListOf()),
        stock       = mutableListOf(),
        foundations = mutableListOf("zero","zero","zero","zero"),
        redealsLeft = 0,
        difficulty  = Difficulty.FACILE,
        initialDeck = emptyList()
    )
    assertTrue(TiramisuSolver.canProgress(sLax, 30))
}

// ===== canProgress: boundary & performance =====

@Test fun `canProgress respects maxDepth boundary`() {
    // Construct a state where progress requires exactly 3 moves.
    // pile 0 = [c1, c3, c5], pile 1 = c7, pile 2 = empty, pile 3 = empty.
    // Steps: c5 -> c7 (5<7 ok, depth 1) → top c3 -> pile 2 (single -> empty, depth 2) →
    //        c1 exposed → auto-promote in same applyMove (no separate move needed)
    // Actually: after c3 -> pile 2, c1 is on pile 0 top → autoPromoteAces runs in applyMove,
    // so progress is detected at depth 2.
    val s = state(
        piles = listOf(listOf("c1", "c3", "c5"), listOf("c7"), emptyList(), emptyList())
    )
    assertTrue(TiramisuSolver.canProgress(s, 2))
    // With maxDepth = 1, BFS can only do one move (c5 -> c7), which doesn't expose c1 alone.
    // After c5 -> c7, pile 0 = [c1, c3] (no progress yet). depth 1 nodes are not expanded
    // further because depth >= maxDepth. So canProgress(s, 1) = false.
    assertFalse(TiramisuSolver.canProgress(s, 1))
}

@Test fun `canProgress terminates within 200ms on dense state`() {
    // Dense state: 4 piles with 7 cards each (28 cards), foundations all empty.
    // No aces visible to short-circuit. Goal: confirm BFS doesn't blow up.
    val s = state(
        piles = listOf(
            listOf("c10","c9","c8","c7","c6","c5","c4"),
            listOf("b10","b9","b8","b7","b6","b5","b4"),
            listOf("d10","d9","d8","d7","d6","d5","d4"),
            listOf("s10","s9","s8","s7","s6","s5","s4")
        )
    )
    val startNs = System.nanoTime()
    val result = TiramisuSolver.canProgress(s, 30)
    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
    assertTrue("canProgress took ${elapsedMs}ms (expected < 200)", elapsedMs < 200)
    // We don't assert on result — the test is purely a performance regression test.
    // (For the record, this particular state is stalled since no aces are visible
    // and no foundation can be started.)
    assertFalse(result)  // sanity, since no aces are visible and no progress is possible
}
```

### Step 2: Run new tests — they must fail (no `canProgress` yet)

Run: `cd /Users/bottazzini/Documents/misc/Tiramisu && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.tiramisu.TiramisuSolverTest"`

Expected: compilation failure on `TiramisuSolver.canProgress` (function doesn't exist yet).

### Step 3: Implement `canProgress` in `TiramisuSolver.kt`

Add this function in the "Stall-detection primitives" block:

```kotlin
/**
 * Returns true if SOME sequence of legal moves of length <= [maxDepth] produces
 * a state with higher [foundationCardCount] than [start]. Returns false if no such
 * sequence exists within the lookahead — the state is considered stalled.
 *
 * BFS with a `visited` set keyed by [canonicalKey] to prevent cycle re-exploration.
 */
fun canProgress(start: TiramisuGameState, maxDepth: Int): Boolean {
    val initialCount = foundationCardCount(start)
    val visited = hashSetOf(canonicalKey(start))
    val queue = ArrayDeque<Pair<TiramisuGameState, Int>>()
    queue.addLast(start to 0)

    while (queue.isNotEmpty()) {
        val (s, depth) = queue.removeFirst()
        if (depth >= maxDepth) continue
        for (move in enumerateLegalMoves(s)) {
            val next = applyMove(s, move)
            if (foundationCardCount(next) > initialCount) return true
            val key = canonicalKey(next)
            if (visited.add(key)) {
                queue.addLast(next to depth + 1)
            }
        }
    }
    return false
}
```

### Step 4: Run the tests — must pass

Run: `cd /Users/bottazzini/Documents/misc/Tiramisu && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.tiramisu.TiramisuSolverTest"`

Expected: PASS.

If the `canProgress respects maxDepth boundary` test fails because the auto-promote-ace happens inside `applyMove` after the move counter, double-check that the assertion text matches actual depth semantics in your implementation. Adjust the test to match the agreed boundary semantics (the standard reading is: `depth` counts moves played, ace auto-promotion is part of the move that exposed it).

### Step 5: Commit

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt \
        app/src/test/java/com/bottazzini/tiramisu/TiramisuSolverTest.kt
git commit -m "feat(solver): canProgress BFS for stall detection"
```

---

## Task 3: Wire `canProgress` into `TiramisuViewModel.isLost()` + integration tests

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt:204`
- Modify: `app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt`

### Step 1: Add integration tests in `TiramisuViewModelTest.kt`

Find the existing `isWon` / `isLost` section and append:

```kotlin
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
```

Note: confirm that `stateWith` accepts the `stock` and `redealsLeft` parameters in the existing helper. If not, adapt the helper signature in the test file to accept them. Inspect the existing helper at the top of `TiramisuViewModelTest.kt` before adding the calls above. (Several existing tests already use `stateWith` with similar parameters — match their pattern.)

### Step 2: Run the tests — must fail

Run: `cd /Users/bottazzini/Documents/misc/Tiramisu && ./gradlew :app:testDebugUnitTest --tests "com.bottazzini.tiramisu.TiramisuViewModelTest"`

Expected: the first test (`isLost true in classic c3 c5 cycle stall`) FAILS because current `isLost()` only checks `findHint() == null`, and `findHint` returns a move in this cycle. The second test PASSES (foundation move is detected by `findHint` too).

### Step 3: Modify `isLost()` in `TiramisuViewModel.kt`

Find the current body (lines 204-210):

```kotlin
fun isLost(): Boolean {
    val s = state ?: return false
    if (s.isWon()) return false
    if (s.stock.isNotEmpty()) return false
    if (canRedeal()) return false
    return TiramisuSolver.findHint(s) == null
}
```

Replace with:

```kotlin
fun isLost(): Boolean {
    val s = state ?: return false
    if (s.isWon()) return false
    if (s.stock.isNotEmpty()) return false
    if (canRedeal()) return false
    return !TiramisuSolver.canProgress(s, TiramisuSolver.MAX_LOOKAHEAD)
}
```

### Step 4: Run all tests

Run: `cd /Users/bottazzini/Documents/misc/Tiramisu && ./gradlew :app:testDebugUnitTest`

Expected: PASS on every test class.

If the previously-passing `isLost` tests start to fail, debug by reading the test scenario and verifying it's still a stalled state under the new definition. If a test scenario is meant to be a "lost" state under the old (`findHint == null`) definition but is actually winnable under the new (`canProgress`) definition, the old test was incorrect; update the test setup to ensure a genuine stall, or keep the new behavior and adjust the test name.

### Step 5: Commit

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt \
        app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt
git commit -m "feat(viewmodel): isLost detects cyclic stalls via canProgress"
```

---

## Task 4: Final verification

**Files:** none.

### Step 1: Full unit test pass

Run: `cd /Users/bottazzini/Documents/misc/Tiramisu && ./gradlew :app:testDebugUnitTest`
Expected: PASS.

### Step 2: APK build

Run: `cd /Users/bottazzini/Documents/misc/Tiramisu && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

### Step 3: Manual smoke test (on device/emulator)

Provoke a cycle on Difficile (e.g., layout where only `c3 ↔ c5` movements are possible, stock empty, redeal exhausted). Verify the "Hai perso" dialog appears within 1 move after entering the cycle.

### Step 4: Performance sanity

If `isLost` is perceptibly slow during normal play (each move triggers a BFS), measure with a couple of log statements around the call site. Expected < 100 ms per call on a mid-range device. If too slow, possible mitigations:
- Run `canProgress` only when `findHint() == null` AND a "no foundation progress in last K moves" heuristic flags suspicion. Costs more complexity; defer unless needed.
- Run `isLost` on a background coroutine. Costs concurrency complexity; defer unless needed.

No code change in this task unless performance issue is observed.
