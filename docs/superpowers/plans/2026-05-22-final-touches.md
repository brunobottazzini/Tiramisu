# Final Touches Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a sound toggle, rename + properly implement "Auto-completion", and implement "Fast deal" (transfer entire deck) — three small additions to settings that complete v1.0 polish.

**Architecture:** Additive only. New `SOUND_ENABLED` config key + gate `playSound()` in 4 Activities. Rename existing `autoMoveAces` pipeline to a generic `autoMoveToFoundation` controlled by a `autoCompleteEnabled` flag injected from `GameActivity`. Add `dealAllFromStock()` method that chains multiple deal waves until stock empty OR Obbligato stop (DIFFICILE + auto-completion OFF). No DB migration.

**Tech Stack:** Kotlin, Android SDK 36, JUnit 4 (unit tests in `app/src/test/`), Gradle (`./gradlew test`).

**Reference spec:** `docs/superpowers/specs/2026-05-22-final-touches-design.md`

---

## Phase A — Sound setting

### Task A1: Add SOUND_ENABLED to Configuration enum

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/settings/SettingsHandler.kt`

- [ ] **Step 1: Add the enum value and default**

In `SettingsHandler.kt`, add `SOUND_ENABLED("soundEnabled")` to the `Configuration` enum (after `DIFFICULTY` to keep newer entries at the bottom):

```kotlin
enum class Configuration(val value: String) {
    FAST_DEAL("fastDeal"),
    CARD_BACK("cardBack"),
    BACKGROUND("background"),
    CARD_TYPE("cardType"),
    HINT_ENABLED("hintEnabled"),
    AUTO_MOVE("autoMove"),
    DIFFICULTY("difficulty"),
    SOUND_ENABLED("soundEnabled")
}
```

And in `insertDefaultSettings()` add at the end:

```kotlin
setDefaultSetting(Configuration.SOUND_ENABLED.value, "enabled")
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/settings/SettingsHandler.kt
git commit -m "feat(settings): add SOUND_ENABLED configuration key"
```

---

### Task A2: Add sound strings to all locales

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-{de,es,fr,hi,it,ja,ko,nl,pl,pt,pt-rBR,pt-rPT,ru,th,tr,zh-rCN}/strings.xml` (16 locales)

- [ ] **Step 1: Add EN strings to `values/strings.xml`**

Locate the existing `<string name="settings_label_auto_move">` and add right after `<string name="auto_move_with_icon">...</string>`:

```xml
<string name="settings_label_sound">Sounds</string>
<string name="sound_with_icon">🔊 Sounds</string>
```

- [ ] **Step 2: Add IT strings to `values-it/strings.xml`**

Same position, IT translation:

```xml
<string name="settings_label_sound">Suoni</string>
<string name="sound_with_icon">🔊 Suoni</string>
```

- [ ] **Step 3: Add translations to the other 15 locales**

Use the same key names and emoji 🔊 prefix. Translations:

- `values-de/strings.xml`: `Geräusche` / `🔊 Geräusche`
- `values-es/strings.xml`: `Sonidos` / `🔊 Sonidos`
- `values-fr/strings.xml`: `Sons` / `🔊 Sons`
- `values-hi/strings.xml`: `ध्वनियाँ` / `🔊 ध्वनियाँ`
- `values-ja/strings.xml`: `サウンド` / `🔊 サウンド`
- `values-ko/strings.xml`: `사운드` / `🔊 사운드`
- `values-nl/strings.xml`: `Geluiden` / `🔊 Geluiden`
- `values-pl/strings.xml`: `Dźwięki` / `🔊 Dźwięki`
- `values-pt/strings.xml`: `Sons` / `🔊 Sons`
- `values-pt-rBR/strings.xml`: `Sons` / `🔊 Sons`
- `values-pt-rPT/strings.xml`: `Sons` / `🔊 Sons`
- `values-ru/strings.xml`: `Звуки` / `🔊 Звуки`
- `values-th/strings.xml`: `เสียง` / `🔊 เสียง`
- `values-tr/strings.xml`: `Sesler` / `🔊 Sesler`
- `values-zh-rCN/strings.xml`: `声音` / `🔊 声音`

Position: same place as IT (right after the `auto_move_with_icon` entry).

- [ ] **Step 4: Verify it builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values*/strings.xml
git commit -m "feat(i18n): add sound toggle label strings"
```

---

### Task A3: Add switch row to settings.xml + handler

**Files:**
- Modify: `app/src/main/res/layout/settings.xml`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/SettingsActivity.kt`

- [ ] **Step 1: Add the switch row to settings.xml**

In `app/src/main/res/layout/settings.xml`, locate the existing `autoMoveRow` block (around line 402-430). Add a new sibling block immediately after `</LinearLayout>` of `autoMoveRow` and before the difficulty section. Also update the difficulty `labelDifficulty` `app:layout_constraintTop_toBottomOf` to point to `@id/soundRow`.

```xml
        <!-- Sound switch -->
        <LinearLayout
            android:id="@+id/soundRow"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/casino_tile_bg"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/autoMoveRow">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/sound_with_icon"
                style="@style/CasinoBodyText" />

            <Switch
                android:id="@+id/switchSound"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:thumb="@drawable/casino_gold_switch_thumb"
                android:track="@drawable/casino_gold_switch_track"
                android:onClick="changeSoundEnabled" />
        </LinearLayout>
```

And update the `labelDifficulty` constraint:

```xml
        <!-- BEFORE -->
        app:layout_constraintTop_toBottomOf="@id/autoMoveRow" />

        <!-- AFTER -->
        app:layout_constraintTop_toBottomOf="@id/soundRow" />
```

- [ ] **Step 2: Add handler in SettingsActivity**

In `SettingsActivity.kt`, add this method right after `changeAutoMove(view: View)`:

```kotlin
fun changeSoundEnabled(view: View) {
    val switch = view as Switch
    val value = if (switch.isChecked) "enabled" else "disabled"
    settingsHandler.updateSetting(Configuration.SOUND_ENABLED.value, value)
}
```

- [ ] **Step 3: Read setting in readConfigurations**

In `SettingsActivity.kt` `readConfigurations()`, add at the end (before `applyScreenBackground(background)`):

```kotlin
val sound = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) ?: "enabled"
findViewById<Switch>(R.id.switchSound).isChecked = (sound == "enabled")
```

- [ ] **Step 4: Verify it builds and switches state visually**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. (Optional: install APK and verify the switch appears and toggles.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/settings.xml app/src/main/java/com/bottazzini/tiramisu/SettingsActivity.kt
git commit -m "feat(settings): add Sounds switch row + handler"
```

---

### Task A4: Gate playSound in all 4 Activities

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/MainActivity.kt`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/YouWonActivity.kt`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/SplashActivity.kt`

- [ ] **Step 1: Gate in GameActivity**

In `GameActivity.kt`, add a property near the other state fields (after `mediaPlayer`):

```kotlin
private var soundsEnabled: Boolean = true
```

In `onResume()` (if missing, add one that calls `super.onResume()`), read the setting:

```kotlin
override fun onResume() {
    super.onResume()
    soundsEnabled = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) != "disabled"
}
```

Note: `GameActivity` doesn't currently override `onResume()`. If it does, just add the line to the existing override. If not, add the full override above.

In the existing `playSound(resId: Int)` method (line ~1045), add a guard as the first line:

```kotlin
private fun playSound(resId: Int) {
    if (!soundsEnabled) return
    try {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resId)?.also {
            // ... rest unchanged
        }
    } catch (e: Exception) {
        // ... rest unchanged
    }
}
```

- [ ] **Step 2: Gate in MainActivity**

In `MainActivity.kt`, add the same property + onResume read + playSound guard, using the same pattern. `MainActivity` already has a `playSound(soundId: Int)` method around line 249. Note: `MainActivity` does not currently hold a `settingsHandler` reference — check if it does (`grep -n "SettingsHandler" MainActivity.kt`). If not, add:

```kotlin
private lateinit var settingsHandler: SettingsHandler
```

Initialize in `onCreate()`:

```kotlin
settingsHandler = SettingsHandler(applicationContext)
```

And in `onDestroy()`:

```kotlin
override fun onDestroy() {
    settingsHandler.close()
    super.onDestroy()
}
```

(Skip the destroy override modifications if it's already there — just add `settingsHandler.close()` to the existing one.)

- [ ] **Step 3: Gate in YouWonActivity**

In `YouWonActivity.kt`, the sound is played inline at line 102: `mediaPlayer = MediaPlayer.create(this, R.raw.youwin)`. Wrap it:

```kotlin
val soundsEnabled = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) != "disabled"
if (soundsEnabled) {
    mediaPlayer = MediaPlayer.create(this, R.raw.youwin)
    mediaPlayer?.start()
}
```

`YouWonActivity` may not currently hold a `settingsHandler`. If not, add the field, init in `onCreate`, close in `onDestroy` as in Step 2.

- [ ] **Step 4: Gate in SplashActivity**

In `SplashActivity.kt`, the sound is played inline at line 81: `mediaPlayer = MediaPlayer.create(this, R.raw.shuffle)`. Wrap it the same way. Add `settingsHandler` field if missing.

- [ ] **Step 5: Verify it builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Manual smoke test**

Install APK, toggle "Sounds" OFF in settings, then:
- Open a game → tap deal → no flipcard sound
- Return to menu → no change_activity sound
- Win a game → no youwin sound
- Restart app → no shuffle sound on splash

Toggle ON again → all sounds return.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt app/src/main/java/com/bottazzini/tiramisu/MainActivity.kt app/src/main/java/com/bottazzini/tiramisu/YouWonActivity.kt app/src/main/java/com/bottazzini/tiramisu/SplashActivity.kt
git commit -m "feat(sound): gate all playSound calls on SOUND_ENABLED setting"
```

---

## Phase B — Auto-completion (rename + behavior)

### Task B1: Rename Ace types/methods to AutoFoundation (atomic refactor)

This is a pure mechanical rename touching 4 files. Must be done in one commit so the project compiles. No behavior change.

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`
- Modify: `app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt`

Mapping:
- `AceMove` → `AutoFoundationMove`
- `AceSource` → `AutoFoundationSource`
- `autoMoveAces` → `autoMoveToFoundation`
- `consumeAutoAceMoves` → `consumeAutoFoundationMoves`
- `_lastAutoAceMoves` → `_lastAutoFoundationMoves`
- `animateAutoAces` (GameActivity only) → `animateAutoFoundation`
- `maybeAnimateAutoAces` (GameActivity only) → `maybeAnimateAutoFoundation`

- [ ] **Step 1: Rename in TiramisuViewModel.kt**

In `TiramisuViewModel.kt`, replace all occurrences:
- Symbol declarations (data class `AceMove`, enum `AceSource`, method `autoMoveAces`, method `consumeAutoAceMoves`, field `_lastAutoAceMoves`)
- All internal references (call sites for `autoMoveAces`, type annotations for `List<AceMove>` etc., `AceSource.STOCK`, `AceSource.PILE_TOP`)
- Doc comments mentioning `[consumeAutoAceMoves]` or `[_lastAutoAceMoves]`

After rename, the relevant declarations should read:

```kotlin
private var _lastAutoFoundationMoves: List<AutoFoundationMove> = emptyList()

fun consumeAutoFoundationMoves(): List<AutoFoundationMove> {
    val moves = _lastAutoFoundationMoves
    _lastAutoFoundationMoves = emptyList()
    return moves
}

// inside dealFromStock:
autoMoveToFoundation(AutoFoundationSource.STOCK)

// inside onFoundationTapped and movePileToPile:
autoMoveToFoundation(AutoFoundationSource.PILE_TOP)

private fun autoMoveToFoundation(defaultSource: AutoFoundationSource) {
    // body unchanged, only the local type names and the `_lastAutoAceMoves` assignment renamed
}

enum class AutoFoundationSource { STOCK, PILE_TOP }

data class AutoFoundationMove(
    val fromPile: Int,
    val toFoundation: Int,
    val card: String,
    val source: AutoFoundationSource
)
```

Reset locations in `newGame`, `newTutorialGame`, `restoreState`, `undo`, `retrySameGame`: `_lastAutoAceMoves = emptyList()` → `_lastAutoFoundationMoves = emptyList()`.

- [ ] **Step 2: Rename in GameActivity.kt**

In `GameActivity.kt`:
- `vm.consumeAutoAceMoves()` → `vm.consumeAutoFoundationMoves()` (line ~397)
- `private fun maybeAnimateAutoAces()` → `private fun maybeAnimateAutoFoundation()` (line ~396)
- `animateAutoAces(moves)` → `animateAutoFoundation(moves)` (line ~399)
- `private fun animateAutoAces(moves: List<AceMove>)` → `private fun animateAutoFoundation(moves: List<AutoFoundationMove>)` (line ~402)
- `AceSource.STOCK` → `AutoFoundationSource.STOCK` (line ~414)
- `AceSource.PILE_TOP` → `AutoFoundationSource.PILE_TOP` (line ~415)
- `maybeAnimateAutoAces()` call inside `onStockTapped()` (line ~221) → `maybeAnimateAutoFoundation()`
- Any other call sites of `maybeAnimateAutoAces` (grep to confirm): rename them too.

- [ ] **Step 3: Rename in TiramisuViewModelTest.kt**

In `TiramisuViewModelTest.kt`:
- `vm.consumeAutoAceMoves()` → `vm.consumeAutoFoundationMoves()` (lines ~187, ~196)
- `AceSource.STOCK` → `AutoFoundationSource.STOCK` (line ~192)
- Test name `dealFromStock auto-moves ace and records it for animation` can stay; tests verify the Ace case which is preserved exactly.

- [ ] **Step 4: Rename in TiramisuSolver.kt**

In `TiramisuSolver.kt` line ~102, update the comment:

```kotlin
// BEFORE:
// mirroring TiramisuViewModel.autoMoveAces().

// AFTER:
// mirroring TiramisuViewModel.autoMoveToFoundation() (the Ace-only subset of it).
```

- [ ] **Step 5: Verify compile + existing tests still pass**

Run: `./gradlew test`
Expected: all existing tests pass (regression check — Aces still auto-move identically).

- [ ] **Step 6: Verify nothing references old names**

Run: `grep -rn "AceMove\|AceSource\|autoMoveAces\|consumeAutoAceMoves\|_lastAutoAceMoves\|animateAutoAces\|maybeAnimateAutoAces" app/src/`
Expected: no matches (only the renamed forms remain).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt
git commit -m "refactor(viewmodel): rename Ace* to AutoFoundation* (no behavior change)"
```

---

### Task B2: Add autoCompleteEnabled flag + extended auto-move behavior (TDD)

**Files:**
- Modify: `app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt`

- [ ] **Step 1: Write the failing test for autoCompleteEnabled=false (regression)**

In `TiramisuViewModelTest.kt`, add this test at the bottom (before the closing `}`):

```kotlin
@Test fun `auto-complete disabled keeps only aces auto-moving`() {
    // foundation b1 already in, pile 0 has b2 on top.
    // With autoCompleteEnabled = false, b2 should NOT auto-move.
    stateWith(
        piles = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
        foundations = listOf("b1", "zero", "zero", "zero"),
        stock = listOf("c1", "d5", "s7", "b8")
    )
    vm.autoCompleteEnabled = false
    vm.dealFromStock()
    val s = vm.state!!
    // Ace c1 was dealt and auto-moved (default behavior).
    assertTrue("ace c1 must auto-move", s.foundations.any { it == "c1" })
    // b2 was NOT auto-moved (still on pile 0).
    assertEquals("b2", s.topOfPile(0))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.bottazzini.tiramisu.TiramisuViewModelTest.auto-complete disabled keeps only aces auto-moving"`
Expected: FAIL with "Unresolved reference: autoCompleteEnabled" (the property doesn't exist yet).

- [ ] **Step 3: Add the property to TiramisuViewModel**

In `TiramisuViewModel.kt`, add as a public mutable property near the top of the class (after `var selectedPileIndex`):

```kotlin
/**
 * When true, [autoMoveToFoundation] will auto-move ANY top-pile card that can
 * go to a foundation, not just Aces. Set by [GameActivity] in onResume from the
 * AUTO_MOVE setting. Default false preserves the pre-feature behavior.
 */
var autoCompleteEnabled: Boolean = false
```

- [ ] **Step 4: Run the test again — it should pass**

Run: `./gradlew test --tests "com.bottazzini.tiramisu.TiramisuViewModelTest.auto-complete disabled keeps only aces auto-moving"`
Expected: PASS (the flag exists, defaults false → only Aces auto-move, regression preserved).

- [ ] **Step 5: Write the failing test for autoCompleteEnabled=true**

Add this test right after the previous one:

```kotlin
@Test fun `auto-complete enabled moves any foundation-eligible top to foundation`() {
    // foundation b1 already in, pile 0 has b2 on top. b3 underneath via stock deal.
    stateWith(
        piles = listOf(listOf("b3", "b2"), emptyList(), emptyList(), emptyList()),
        foundations = listOf("b1", "zero", "zero", "zero")
    )
    vm.autoCompleteEnabled = true
    // Trigger autoMove by tapping foundation on the b2 (acts like the player sent it).
    // But simpler: just deal nothing, just probe via dealFromStock returning false path.
    // We need a public entry point that triggers autoMoveToFoundation. Use a foundation-tap on pile 0:
    val moved = vm.onFoundationTapped(0)
    assertTrue("b2 went to foundation", moved)
    val s = vm.state!!
    // b2 in foundation
    assertTrue(s.foundations.any { it == "b2" })
    // b3 also auto-moved by the chain because of autoCompleteEnabled
    assertTrue("b3 must auto-move next", s.foundations.any { it == "b3" })
    assertEquals("zero", s.topOfPile(0))
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `./gradlew test --tests "com.bottazzini.tiramisu.TiramisuViewModelTest.auto-complete enabled moves any foundation-eligible top to foundation"`
Expected: FAIL — b3 is not auto-moved because `autoMoveToFoundation` still only checks rank == 1.

- [ ] **Step 7: Extend autoMoveToFoundation to honor the flag**

In `TiramisuViewModel.kt`, replace the body of `autoMoveToFoundation`:

```kotlin
private fun autoMoveToFoundation(defaultSource: AutoFoundationSource) {
    val s = state ?: return
    val moves = mutableListOf<AutoFoundationMove>()
    var moved = true
    while (moved) {
        moved = false
        for (pileIdx in 0..3) {
            val card = s.topOfPile(pileIdx)
            if (card == "zero") continue
            val isAce = TiramisuMoveValidator.rank(card) == 1
            // Disabled: only Aces. Enabled: any rank.
            if (!autoCompleteEnabled && !isAce) continue
            for (fIdx in 0..3) {
                if (TiramisuMoveValidator.canMoveToFoundation(card, s.foundations[fIdx])) {
                    s.piles[pileIdx].removeAt(s.piles[pileIdx].size - 1)
                    s.foundations[fIdx] = card
                    moves.add(AutoFoundationMove(pileIdx, fIdx, card, defaultSource))
                    moved = true
                    break
                }
            }
        }
    }
    _lastAutoFoundationMoves = moves
}
```

- [ ] **Step 8: Run both tests**

Run: `./gradlew test --tests "com.bottazzini.tiramisu.TiramisuViewModelTest"`
Expected: ALL tests pass (the two new ones + the existing 18).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt
git commit -m "feat(viewmodel): autoCompleteEnabled flag extends auto-move to any rank"
```

---

### Task B3: Wire AUTO_MOVE setting + SOUND_ENABLED + FAST_DEAL reads into GameActivity.onResume

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`

- [ ] **Step 1: Add the three fields**

In `GameActivity.kt`, near the existing state fields (`soundsEnabled` was added in Task A4; add the other two right next to it):

```kotlin
private var soundsEnabled: Boolean = true
private var fastDealEnabled: Boolean = true
```

(`vm.autoCompleteEnabled` is the ViewModel property; no separate Activity-side field needed.)

- [ ] **Step 2: Read all three settings in onResume**

Locate the `onResume` override in `GameActivity` (created in Task A4 step 1). Extend it:

```kotlin
override fun onResume() {
    super.onResume()
    soundsEnabled = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) != "disabled"
    fastDealEnabled = settingsHandler.readValue(Configuration.FAST_DEAL.value) == "enabled"
    vm.autoCompleteEnabled = settingsHandler.readValue(Configuration.AUTO_MOVE.value) == "enabled"
}
```

- [ ] **Step 3: Verify it builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Manual smoke test**

Install APK. Toggle "Completamento automatico" OFF, start a game with a 2-on-pile where foundation is at 1 → 2 stays. Toggle ON, retry → 2 flies to foundation automatically.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt
git commit -m "feat(game): wire AUTO_MOVE/SOUND_ENABLED/FAST_DEAL into onResume"
```

---

### Task B4: Update auto-move label strings to "Completamento automatico"

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: 16 other `app/src/main/res/values-*/strings.xml`

Only the text changes — keys (`settings_label_auto_move`, `auto_move_with_icon`) stay the same.

- [ ] **Step 1: Update EN in values/strings.xml**

Replace:

```xml
<!-- BEFORE -->
<string name="settings_label_auto_move">Auto-move to final deck</string>
<string name="auto_move_with_icon">🎯 Auto-move to final deck</string>

<!-- AFTER -->
<string name="settings_label_auto_move">Auto-complete</string>
<string name="auto_move_with_icon">🎯 Auto-complete</string>
```

- [ ] **Step 2: Update IT in values-it/strings.xml**

```xml
<!-- BEFORE -->
<string name="settings_label_auto_move">Auto-muovi su end deck</string>
<string name="auto_move_with_icon">🎯 Auto-muovi su end deck</string>

<!-- AFTER -->
<string name="settings_label_auto_move">Completamento automatico</string>
<string name="auto_move_with_icon">🎯 Completamento automatico</string>
```

- [ ] **Step 3: Update the other 15 locales**

Per-locale translations of "Auto-complete":

- `values-de`: `Automatisches Vervollständigen` → `🎯 Automatisches Vervollständigen`
- `values-es`: `Autocompletar` → `🎯 Autocompletar`
- `values-fr`: `Auto-complétion` → `🎯 Auto-complétion`
- `values-hi`: `स्वतः पूर्ण` → `🎯 स्वतः पूर्ण`
- `values-ja`: `自動完了` → `🎯 自動完了`
- `values-ko`: `자동 완료` → `🎯 자동 완료`
- `values-nl`: `Automatisch voltooien` → `🎯 Automatisch voltooien`
- `values-pl`: `Automatyczne ukończenie` → `🎯 Automatyczne ukończenie`
- `values-pt`: `Auto-completar` → `🎯 Auto-completar`
- `values-pt-rBR`: `Auto-completar` → `🎯 Auto-completar`
- `values-pt-rPT`: `Auto-completar` → `🎯 Auto-completar`
- `values-ru`: `Автозавершение` → `🎯 Автозавершение`
- `values-th`: `เติมอัตโนมัติ` → `🎯 เติมอัตโนมัติ`
- `values-tr`: `Otomatik tamamlama` → `🎯 Otomatik tamamlama`
- `values-zh-rCN`: `自动完成` → `🎯 自动完成`

For each: locate the existing `settings_label_auto_move` and `auto_move_with_icon` entries and replace their text content only. Keep the key names.

- [ ] **Step 4: Verify it builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values*/strings.xml
git commit -m "feat(i18n): rename auto-move label to Auto-complete / Completamento automatico"
```

---

### Task B5: Drop "auto-move" mention from achievement descriptions

**Files:**
- Modify: 17 `app/src/main/res/values*/strings.xml` files (only the ones containing `achievement_no_assist_desc` and `achievement_perfectionist_desc`)

The motor in `AchievementEngine.kt` only checks `hintsUsed == 0 && redealsUsed == 0`. The mention of "auto-move" in the strings is inert — simplify it.

- [ ] **Step 1: Update EN in values/strings.xml**

```xml
<!-- BEFORE -->
<string name="achievement_no_assist_desc">Win without hints or auto-move</string>
<string name="achievement_perfectionist_desc">Win 3 games in a row without hints or auto-moves</string>

<!-- AFTER -->
<string name="achievement_no_assist_desc">Win without hints or redeals</string>
<string name="achievement_perfectionist_desc">Win 3 games in a row without hints or redeals</string>
```

- [ ] **Step 2: Update IT in values-it/strings.xml**

```xml
<!-- AFTER -->
<string name="achievement_no_assist_desc">Vinci senza hint o ridistribuzioni</string>
<string name="achievement_perfectionist_desc">Vinci 3 partite di fila senza hint o ridistribuzioni</string>
```

- [ ] **Step 3: Update the other 15 locales**

For each locale, find the two strings and replace "auto-move"/equivalent with "redeals"/equivalent. If the current translation does NOT mention auto-move (some locales may have simpler text), leave it alone.

Quick survey to find which locales need updating:

```bash
grep -rn "auto-move\|auto move\|automatic move\|automoves" app/src/main/res/values*/strings.xml
```

Update only the files that match. Translations of "redeals" should match each locale's existing term for redeal (search for the existing "redeal"-related string in the same file to copy the wording).

- [ ] **Step 4: Verify it builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values*/strings.xml
git commit -m "feat(i18n): drop inert auto-move mention from achievement descriptions"
```

---

## Phase C — Fast deal

### Task C1: Add DealWave data class + dealAllFromStock method (TDD)

**Files:**
- Modify: `app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt`

- [ ] **Step 1: Write the failing test — full drain**

Add to `TiramisuViewModelTest.kt`:

```kotlin
@Test fun `dealAllFromStock drains stock in waves when no Obbligato block`() {
    stateWith(
        piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
        stock = listOf("b3", "c5", "d7", "s2", "b4", "c6", "d8", "s3"),
        difficulty = Difficulty.NORMALE
    )
    val waves = vm.dealAllFromStock()
    assertEquals(2, waves.size)
    val s = vm.state!!
    assertTrue(s.stock.isEmpty())
    // First wave: 4 cards on piles 0..3
    val firstWaveCards = waves[0].cardsDealt.map { it.second }
    assertEquals(listOf("b3", "c5", "d7", "s2"), firstWaveCards)
}
```

- [ ] **Step 2: Run test — verify it fails**

Run: `./gradlew test --tests "com.bottazzini.tiramisu.TiramisuViewModelTest.dealAllFromStock drains stock in waves when no Obbligato block"`
Expected: FAIL with "Unresolved reference: dealAllFromStock".

- [ ] **Step 3: Implement DealWave + dealAllFromStock**

In `TiramisuViewModel.kt`, add the data class at the bottom of the file (with the other top-level types):

```kotlin
data class DealWave(
    val cardsDealt: List<Pair<Int, String>>,
    val autoFoundationMoves: List<AutoFoundationMove>
)
```

And add the method inside the class, right after `dealFromStock()`:

```kotlin
/**
 * Chain multiple deal waves until either the stock empties or — in DIFFICILE
 * with auto-complete OFF — an Obbligato condition appears that the system
 * cannot resolve automatically. A single [previousState] snapshot is captured
 * before the first wave so undo restores the pre-chain state.
 *
 * Returns the list of waves performed (each with the cards it dealt and any
 * auto-foundation moves it triggered) for the animation layer to consume.
 */
fun dealAllFromStock(): List<DealWave> {
    val s = state ?: return emptyList()
    if (s.stock.isEmpty()) return emptyList()
    val snapshot = s.deepCopy()
    val waves = mutableListOf<DealWave>()
    while (s.stock.isNotEmpty()) {
        val cards = mutableListOf<Pair<Int, String>>()
        val toDeal = minOf(4, s.stock.size)
        for (i in 0 until toDeal) {
            val card = s.stock.removeAt(0)
            s.piles[i].add(card)
            cards.add(i to card)
        }
        autoMoveToFoundation(AutoFoundationSource.STOCK)
        val moves = _lastAutoFoundationMoves.toList()
        _lastAutoFoundationMoves = emptyList()
        waves.add(DealWave(cards, moves))
        // Stop if Obbligato now blocks moves and auto-complete didn't dissolve it.
        if (s.difficulty.obbligato && obbligatoTargets().isNotEmpty()) break
    }
    selectedPileIndex = null
    previousState = snapshot
    return waves
}
```

- [ ] **Step 4: Run test — verify it passes**

Run: `./gradlew test --tests "com.bottazzini.tiramisu.TiramisuViewModelTest.dealAllFromStock drains stock in waves when no Obbligato block"`
Expected: PASS.

- [ ] **Step 5: Write the failing test — Obbligato stop**

Add to `TiramisuViewModelTest.kt`:

```kotlin
@Test fun `dealAllFromStock stops at Obbligato when auto-complete is off`() {
    // DIFFICILE + foundation b at "zero". Stock starts with b1 (ace, always auto-moves),
    // then b2 (foundation-eligible after b1 lands → Obbligato fires in DIFFICILE).
    // After wave 1: piles get b1, c5, d7, s2. b1 auto-moves to foundation.
    // After wave 2: piles get b2, c6, d8, s3. b2 is now foundation-eligible → Obbligato.
    // With autoCompleteEnabled = false, the system stops after wave 2 (b2 stays on top).
    stateWith(
        piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
        stock = listOf("b1", "c5", "d7", "s2", "b2", "c6", "d8", "s3", "b3", "c7", "d9", "s4"),
        difficulty = Difficulty.DIFFICILE
    )
    vm.autoCompleteEnabled = false
    val waves = vm.dealAllFromStock()
    assertEquals(2, waves.size)
    val s = vm.state!!
    // Stock has 4 cards left (the third wave that wasn't dealt).
    assertEquals(4, s.stock.size)
    // b2 still on pile 0 (Obbligato block prevented further deals AND didn't auto-move it).
    assertEquals("b2", s.topOfPile(0))
}

@Test fun `dealAllFromStock drains fully when auto-complete is on despite Obbligato`() {
    stateWith(
        piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
        stock = listOf("b1", "c5", "d7", "s2", "b2", "c6", "d8", "s3"),
        difficulty = Difficulty.DIFFICILE
    )
    vm.autoCompleteEnabled = true
    val waves = vm.dealAllFromStock()
    assertEquals(2, waves.size)
    val s = vm.state!!
    // Auto-complete dissolved Obbligato at each wave → chain reached stock-empty.
    assertTrue(s.stock.isEmpty())
    // b1 and b2 both auto-moved to foundation.
    assertTrue(s.foundations.any { it == "b2" })
}
```

- [ ] **Step 6: Run tests — verify they pass**

Run: `./gradlew test --tests "com.bottazzini.tiramisu.TiramisuViewModelTest"`
Expected: all tests pass (existing + 5 new total across B2 and C1).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt
git commit -m "feat(viewmodel): dealAllFromStock chains waves with Obbligato stop"
```

---

### Task C2: Implement fast deal animation in GameActivity

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`

- [ ] **Step 1: Add the wave gap constant**

In `GameActivity.kt` `companion object`, add right after the existing animation constants:

```kotlin
/** Gap between fast-deal waves so the player perceives discrete ondate. */
private const val FAST_DEAL_WAVE_GAP_MS = 120L
```

- [ ] **Step 2: Refactor onStockTapped to branch on fastDealEnabled**

Locate `onStockTapped()` (around line 211). Replace its body:

```kotlin
private fun onStockTapped() {
    if (isAnimating) return
    if (isTutorialMode) {
        val eng = tutorialEngine ?: return
        if (!eng.isStockDealStep()) return
    }
    val s = vm.state ?: return
    // Multi-wave path: only when fast deal is ON, not in tutorial, and stock has > 4 cards
    // (a single tap on ≤ 4 cards behaves identically with or without fast deal).
    if (fastDealEnabled && !isTutorialMode && s.stock.size > 4) {
        val waves = vm.dealAllFromStock()
        if (waves.isEmpty()) return
        playSound(R.raw.flipcard)
        animateFastDealChain(waves) {
            checkWin()
            checkLost()
        }
        return
    }
    val sizeBefore = s.piles.map { it.size }
    if (vm.dealFromStock()) {
        playSound(R.raw.flipcard)
        animateDeal(sizeBefore) {
            maybeAnimateAutoFoundation()
            checkWin()
            checkLost()
            if (isTutorialMode) advanceTutorial()
        }
    }
}
```

- [ ] **Step 3: Add animateFastDealChain method**

Each wave's `setStartDelay` is relative to NOW (when `animate()` is called). We use `postDelayed` to chain waves, so each wave starts ghost animations from delay=0 (relative to its own call time). We also need a foundation-ghost helper that doesn't toggle `isAnimating` (the chain owns it).

In `GameActivity.kt`, add a new private method right after `animateDeal(...)` (around line 390):

```kotlin
/**
 * Plays a chain of [DealWave]s as sequential ghost-animation runs separated by
 * [FAST_DEAL_WAVE_GAP_MS]. Each wave animates its dealt cards (stock → pile) and
 * its auto-foundation moves (pile → foundation). Blocks user interaction for the
 * entire chain via [isAnimating]. State has already been mutated by [vm.dealAllFromStock];
 * we [renderAll] up-front so ghost positions are stable, then play overlay animations.
 */
private fun animateFastDealChain(waves: List<TiramisuViewModel.DealWave>, onComplete: () -> Unit) {
    isAnimating = true
    val s = vm.state ?: run { isAnimating = false; onComplete(); return }

    val gameRootContainer = gameRoot as ConstraintLayout
    val gameRootPos = locationOnScreen(gameRootContainer)
    val stockPos = locationOnScreen(stockArea)
    val density = resources.displayMetrics.density
    val peekPx = (CARD_PEEK_DP * density).toInt()

    // Render the final state up-front so card positions are stable for target computation.
    renderAll()

    // Reconstruct each pile's pre-chain size: finalSize - dealt + foundationOut.
    val finalSizes = IntArray(4) { s.piles[it].size }
    val perPileDealt = IntArray(4).apply {
        for (w in waves) for ((p, _) in w.cardsDealt) this[p]++
    }
    val perPileFoundationOut = IntArray(4).apply {
        for (w in waves) for (m in w.autoFoundationMoves) this[m.fromPile]++
    }
    // pileSize is mutated as the animation progresses: incremented on deal, decremented on foundation.
    val pileSize = IntArray(4) { p -> finalSizes[p] - perPileDealt[p] + perPileFoundationOut[p] }

    val ghosts = mutableListOf<ImageView>()

    fun playWave(idx: Int) {
        if (idx >= waves.size) {
            ghosts.forEach { gameRootContainer.removeView(it) }
            isAnimating = false
            onComplete()
            return
        }
        val wave = waves[idx]
        // 1) Deal-card ghosts, staggered from delay=0 (relative to this wave's t0).
        var stagger = 0L
        for ((pileIdx, card) in wave.cardsDealt) {
            val container = pileContainers[pileIdx] ?: continue
            val cardW = container.width - container.paddingLeft - container.paddingRight
            if (cardW <= 0) continue
            val cardH = (cardW * (CARD_ASPECT_H / CARD_ASPECT_W)).toInt()
            val contPos = locationOnScreen(container)
            val targetX = (contPos[0] - gameRootPos[0]).toFloat()
            val targetY = (contPos[1] + pileSize[pileIdx] * peekPx - gameRootPos[1]).toFloat()

            val resId = resources.getIdentifier("${cardType}_$card", "drawable", packageName)
            val ghost = ImageView(this).apply {
                if (resId != 0) setImageResource(resId)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = ConstraintLayout.LayoutParams(cardW, cardH)
                translationX = (stockPos[0] - gameRootPos[0]).toFloat()
                translationY = (stockPos[1] - gameRootPos[1]).toFloat()
            }
            gameRootContainer.addView(ghost)
            ghosts.add(ghost)
            ghost.animate()
                .translationX(targetX)
                .translationY(targetY)
                .setDuration(DEAL_CARD_DURATION_MS)
                .setStartDelay(stagger)
                .start()
            stagger += DEAL_CARD_STAGGER_MS
            pileSize[pileIdx]++
        }
        val cardsDuration = if (wave.cardsDealt.isEmpty()) 0L
            else (wave.cardsDealt.size - 1) * DEAL_CARD_STAGGER_MS + DEAL_CARD_DURATION_MS

        // 2) Foundation ghosts, fired after deal-cards land.
        val foundationDuration = if (wave.autoFoundationMoves.isEmpty()) 0L
            else (wave.autoFoundationMoves.size - 1) * ACE_STAGGER_MS + ACE_DURATION_MS

        if (wave.autoFoundationMoves.isNotEmpty()) {
            gameRoot.postDelayed({
                animateFoundationGhosts(wave.autoFoundationMoves, ghosts, gameRootContainer, gameRootPos)
                for (m in wave.autoFoundationMoves) {
                    pileSize[m.fromPile] = (pileSize[m.fromPile] - 1).coerceAtLeast(0)
                }
            }, cardsDuration)
        }

        // 3) Schedule next wave after this wave fully completes + gap.
        gameRoot.postDelayed({
            playWave(idx + 1)
        }, cardsDuration + foundationDuration + FAST_DEAL_WAVE_GAP_MS)
    }

    playWave(0)
}

/**
 * Subset of [animateAutoFoundation] used inside the fast-deal chain. Does NOT toggle
 * [isAnimating] (the chain owns that lifecycle) and adds ghosts to a shared accumulator
 * for the caller to clean up at chain end. Ghost source position is the top child of
 * the originating pile, falling back to the stock area.
 */
private fun animateFoundationGhosts(
    moves: List<TiramisuViewModel.AutoFoundationMove>,
    ghostsAccumulator: MutableList<ImageView>,
    gameRootContainer: ConstraintLayout,
    gameRootPos: IntArray
) {
    for ((idx, move) in moves.withIndex()) {
        val destView = foundationViews[move.toFoundation] ?: continue
        val resId = resources.getIdentifier("${cardType}_${move.card}", "drawable", packageName)
        if (resId == 0) continue
        val container = pileContainers[move.fromPile]
        val sourceLoc = container?.let {
            val topChild = it.getChildAt(it.childCount - 1)
            if (topChild != null) locationOnScreen(topChild) else locationOnScreen(stockArea)
        } ?: locationOnScreen(stockArea)
        val destLoc = locationOnScreen(destView)
        val ghost = ImageView(this).apply {
            setImageResource(resId)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = ConstraintLayout.LayoutParams(destView.width, destView.height)
            translationX = (sourceLoc[0] - gameRootPos[0]).toFloat()
            translationY = (sourceLoc[1] - gameRootPos[1]).toFloat()
        }
        gameRootContainer.addView(ghost)
        ghostsAccumulator.add(ghost)
        ghost.animate()
            .translationX((destLoc[0] - gameRootPos[0]).toFloat())
            .translationY((destLoc[1] - gameRootPos[1]).toFloat())
            .setDuration(ACE_DURATION_MS)
            .setStartDelay(idx * ACE_STAGGER_MS)
            .start()
    }
}
```

- [ ] **Step 4: Verify it builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual smoke test**

Install APK with FAST_DEAL ON (default). Start a NORMALE game, deal once to start, then redeal so stock is full. Tap stock → all remaining cards should cascade onto piles in ~3-4 seconds with visible animation.

Then: turn FAST_DEAL OFF in settings, restart same game → tap stock = single wave (4 cards), as before.

Edge case: DIFFICILE + auto-completion OFF + fast deal ON → tap fast deal: the chain should stop visibly when a foundation-eligible top appears (Obbligato highlight, if implemented in renderAll, will show).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt
git commit -m "feat(game): fast deal animation chains waves with auto-foundation handoff"
```

---

## Phase D — Verification

### Task D1: Full regression run + manual scenario sweep

- [ ] **Step 1: Run all unit tests**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL. All `TiramisuViewModelTest` tests pass (original + 5 new).

- [ ] **Step 2: Build release APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual sweep checklist (install APK)**

- [ ] Sounds toggle ON → flipcard during deal, change_activity on nav, youwin on victory, shuffle on splash all heard.
- [ ] Sounds toggle OFF → all four channels silent.
- [ ] Auto-completion OFF → only Aces auto-move. A 2 sitting on a pile when foundation is at 1 stays put until tapped.
- [ ] Auto-completion ON → that same 2 auto-flies to foundation. Chains (3, 4, ...) follow on the same animation step.
- [ ] Fast deal ON + stock > 4 cards → single tap drains all waves; animation visible ~3-4s.
- [ ] Fast deal OFF → single tap = 4 cards, as before.
- [ ] DIFFICILE + Auto-completion OFF + Fast deal ON → chain stops when a top-pile becomes foundation-eligible. Top highlighted (Obbligato).
- [ ] DIFFICILE + Auto-completion ON + Fast deal ON → chain drains fully; aces and other foundation-eligible cards fly to foundation between waves.
- [ ] Undo after a fast deal → state returns to before-fast-deal (stock full, piles empty).
- [ ] App close/reopen → settings preserved; game resumes correctly.

- [ ] **Step 4: Tag the release-candidate commit (optional)**

If desired:

```bash
git tag v1.0-rc-final-touches
```

No commit needed for D1 itself.

---

## Out of scope

- Renaming DB key `autoMove` → `autoComplete` (preserves existing user values).
- Granular per-category sound mute.
- Auto-completion expanded to tableau→tableau (foundation only).
- Modifying `AchievementEngine.kt` logic (criterion already not tied to auto-move).
- Dedicated "victory lap" animation (auto-completion makes end-game self-driving when enabled).
- Long-press / double-tap shortcut for fast deal (single-tap-when-ON is sufficient).
