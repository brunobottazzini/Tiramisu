# Tiramisù App — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new Android solitaire app "Tiramisù" forked from Trasloco, with the Tiramisu card game rules, 3 difficulty levels, tutorial, and full statistics.

**Architecture:** Hard fork of Trasloco — copy the entire project, rename the package to `com.bottazzini.tiramisu`, delete Trasloco's game engine, then implement the Tiramisu game engine from scratch. All assets (card images, sounds, backgrounds) and infrastructure (DB, settings, themes, deck picker) are reused.

**Tech Stack:** Android (Kotlin), SQLite via SQLiteOpenHelper, JUnit 4, AndroidX Espresso, ConstraintLayout, Glide.

**Note on git:** No git repo exists yet. The `git commit` steps below are staged for later. Once development is complete, run `git init && git add . && git commit -m "feat: initial Tiramisù v1.0"` from the project root.

**Spec:** `docs/superpowers/specs/2026-05-21-tiramisu-design.md`

---

## File Map

### Created (new)
- `app/src/main/java/com/bottazzini/tiramisu/utils/Difficulty.kt`
- `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuMoveValidator.kt`
- `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuDeckSetup.kt`
- `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuGameState.kt`
- `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt`
- `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialSteps.kt`
- `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialEngine.kt`
- `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt`
- `app/src/main/res/layout/game.xml` *(replaced)*
- `app/src/test/java/com/bottazzini/tiramisu/TiramisuMoveValidatorTest.kt`
- `app/src/test/java/com/bottazzini/tiramisu/TiramisuDeckSetupTest.kt`
- `app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt`
- `app/src/test/java/com/bottazzini/tiramisu/TiramisuSolverTest.kt`

### Modified (from Trasloco fork)
- `app/build.gradle` — applicationId, namespace, versionCode/Name
- `settings.gradle` — rootProject.name
- `app/src/main/AndroidManifest.xml` — package refs
- `app/src/main/java/com/bottazzini/tiramisu/db/DatabaseHandler.kt` — DB name, version, new columns
- `app/src/main/java/com/bottazzini/tiramisu/db/columns/GameLogColumns.kt` — add DIFFICULTY, REDEALS_USED
- `app/src/main/java/com/bottazzini/tiramisu/settings/GameLog.kt` — add difficulty, redealsUsed
- `app/src/main/java/com/bottazzini/tiramisu/settings/GameLogRepository.kt` — insert/query difficulty
- `app/src/main/java/com/bottazzini/tiramisu/settings/GameStateRepository.kt` — rewrite for Tiramisu state
- `app/src/main/java/com/bottazzini/tiramisu/settings/SettingsHandler.kt` — add DIFFICULTY config
- `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt` — full rewrite
- `app/src/main/java/com/bottazzini/tiramisu/GameViewModel.kt` — replaced by TiramisuViewModel
- `app/src/main/java/com/bottazzini/tiramisu/SettingsActivity.kt` — add difficulty row
- `app/src/main/java/com/bottazzini/tiramisu/StatsActivity.kt` — per-difficulty stats
- `app/src/main/java/com/bottazzini/tiramisu/MainActivity.kt` — tutorial prompt for Tiramisu
- `app/src/main/res/layout/settings.xml` — add difficulty row
- `app/src/main/res/values/strings.xml` — Tiramisu strings

### Deleted (Trasloco-specific)
- `utils/CardMoveValidator.kt`
- `utils/TraslocoSolver.kt`
- `utils/DeckSetup.kt`
- `utils/HintEngine.kt`
- `utils/TutorialEngine.kt`
- `utils/TutorialSteps.kt`

---

## Task 1: Fork Trasloco → Tiramisu

**Files:**
- Create: entire project from Trasloco copy
- Modify: `app/build.gradle`, `settings.gradle`, `AndroidManifest.xml`, all `.kt` files (package rename)

- [ ] **Step 1: Copy the project**

```bash
cp -rn /Users/bottazzini/Documents/Progetti/Trasloco/. /Users/bottazzini/Documents/Progetti/Tiramisu/
# Remove Trasloco's git history (fresh start)
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/.git
# Remove Trasloco's compiled artifacts and release builds
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/app/build
rm -f  /Users/bottazzini/Documents/Progetti/Tiramisu/app/release/app-release.aab
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/.gradle
# Remove Trasloco-specific docs (Tiramisu already has its own in docs/)
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/implementation
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/plans/2026-05-11-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/plans/2026-05-12-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/plans/2026-05-13-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/plans/2026-05-15-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/plans/2026-05-19-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/plans/2026-05-20-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/specs/2026-05-11-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/specs/2026-05-12-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/specs/2026-05-13-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/specs/2026-05-15-*.md
rm -rf /Users/bottazzini/Documents/Progetti/Tiramisu/docs/superpowers/specs/2026-05-20-*.md
```

- [ ] **Step 2: Rename the source directory**

```bash
BASE=/Users/bottazzini/Documents/Progetti/Tiramisu/app/src

mv "$BASE/main/java/com/bottazzini/trasloco"      "$BASE/main/java/com/bottazzini/tiramisu"
mv "$BASE/androidTest/java/com/bottazzini/trasloco" "$BASE/androidTest/java/com/bottazzini/tiramisu"

# Create unit test dir (Trasloco didn't use src/test/)
mkdir -p "$BASE/test/java/com/bottazzini/tiramisu"
```

- [ ] **Step 3: Replace all package references**

```bash
TIRAMISU=/Users/bottazzini/Documents/Progetti/Tiramisu

# Kotlin sources
find "$TIRAMISU/app/src" -name "*.kt" \
  -exec sed -i '' 's/com\.bottazzini\.trasloco/com.bottazzini.tiramisu/g' {} \;

# Gradle and manifest
sed -i '' 's/com\.bottazzini\.trasloco/com.bottazzini.tiramisu/g' \
  "$TIRAMISU/app/build.gradle" \
  "$TIRAMISU/app/src/main/AndroidManifest.xml"

# XML resources
find "$TIRAMISU/app/src/main/res" -name "*.xml" \
  -exec sed -i '' 's/com\.bottazzini\.trasloco/com.bottazzini.tiramisu/g' {} \;

# settings.gradle
sed -i '' "s/rootProject.name = 'Trasloco'/rootProject.name = 'Tiramisu'/" \
  "$TIRAMISU/settings.gradle"
```

- [ ] **Step 4: Reset version and rename app in `app/build.gradle`**

Open `app/build.gradle`. Change these lines:

```groovy
defaultConfig {
    applicationId = "com.bottazzini.tiramisu"
    minSdk = 24
    targetSdk = 36
    versionCode 1
    versionName "1.0.0"
    testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
}
// also update namespace
namespace 'com.bottazzini.tiramisu'
```

- [ ] **Step 5: Update app name in `app/src/main/res/values/strings.xml`**

Change the three name strings at the top:

```xml
<string name="app_name" translatable="false">Tiramisù</string>
<string name="title" translatable="false">Tiramisù</string>
<string name="trasloco" translatable="false">Tiramisù</string>
```

- [ ] **Step 6: Rename DB in `app/src/main/java/com/bottazzini/tiramisu/db/DatabaseHandler.kt`**

```kotlin
private const val DATABASE_NAME = "Tiramisu.db"
private const val DATABASE_VERSION = 1
```

Also update `onUpgrade` — since this is a new app there are no migrations yet:

```kotlin
override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    Log.w(TAG, "onUpgrade: from $oldVersion to $newVersion")
    // v1 → no migrations needed yet
}
```

- [ ] **Step 7: Verify the project builds**

Open Android Studio (or run from command line):

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. Fix any remaining `trasloco` references if compilation fails.

---

## Task 2: Delete Trasloco game classes

**Files:**
- Delete: `utils/CardMoveValidator.kt`, `utils/TraslocoSolver.kt`, `utils/DeckSetup.kt`, `utils/HintEngine.kt`, `utils/TutorialEngine.kt`, `utils/TutorialSteps.kt`
- Modify: `GameActivity.kt`, `GameViewModel.kt` — remove references so project still compiles

- [ ] **Step 1: Delete the six files**

```bash
BASE=/Users/bottazzini/Documents/Progetti/Tiramisu/app/src/main/java/com/bottazzini/tiramisu/utils
rm "$BASE/CardMoveValidator.kt"
rm "$BASE/TraslocoSolver.kt"
rm "$BASE/DeckSetup.kt"
rm "$BASE/HintEngine.kt"
rm "$BASE/TutorialEngine.kt"
rm "$BASE/TutorialSteps.kt"
```

- [ ] **Step 2: Stub out `GameActivity.kt` temporarily**

Replace the entire contents of `GameActivity.kt` with a minimal stub so the project compiles while we build the real implementation:

```kotlin
package com.bottazzini.tiramisu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Temporary stub — will be fully implemented in Task 12. */
class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TUTORIAL_MODE = "tutorial_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game)
    }
}
```

- [ ] **Step 3: Stub `GameViewModel.kt`**

```kotlin
package com.bottazzini.tiramisu

import androidx.lifecycle.ViewModel

/** Replaced by TiramisuViewModel in Task 7. Kept for compilation. */
class GameViewModel : ViewModel()
```

- [ ] **Step 4: Verify the project compiles**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 3: Difficulty enum + SettingsHandler

**Files:**
- Create: `app/src/main/java/com/bottazzini/tiramisu/utils/Difficulty.kt`
- Modify: `settings/SettingsHandler.kt`

- [ ] **Step 1: Create `Difficulty.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/utils/Difficulty.kt
package com.bottazzini.tiramisu.utils

enum class Difficulty(
    val key: String,
    val displayName: String,
    val redeals: Int,
    val obbligato: Boolean
) {
    FACILE("facile", "🟢 Facile", redeals = 2, obbligato = false),
    NORMALE("normale", "🟡 Normale", redeals = 1, obbligato = false),
    DIFFICILE("difficile", "🔴 Difficile", redeals = 1, obbligato = true);

    companion object {
        fun fromKey(key: String): Difficulty =
            entries.firstOrNull { it.key == key } ?: NORMALE
    }
}
```

- [ ] **Step 2: Add `DIFFICULTY` to `Configuration` enum in `SettingsHandler.kt`**

Open `app/src/main/java/com/bottazzini/tiramisu/settings/SettingsHandler.kt`.

Change the `Configuration` enum to add DIFFICULTY:

```kotlin
enum class Configuration(val value: String) {
    FAST_DEAL("fastDeal"),
    CARD_BACK("cardBack"),
    BACKGROUND("background"),
    CARD_TYPE("cardType"),
    HINT_ENABLED("hintEnabled"),
    AUTO_MOVE("autoMove"),
    DIFFICULTY("difficulty")
}
```

- [ ] **Step 3: Add default for DIFFICULTY in `insertDefaultSettings()`**

```kotlin
fun insertDefaultSettings() {
    setDefaultSetting(Configuration.FAST_DEAL.value, "enabled")
    setDefaultSetting(Configuration.CARD_BACK.value, "bg2")
    setDefaultSetting(Configuration.BACKGROUND.value, "bordeaux")
    setDefaultSetting(Configuration.CARD_TYPE.value, "piacentine")
    setDefaultSetting(Configuration.HINT_ENABLED.value, "enabled")
    setDefaultSetting(Configuration.AUTO_MOVE.value, "disabled")
    setDefaultSetting(Configuration.DIFFICULTY.value, Difficulty.NORMALE.key)
}
```

- [ ] **Step 4: Verify build**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 4: SettingsActivity — difficulty row

**Files:**
- Modify: `app/src/main/res/layout/settings.xml`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/SettingsActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add difficulty strings to `strings.xml`**

```xml
<string name="settings_label_difficulty">Difficulty</string>
<string name="difficulty_facile">🟢 Facile (2 redeals)</string>
<string name="difficulty_normale">🟡 Normale (1 redeal)</string>
<string name="difficulty_difficile">🔴 Difficile (obbligato)</string>
```

- [ ] **Step 2: Add difficulty row to `settings.xml`**

Insert after the `autoMoveRow` LinearLayout and before the Credits TextView:

```xml
<!-- Difficulty section -->
<TextView
    android:id="@+id/labelDifficulty"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="24dp"
    android:text="@string/settings_label_difficulty"
    style="@style/CasinoSectionLabel"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toBottomOf="@id/autoMoveRow" />

<RadioGroup
    android:id="@+id/difficultyRadioGroup"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:orientation="vertical"
    android:background="@drawable/casino_tile_bg"
    android:padding="12dp"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toBottomOf="@id/labelDifficulty">

    <RadioButton
        android:id="@+id/radioDiffFacile"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:text="@string/difficulty_facile"
        android:textSize="16sp"
        android:gravity="center_vertical"
        android:paddingStart="8dp" />

    <RadioButton
        android:id="@+id/radioDiffNormale"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:text="@string/difficulty_normale"
        android:textSize="16sp"
        android:gravity="center_vertical"
        android:paddingStart="8dp" />

    <RadioButton
        android:id="@+id/radioDiffDifficile"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:text="@string/difficulty_difficile"
        android:textSize="16sp"
        android:gravity="center_vertical"
        android:paddingStart="8dp" />
</RadioGroup>
```

Also update the Credits constraint to point to `difficultyRadioGroup`:
```xml
app:layout_constraintTop_toBottomOf="@id/difficultyRadioGroup"
```

- [ ] **Step 3: Wire up difficulty radio in `SettingsActivity.kt`**

Add these fields and methods. Find the `onResume` section and add alongside the existing settings wiring:

```kotlin
// Add to field declarations:
private lateinit var difficultyRadioGroup: RadioGroup

// Add to onCreate() after settingsHandler initialization:
difficultyRadioGroup = findViewById(R.id.difficultyRadioGroup)
loadDifficultySetting()
difficultyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
    val key = when (checkedId) {
        R.id.radioDiffFacile    -> Difficulty.FACILE.key
        R.id.radioDiffNormale   -> Difficulty.NORMALE.key
        R.id.radioDiffDifficile -> Difficulty.DIFFICILE.key
        else                    -> Difficulty.NORMALE.key
    }
    settingsHandler.updateSetting(Configuration.DIFFICULTY.value, key)
}

// New private method:
private fun loadDifficultySetting() {
    val key = settingsHandler.readValue(Configuration.DIFFICULTY.value) ?: Difficulty.NORMALE.key
    val id = when (key) {
        Difficulty.FACILE.key    -> R.id.radioDiffFacile
        Difficulty.DIFFICILE.key -> R.id.radioDiffDifficile
        else                     -> R.id.radioDiffNormale
    }
    difficultyRadioGroup.check(id)
}
```

Add import at top of SettingsActivity.kt:
```kotlin
import com.bottazzini.tiramisu.utils.Difficulty
```

- [ ] **Step 4: Verify build**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 5: TiramisuDeckSetup (with unit tests)

**Files:**
- Create: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuDeckSetup.kt`
- Create: `app/src/test/java/com/bottazzini/tiramisu/TiramisuDeckSetupTest.kt`

- [ ] **Step 1: Write the failing tests first**

```kotlin
// app/src/test/java/com/bottazzini/tiramisu/TiramisuDeckSetupTest.kt
package com.bottazzini.tiramisu

import com.bottazzini.tiramisu.utils.TiramisuDeckSetup
import org.junit.Assert.*
import org.junit.Test

class TiramisuDeckSetupTest {

    @Test
    fun `shuffled deck has exactly 40 cards`() {
        assertEquals(40, TiramisuDeckSetup.shuffledDeck().size)
    }

    @Test
    fun `shuffled deck contains all 40 unique cards`() {
        val deck = TiramisuDeckSetup.shuffledDeck()
        assertEquals(40, deck.toSet().size)
        val suits = listOf("b", "c", "d", "s")
        for (suit in suits) {
            for (rank in 1..10) {
                assertTrue("$suit$rank missing", deck.contains("$suit$rank"))
            }
        }
    }

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
        assertEquals("positions 4 and 5 must share suit for tutorial", card4[0], card5[0])
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew test --tests "*.TiramisuDeckSetupTest"
```

Expected: compilation error (`TiramisuDeckSetup` doesn't exist).

- [ ] **Step 3: Implement `TiramisuDeckSetup.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuDeckSetup.kt
package com.bottazzini.tiramisu.utils

object TiramisuDeckSetup {

    private val SUITS = listOf("b", "c", "d", "s")

    /** Returns a new ordered deck of 40 cards. */
    fun orderedDeck(): List<String> =
        SUITS.flatMap { suit -> (1..10).map { rank -> "$suit$rank" } }

    /** Returns a new shuffled deck of 40 cards (random order). */
    fun shuffledDeck(): List<String> = orderedDeck().shuffled()

    /**
     * Deterministic deck for the tutorial.
     *
     * First deal (indices 0-3):  b1, c5, c3, d8
     *   → b1 (Asso di Bastoni) auto-goes to foundation
     *   → c5, c3 land in piles 1 and 2 — both coppe → same-suit move available
     *
     * Second deal (indices 4-7): c7, b5, d3, s4
     *   → c7 lands on pile 0 (empty after b1 was taken), c3 still on pile 2
     *   → c7→c3 move not valid (c7 > c3 but same suit → valid!)
     */
    fun tutorialDeck(): List<String> = listOf(
        // First 4 → initial deal to piles 0-3
        "b1", "c5", "c3", "d8",
        // Second 4 → second deal
        "c7", "b5", "d3", "s4",
        // Remaining 32 cards (no special ordering needed)
        "b2", "b3", "b4", "b6", "b7", "b8", "b9", "b10",
        "c1", "c2", "c4", "c6", "c8", "c9", "c10",
        "d1", "d2", "d4", "d5", "d6", "d7", "d9", "d10",
        "s1", "s2", "s3", "s5", "s6", "s7", "s8", "s9", "s10"
    )
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew test --tests "*.TiramisuDeckSetupTest"
```

Expected: `BUILD SUCCESSFUL`, all tests PASS.

---

## Task 6: TiramisuMoveValidator (with unit tests)

**Files:**
- Create: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuMoveValidator.kt`
- Create: `app/src/test/java/com/bottazzini/tiramisu/TiramisuMoveValidatorTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// app/src/test/java/com/bottazzini/tiramisu/TiramisuMoveValidatorTest.kt
package com.bottazzini.tiramisu

import com.bottazzini.tiramisu.utils.TiramisuMoveValidator
import org.junit.Assert.*
import org.junit.Test

class TiramisuMoveValidatorTest {

    // --- suit() and rank() helpers ---

    @Test fun `suit extracts single character`() {
        assertEquals("c", TiramisuMoveValidator.suit("c5"))
        assertEquals("b", TiramisuMoveValidator.suit("b10"))
    }

    @Test fun `rank extracts integer correctly for two-digit rank`() {
        assertEquals(10, TiramisuMoveValidator.rank("c10"))
        assertEquals(1, TiramisuMoveValidator.rank("b1"))
    }

    // --- canMoveToTableau ---

    @Test fun `same suit any rank allows tableau move`() {
        assertTrue(TiramisuMoveValidator.canMoveToTableau("c5", "c3"))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("b1", "b10"))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("s7", "s2"))
    }

    @Test fun `different suit blocks tableau move`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("c5", "b3"))
        assertFalse(TiramisuMoveValidator.canMoveToTableau("d1", "s1"))
    }

    @Test fun `empty pile (zero) accepts any card`() {
        assertTrue(TiramisuMoveValidator.canMoveToTableau("s7", "zero"))
        assertTrue(TiramisuMoveValidator.canMoveToTableau("b1", "zero"))
    }

    @Test fun `zero as moving card is rejected`() {
        assertFalse(TiramisuMoveValidator.canMoveToTableau("zero", "c5"))
        assertFalse(TiramisuMoveValidator.canMoveToTableau("zero", "zero"))
    }

    // --- canMoveToFoundation ---

    @Test fun `ace goes to empty foundation`() {
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("b1", "zero"))
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("s1", "zero"))
    }

    @Test fun `non-ace blocked from empty foundation`() {
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("b2", "zero"))
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("c5", "zero"))
    }

    @Test fun `ascending same suit allowed in foundation`() {
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("c2", "c1"))
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("b10", "b9"))
        assertTrue(TiramisuMoveValidator.canMoveToFoundation("d5", "d4"))
    }

    @Test fun `skipping ranks blocked in foundation`() {
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("c3", "c1"))
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("b5", "b3"))
    }

    @Test fun `different suit blocked in foundation`() {
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("b2", "c1"))
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("d3", "s2"))
    }

    @Test fun `zero moving card rejected in foundation`() {
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("zero", "c1"))
        assertFalse(TiramisuMoveValidator.canMoveToFoundation("zero", "zero"))
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure**

```bash
./gradlew test --tests "*.TiramisuMoveValidatorTest"
```

Expected: compilation error.

- [ ] **Step 3: Implement `TiramisuMoveValidator.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuMoveValidator.kt
package com.bottazzini.tiramisu.utils

/**
 * Pure validation logic for Tiramisù.
 * Cards: "<suit><rank>" e.g. "c5", "b1", "d10", "s3".
 * "zero" = empty slot.
 * Suits: b=bastoni, c=coppe, d=denari, s=spade.
 */
object TiramisuMoveValidator {

    fun suit(card: String): String = card.substring(0, 1)
    fun rank(card: String): Int    = card.substring(1).toInt()

    /**
     * Can [movingCard] be placed on top of [destinationTop] in the tableau?
     * Rule: same suit, any rank. Empty pile (zero) accepts anything.
     */
    fun canMoveToTableau(movingCard: String, destinationTop: String): Boolean {
        if (movingCard == "zero") return false
        if (destinationTop == "zero") return true  // empty pile accepts any card
        return suit(movingCard) == suit(destinationTop)
    }

    /**
     * Can [movingCard] be placed on [foundationTop]?
     * Rule: same suit, rank = foundationTop.rank + 1.
     * Empty foundation (zero) accepts only Aces (rank 1).
     */
    fun canMoveToFoundation(movingCard: String, foundationTop: String): Boolean {
        if (movingCard == "zero") return false
        val movingRank = rank(movingCard)
        if (foundationTop == "zero") return movingRank == 1
        return suit(movingCard) == suit(foundationTop) &&
               movingRank == rank(foundationTop) + 1
    }
}
```

- [ ] **Step 4: Run tests — expect all PASS**

```bash
./gradlew test --tests "*.TiramisuMoveValidatorTest"
```

Expected: all tests PASS.

---

## Task 7: TiramisuGameState + TiramisuViewModel (with unit tests)

**Files:**
- Create: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuGameState.kt`
- Create: `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt`
- Create: `app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt`

- [ ] **Step 1: Create `TiramisuGameState.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuGameState.kt
package com.bottazzini.tiramisu.utils

/**
 * Full mutable game state for Tiramisù.
 *
 * piles:       4 card stacks. Last element = top card. Empty pile top = "zero".
 * stock:       Remaining cards in tallone. Index 0 = next card to be dealt.
 * foundations: 4 foundation tops. "zero" = empty foundation.
 * redealsLeft: How many redistributions remain.
 */
class TiramisuGameState(
    val piles:       List<MutableList<String>>,  // indices 0-3
    val stock:       MutableList<String>,
    val foundations: MutableList<String>,        // indices 0-3, "zero" if empty
    var redealsLeft: Int,
    val difficulty:  Difficulty,
    var gameStartTimeMillis: Long   = 0L,
    var timerPausedMs:       Long   = 0L,
    var isTimerPaused:       Boolean = false,
    var hasActiveGame:       Boolean = false
) {
    /** Top card of pile [idx], or "zero" if empty. */
    fun topOfPile(idx: Int): String = piles[idx].lastOrNull() ?: "zero"

    /** True when all 4 foundations are complete (top card = rank 10). */
    fun isWon(): Boolean = foundations.all { top ->
        top != "zero" && TiramisuMoveValidator.rank(top) == 10
    }

    companion object {
        /** Create a fresh game state with a shuffled stock. */
        fun newGame(difficulty: Difficulty): TiramisuGameState = TiramisuGameState(
            piles       = List(4) { mutableListOf() },
            stock       = TiramisuDeckSetup.shuffledDeck().toMutableList(),
            foundations = MutableList(4) { "zero" },
            redealsLeft = difficulty.redeals,
            difficulty  = difficulty,
            hasActiveGame = true
        )

        /** Create a fresh game state with the tutorial deck. */
        fun tutorialGame(difficulty: Difficulty = Difficulty.FACILE): TiramisuGameState =
            TiramisuGameState(
                piles       = List(4) { mutableListOf() },
                stock       = TiramisuDeckSetup.tutorialDeck().toMutableList(),
                foundations = MutableList(4) { "zero" },
                redealsLeft = difficulty.redeals,
                difficulty  = difficulty,
                hasActiveGame = true
            )
    }
}
```

- [ ] **Step 2: Create `TiramisuViewModel.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt
package com.bottazzini.tiramisu

import androidx.lifecycle.ViewModel
import com.bottazzini.tiramisu.utils.Difficulty
import com.bottazzini.tiramisu.utils.TiramisuGameState
import com.bottazzini.tiramisu.utils.TiramisuMoveValidator

class TiramisuViewModel : ViewModel() {

    var state: TiramisuGameState? = null
        private set

    /** Index (0-3) of the currently selected pile, or null if nothing selected. */
    var selectedPileIndex: Int? = null
        private set

    // ---- Game lifecycle ----

    fun newGame(difficulty: Difficulty) {
        state = TiramisuGameState.newGame(difficulty)
        selectedPileIndex = null
    }

    fun newTutorialGame(difficulty: Difficulty = Difficulty.FACILE) {
        state = TiramisuGameState.tutorialGame(difficulty)
        selectedPileIndex = null
    }

    fun restoreState(restored: TiramisuGameState) {
        state = restored
        selectedPileIndex = null
    }

    // ---- Tallone interactions ----

    /**
     * Deal one card from the stock to each pile (left to right).
     * Returns true if at least one card was dealt.
     * After dealing, auto-moves any new Aces to foundations.
     */
    fun dealFromStock(): Boolean {
        val s = state ?: return false
        if (s.stock.isEmpty()) return false
        val toDeal = minOf(4, s.stock.size)
        for (i in 0 until toDeal) {
            s.piles[i].add(s.stock.removeAt(0))
        }
        autoMoveAces()
        selectedPileIndex = null
        return true
    }

    /**
     * Redistribute: collect piles 3→2→1→0 into new stock.
     * Returns true if redeal was performed.
     */
    fun redeal(): Boolean {
        val s = state ?: return false
        if (s.redealsLeft <= 0 || s.stock.isNotEmpty()) return false
        val newStock = mutableListOf<String>()
        for (i in 3 downTo 0) {
            newStock.addAll(s.piles[i])
            s.piles[i].clear()
        }
        s.stock.addAll(newStock)
        s.redealsLeft--
        selectedPileIndex = null
        return true
    }

    /** True when the stock is empty and a redeal is still available. */
    fun canRedeal(): Boolean {
        val s = state ?: return false
        return s.stock.isEmpty() && s.redealsLeft > 0
    }

    // ---- Pile interactions ----

    /**
     * Called when the player taps pile [pileIdx].
     * - If nothing is selected: select this pile (if it has a card).
     * - If this pile is already selected: deselect.
     * - If another pile is selected: try to move top card → this pile.
     *
     * Returns the outcome.
     */
    fun onPileTapped(pileIdx: Int): TapResult {
        val s = state ?: return TapResult.NOTHING
        val selected = selectedPileIndex

        return when {
            selected == null -> {
                if (s.topOfPile(pileIdx) == "zero") TapResult.NOTHING
                else { selectedPileIndex = pileIdx; TapResult.SELECTED }
            }
            selected == pileIdx -> {
                selectedPileIndex = null
                TapResult.DESELECTED
            }
            else -> {
                val moved = movePileToPile(selected, pileIdx)
                selectedPileIndex = null
                if (moved) TapResult.MOVED else TapResult.INVALID
            }
        }
    }

    /**
     * Attempt to move the top card of pile [pileIdx] to a matching foundation.
     * Returns true if the move was made.
     */
    fun onFoundationTapped(pileIdx: Int): Boolean {
        val s = state ?: return false
        val moving = s.topOfPile(pileIdx)
        if (moving == "zero") return false
        for (fIdx in 0..3) {
            if (TiramisuMoveValidator.canMoveToFoundation(moving, s.foundations[fIdx])) {
                s.piles[pileIdx].removeLast()
                s.foundations[fIdx] = moving
                selectedPileIndex = null
                autoMoveAces()
                return true
            }
        }
        return false
    }

    /**
     * Attempt to move selected card directly to the right foundation slot.
     * Returns true if moved.
     */
    fun onFoundationAreaTapped(): Boolean {
        val sel = selectedPileIndex ?: return false
        val moved = onFoundationTapped(sel)
        if (moved) selectedPileIndex = null
        return moved
    }

    // ---- Obbligato ----

    /**
     * In DIFFICILE mode: returns the pile indices whose top card MUST go to
     * a foundation before any other move is made.
     * Empty list means no obligation (or not in DIFFICILE mode).
     */
    fun obbligatoTargets(): List<Int> {
        val s = state ?: return emptyList()
        if (!s.difficulty.obbligato) return emptyList()
        return (0..3).filter { pileIdx ->
            val card = s.topOfPile(pileIdx)
            card != "zero" && canGoToAnyFoundation(card)
        }
    }

    /** True if the obbligato constraint is currently blocking moves. */
    fun isObbligatoBlocking(): Boolean = obbligatoTargets().isNotEmpty()

    // ---- Win check ----

    fun isWon(): Boolean = state?.isWon() ?: false

    // ---- Private helpers ----

    private fun movePileToPile(srcIdx: Int, dstIdx: Int): Boolean {
        val s = state ?: return false
        val moving = s.topOfPile(srcIdx)
        val dest   = s.topOfPile(dstIdx)
        if (!TiramisuMoveValidator.canMoveToTableau(moving, dest)) return false
        // In DIFFICILE, block non-foundation moves when obbligato targets exist
        if (s.difficulty.obbligato && obbligatoTargets().isNotEmpty()) return false
        s.piles[dstIdx].add(s.piles[srcIdx].removeLast())
        autoMoveAces()
        return true
    }

    private fun canGoToAnyFoundation(card: String): Boolean {
        val s = state ?: return false
        return s.foundations.any { top ->
            TiramisuMoveValidator.canMoveToFoundation(card, top)
        }
    }

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
                            s.piles[pileIdx].removeLast()
                            s.foundations[fIdx] = card
                            moved = true
                            break
                        }
                    }
                }
            }
        }
    }
}

enum class TapResult { SELECTED, DESELECTED, MOVED, INVALID, NOTHING }
```

- [ ] **Step 3: Write tests for `TiramisuViewModel.kt`**

```kotlin
// app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt
package com.bottazzini.tiramisu

import com.bottazzini.tiramisu.utils.Difficulty
import com.bottazzini.tiramisu.utils.TiramisuGameState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TiramisuViewModelTest {

    private lateinit var vm: TiramisuViewModel

    @Before
    fun setup() {
        vm = TiramisuViewModel()
    }

    private fun stateWith(
        piles: List<List<String>>,
        stock: List<String> = emptyList(),
        foundations: List<String> = listOf("zero", "zero", "zero", "zero"),
        redeals: Int = 1,
        difficulty: Difficulty = Difficulty.NORMALE
    ): TiramisuGameState {
        val gs = TiramisuGameState(
            piles       = piles.map { it.toMutableList() },
            stock       = stock.toMutableList(),
            foundations = foundations.toMutableList(),
            redealsLeft = redeals,
            difficulty  = difficulty,
            hasActiveGame = true
        )
        vm.restoreState(gs)
        return gs
    }

    // ---- dealFromStock ----

    @Test fun `dealFromStock deals one card per pile`() {
        stateWith(
            piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            stock = listOf("b3", "c5", "d7", "s2")
        )
        val dealt = vm.dealFromStock()
        assertTrue(dealt)
        val s = vm.state!!
        assertEquals("b3", s.topOfPile(0))
        assertEquals("c5", s.topOfPile(1))
        assertEquals("d7", s.topOfPile(2))
        assertEquals("s2", s.topOfPile(3))
        assertTrue(s.stock.isEmpty())
    }

    @Test fun `dealFromStock returns false when stock is empty`() {
        stateWith(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()))
        assertFalse(vm.dealFromStock())
    }

    @Test fun `dealFromStock auto-moves ace to foundation`() {
        stateWith(
            piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
            stock = listOf("b1", "c5", "d7", "s2")
        )
        vm.dealFromStock()
        // b1 should have auto-moved to a foundation
        val s = vm.state!!
        assertTrue("ace not auto-moved", s.foundations.any { it == "b1" })
        assertEquals("zero", s.topOfPile(0)) // pile 0 empty after ace removed
    }

    // ---- onPileTapped ----

    @Test fun `tapping pile with card selects it`() {
        stateWith(piles = listOf(listOf("c5"), emptyList(), emptyList(), emptyList()))
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.SELECTED, result)
        assertEquals(0, vm.selectedPileIndex)
    }

    @Test fun `tapping empty pile does nothing`() {
        stateWith(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()))
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.NOTHING, result)
        assertNull(vm.selectedPileIndex)
    }

    @Test fun `tapping selected pile deselects`() {
        stateWith(piles = listOf(listOf("c5"), emptyList(), emptyList(), emptyList()))
        vm.onPileTapped(0)
        val result = vm.onPileTapped(0)
        assertEquals(TapResult.DESELECTED, result)
        assertNull(vm.selectedPileIndex)
    }

    @Test fun `moving same-suit card succeeds`() {
        stateWith(piles = listOf(listOf("c5"), listOf("c3"), emptyList(), emptyList()))
        vm.onPileTapped(0) // select pile 0 (c5)
        val result = vm.onPileTapped(1) // move to pile 1 (c3)
        assertEquals(TapResult.MOVED, result)
        val s = vm.state!!
        assertEquals("c5", s.topOfPile(1))
        assertEquals("zero", s.topOfPile(0))
    }

    @Test fun `moving different-suit card is invalid`() {
        stateWith(piles = listOf(listOf("c5"), listOf("b3"), emptyList(), emptyList()))
        vm.onPileTapped(0)
        val result = vm.onPileTapped(1)
        assertEquals(TapResult.INVALID, result)
        val s = vm.state!!
        assertEquals("c5", s.topOfPile(0)) // card stays
        assertEquals("b3", s.topOfPile(1))
    }

    // ---- redeal ----

    @Test fun `redeal moves piles right-to-left into stock`() {
        stateWith(
            piles = listOf(listOf("b2"), listOf("c5"), listOf("d7"), listOf("s9")),
            stock = emptyList(),
            redeals = 1
        )
        val ok = vm.redeal()
        assertTrue(ok)
        val s = vm.state!!
        // Stock = pile3 + pile2 + pile1 + pile0 = s9, d7, c5, b2
        assertEquals(listOf("s9", "d7", "c5", "b2"), s.stock.toList())
        assertEquals(0, s.redealsLeft)
        assertTrue(s.piles.all { it.isEmpty() })
    }

    @Test fun `redeal fails when stock is not empty`() {
        stateWith(
            piles = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
            stock = listOf("c5"),
            redeals = 1
        )
        assertFalse(vm.redeal())
    }

    @Test fun `redeal fails when redealsLeft is 0`() {
        stateWith(
            piles = listOf(listOf("b2"), emptyList(), emptyList(), emptyList()),
            stock = emptyList(),
            redeals = 0
        )
        assertFalse(vm.redeal())
    }

    // ---- obbligato ----

    @Test fun `obbligato blocks pile-to-pile when foundation move available`() {
        stateWith(
            piles = listOf(listOf("b2"), listOf("b3"), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero"),
            difficulty = Difficulty.DIFFICILE
        )
        // b2 can go to foundation (b1+1=b2), so obbligato blocks other moves
        vm.onPileTapped(1) // try to select b3
        val result = vm.onPileTapped(0) // try to move b3 onto b2
        // Should be blocked because b2 must go to foundation first
        assertEquals(TapResult.INVALID, result)
    }

    @Test fun `obbligato not active in NORMALE mode`() {
        stateWith(
            piles = listOf(listOf("b2"), listOf("b3"), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero"),
            difficulty = Difficulty.NORMALE
        )
        vm.onPileTapped(1)
        val result = vm.onPileTapped(0)
        // Normal mode: b3→b2 is valid (same suit)
        assertEquals(TapResult.MOVED, result)
    }

    // ---- win detection ----

    @Test fun `isWon returns true when all foundations complete`() {
        val wonFoundations = listOf("b10", "c10", "d10", "s10")
        stateWith(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
                  foundations = wonFoundations)
        assertTrue(vm.isWon())
    }

    @Test fun `isWon returns false when foundations incomplete`() {
        stateWith(piles = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
                  foundations = listOf("b10", "c9", "zero", "s10"))
        assertFalse(vm.isWon())
    }
}
```

- [ ] **Step 4: Run tests — expect compilation failure**

```bash
./gradlew test --tests "*.TiramisuViewModelTest"
```

Expected: compilation error (TiramisuViewModel not yet created for JVM).

> **Note:** `TiramisuViewModel` extends `ViewModel` from AndroidX, which requires Android. For unit tests, the class is tested as a plain Kotlin object because `ViewModel` has no Android-specific logic that affects these tests. If you get `androidx.lifecycle.ViewModel` issues, add `testImplementation 'androidx.arch.core:core-testing:2.2.0'` to `build.gradle`.

- [ ] **Step 5: Run tests — expect PASS**

```bash
./gradlew test --tests "*.TiramisuViewModelTest"
```

Expected: all tests PASS.

---

## Task 8: TiramisuSolver (hint engine)

**Files:**
- Create: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt`
- Create: `app/src/test/java/com/bottazzini/tiramisu/TiramisuSolverTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// app/src/test/java/com/bottazzini/tiramisu/TiramisuSolverTest.kt
package com.bottazzini.tiramisu

import com.bottazzini.tiramisu.utils.Difficulty
import com.bottazzini.tiramisu.utils.TiramisuGameState
import com.bottazzini.tiramisu.utils.TiramisuSolver
import org.junit.Assert.*
import org.junit.Test

class TiramisuSolverTest {

    private fun state(
        piles: List<List<String>>,
        foundations: List<String> = listOf("zero","zero","zero","zero"),
        stock: List<String> = emptyList()
    ) = TiramisuGameState(
        piles       = piles.map { it.toMutableList() },
        stock       = stock.toMutableList(),
        foundations = foundations.toMutableList(),
        redealsLeft = 1,
        difficulty  = Difficulty.NORMALE
    )

    @Test fun `finds foundation hint when ace available`() {
        val s = state(piles = listOf(listOf("b1"), emptyList(), emptyList(), emptyList()))
        val hint = TiramisuSolver.findHint(s)
        assertNotNull(hint)
        assertEquals(0, hint!!.fromPile)
        assertTrue(hint.toFoundation)
    }

    @Test fun `finds pile-to-pile hint for same suit`() {
        val s = state(piles = listOf(listOf("c5"), listOf("c3"), emptyList(), emptyList()))
        val hint = TiramisuSolver.findHint(s)
        assertNotNull(hint)
        assertFalse(hint!!.toFoundation)
    }

    @Test fun `returns null when no moves available`() {
        val s = state(piles = listOf(listOf("b5"), listOf("c3"), listOf("d7"), listOf("s2")))
        // No same-suit pairs, no aces, no stock
        val hint = TiramisuSolver.findHint(s)
        assertNull(hint)
    }

    @Test fun `prefers foundation over tableau move`() {
        val s = state(
            piles       = listOf(listOf("b2"), listOf("b5"), emptyList(), emptyList()),
            foundations = listOf("b1", "zero", "zero", "zero")
        )
        val hint = TiramisuSolver.findHint(s)
        assertNotNull(hint)
        assertTrue(hint!!.toFoundation) // foundation preferred
    }
}
```

- [ ] **Step 2: Run tests — expect failure**

```bash
./gradlew test --tests "*.TiramisuSolverTest"
```

Expected: compilation error.

- [ ] **Step 3: Implement `TiramisuSolver.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt
package com.bottazzini.tiramisu.utils

/**
 * Finds the first available hint move in a Tiramisù game state.
 * Priority: foundation moves first, then tableau-to-tableau.
 */
object TiramisuSolver {

    data class Hint(
        val fromPile:    Int,
        val toPile:      Int?,    // null if toFoundation is true
        val toFoundation: Boolean
    )

    /** Returns the first available move, or null if none exists. */
    fun findHint(state: TiramisuGameState): Hint? {
        // 1. Foundation moves (highest priority)
        for (pileIdx in 0..3) {
            val card = state.topOfPile(pileIdx)
            if (card == "zero") continue
            for (fIdx in 0..3) {
                if (TiramisuMoveValidator.canMoveToFoundation(card, state.foundations[fIdx])) {
                    return Hint(fromPile = pileIdx, toPile = null, toFoundation = true)
                }
            }
        }

        // 2. Tableau-to-tableau moves
        for (srcIdx in 0..3) {
            val srcCard = state.topOfPile(srcIdx)
            if (srcCard == "zero") continue
            for (dstIdx in 0..3) {
                if (srcIdx == dstIdx) continue
                if (TiramisuMoveValidator.canMoveToTableau(srcCard, state.topOfPile(dstIdx))) {
                    // Skip moves that are just shuffling between empty piles (unhelpful)
                    if (state.topOfPile(dstIdx) == "zero" && state.piles[srcIdx].size == 1) continue
                    return Hint(fromPile = srcIdx, toPile = dstIdx, toFoundation = false)
                }
            }
        }

        return null
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
./gradlew test --tests "*.TiramisuSolverTest"
```

Expected: all tests PASS.

---

## Task 9: Database schema — GameLog with difficulty

**Files:**
- Modify: `db/columns/GameLogColumns.kt`
- Modify: `settings/GameLog.kt`
- Modify: `settings/GameLogRepository.kt`
- Modify: `db/DatabaseHandler.kt`

- [ ] **Step 1: Add columns to `GameLogColumns.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/db/columns/GameLogColumns.kt
package com.bottazzini.tiramisu.db.columns

import android.provider.BaseColumns

object GameLogColumns {
    object GameLogEntry : BaseColumns {
        const val TABLE_NAME          = "game_log"
        const val COLUMN_TIMESTAMP    = "timestamp"
        const val COLUMN_DURATION_MS  = "duration_ms"
        const val COLUMN_WON          = "won"
        const val COLUMN_HINTS_USED   = "hints_used"
        const val COLUMN_DIFFICULTY   = "difficulty"
        const val COLUMN_REDEALS_USED = "redeals_used"
    }
}
```

- [ ] **Step 2: Update `GameLog.kt` data class**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/settings/GameLog.kt
package com.bottazzini.tiramisu.settings

data class GameLog(
    val id:          Long   = 0,
    val timestamp:   Long,
    val durationMs:  Long,
    val won:         Boolean,
    val hintsUsed:   Int,
    val difficulty:  String,   // "facile" | "normale" | "difficile"
    val redealsUsed: Int
)
```

- [ ] **Step 3: Update `DatabaseHandler.kt`** — new CREATE statement

Update `SQL_CREATE_GAME_LOG` and bump version to 1 (fresh install, no migration needed):

```kotlin
private const val DATABASE_VERSION = 1
private const val DATABASE_NAME    = "Tiramisu.db"

private const val SQL_CREATE_GAME_LOG =
    "CREATE TABLE IF NOT EXISTS ${GameLogEntry.TABLE_NAME} (" +
    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "${GameLogEntry.COLUMN_TIMESTAMP}    INTEGER NOT NULL," +
    "${GameLogEntry.COLUMN_DURATION_MS}  INTEGER NOT NULL," +
    "${GameLogEntry.COLUMN_WON}          INTEGER NOT NULL," +
    "${GameLogEntry.COLUMN_HINTS_USED}   INTEGER NOT NULL," +
    "${GameLogEntry.COLUMN_DIFFICULTY}   TEXT    NOT NULL DEFAULT 'normale'," +
    "${GameLogEntry.COLUMN_REDEALS_USED} INTEGER NOT NULL DEFAULT 0)"
```

Remove the `COLUMN_AUTO_MOVES` reference that existed in Trasloco. The final `onCreate` should call `SQL_CREATE_GAME_LOG` (along with settings, records, achievements tables).

- [ ] **Step 4: Update `GameLogRepository.kt`**

Replace the entire file:

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/settings/GameLogRepository.kt
package com.bottazzini.tiramisu.settings

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import com.bottazzini.tiramisu.db.DatabaseHandler
import com.bottazzini.tiramisu.db.columns.GameLogColumns.GameLogEntry

class GameLogRepository(context: Context) {
    private val dbHandler = DatabaseHandler(context)

    fun insert(log: GameLog) {
        val db = dbHandler.writableDatabase
        val values = ContentValues().apply {
            put(GameLogEntry.COLUMN_TIMESTAMP,    log.timestamp)
            put(GameLogEntry.COLUMN_DURATION_MS,  log.durationMs)
            put(GameLogEntry.COLUMN_WON,          if (log.won) 1 else 0)
            put(GameLogEntry.COLUMN_HINTS_USED,   log.hintsUsed)
            put(GameLogEntry.COLUMN_DIFFICULTY,   log.difficulty)
            put(GameLogEntry.COLUMN_REDEALS_USED, log.redealsUsed)
        }
        db.insert(GameLogEntry.TABLE_NAME, null, values)
        trimIfNeeded(db)
    }

    fun countAll(): Long = rawCount("SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME}")
    fun countWins(): Long = rawCount(
        "SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1")

    fun countByDifficulty(difficulty: String): Long = rawCount(
        "SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_DIFFICULTY}='$difficulty'")
    fun countWinsByDifficulty(difficulty: String): Long = rawCount(
        "SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME} WHERE ${GameLogEntry.COLUMN_WON}=1 AND ${GameLogEntry.COLUMN_DIFFICULTY}='$difficulty'")

    fun bestWinTimeMs(difficulty: String? = null): Long? {
        val where = buildString {
            append("${GameLogEntry.COLUMN_WON}=1")
            if (difficulty != null) append(" AND ${GameLogEntry.COLUMN_DIFFICULTY}='$difficulty'")
        }
        val cursor = dbHandler.readableDatabase.rawQuery(
            "SELECT MIN(${GameLogEntry.COLUMN_DURATION_MS}) FROM ${GameLogEntry.TABLE_NAME} WHERE $where", null)
        val v = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        cursor.close()
        return v
    }

    fun avgWinDurationMs(difficulty: String? = null): Long? {
        val where = buildString {
            append("${GameLogEntry.COLUMN_WON}=1")
            if (difficulty != null) append(" AND ${GameLogEntry.COLUMN_DIFFICULTY}='$difficulty'")
        }
        val cursor = dbHandler.readableDatabase.rawQuery(
            "SELECT AVG(${GameLogEntry.COLUMN_DURATION_MS}) FROM ${GameLogEntry.TABLE_NAME} WHERE $where", null)
        val v = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        cursor.close()
        return v
    }

    fun close() { dbHandler.close() }

    private fun rawCount(sql: String): Long {
        val cursor = dbHandler.readableDatabase.rawQuery(sql, null)
        val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        cursor.close()
        return count
    }

    private fun trimIfNeeded(db: android.database.sqlite.SQLiteDatabase) {
        val count = rawCount("SELECT COUNT(*) FROM ${GameLogEntry.TABLE_NAME}")
        if (count > 500) {
            db.execSQL(
                "DELETE FROM ${GameLogEntry.TABLE_NAME} WHERE ${BaseColumns._ID} NOT IN " +
                "(SELECT ${BaseColumns._ID} FROM ${GameLogEntry.TABLE_NAME} ORDER BY ${BaseColumns._ID} DESC LIMIT 500)"
            )
        }
    }
}
```

- [ ] **Step 5: Verify build**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 10: GameStateRepository — save/restore Tiramisu state

**Files:**
- Modify: `settings/GameStateRepository.kt`

Replace the entire file with a Tiramisu-specific implementation:

- [ ] **Step 1: Rewrite `GameStateRepository.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/settings/GameStateRepository.kt
package com.bottazzini.tiramisu.settings

import android.content.Context
import com.bottazzini.tiramisu.utils.Difficulty
import com.bottazzini.tiramisu.utils.TiramisuGameState
import org.json.JSONArray
import org.json.JSONObject

class GameStateRepository(context: Context) {

    private val settingsHandler = SettingsHandler(context)

    fun hasSavedGame(): Boolean {
        val raw = settingsHandler.readValue(KEY) ?: return false
        return raw.isNotEmpty() && raw != SENTINEL_EMPTY
    }

    fun clear() {
        settingsHandler.updateSetting(KEY, SENTINEL_EMPTY)
    }

    fun save(state: TiramisuGameState) {
        val json = JSONObject().apply {
            put("v",              CURRENT_VERSION)
            put("difficulty",     state.difficulty.key)
            put("redealsLeft",    state.redealsLeft)
            put("gameStartMs",    state.gameStartTimeMillis)
            put("timerPausedMs",  state.timerPausedMs)
            put("isTimerPaused",  state.isTimerPaused)
            put("stock",          listToJson(state.stock))
            put("foundations",    listToJson(state.foundations))
            for (i in 0..3) put("pile$i", listToJson(state.piles[i]))
        }
        settingsHandler.updateSetting(KEY, json.toString())
    }

    fun load(): TiramisuGameState? {
        val raw = settingsHandler.readValue(KEY) ?: return null
        if (raw == SENTINEL_EMPTY || raw.isEmpty()) return null
        return try {
            val json    = JSONObject(raw)
            val version = json.optInt("v", 0)
            if (version != CURRENT_VERSION) return null
            val difficulty = Difficulty.fromKey(json.getString("difficulty"))
            TiramisuGameState(
                piles             = List(4) { i -> jsonToMutableList(json.getJSONArray("pile$i")) },
                stock             = jsonToMutableList(json.getJSONArray("stock")),
                foundations       = jsonToMutableList(json.getJSONArray("foundations")),
                redealsLeft       = json.getInt("redealsLeft"),
                difficulty        = difficulty,
                gameStartTimeMillis = json.getLong("gameStartMs"),
                timerPausedMs     = json.getLong("timerPausedMs"),
                isTimerPaused     = json.getBoolean("isTimerPaused"),
                hasActiveGame     = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() { settingsHandler.close() }

    companion object {
        private const val KEY              = "savedTiramisuState"
        private const val CURRENT_VERSION  = 1
        private const val SENTINEL_EMPTY   = "__empty__"

        private fun listToJson(list: List<String>): JSONArray {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            return arr
        }

        private fun jsonToMutableList(arr: JSONArray): MutableList<String> {
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            return list
        }
    }
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 11: game.xml — Tiramisu layout

**Files:**
- Modify: `app/src/main/res/layout/game.xml` *(complete replacement)*
- Modify: `app/src/main/res/values/strings.xml` *(add Tiramisu game strings)*

- [ ] **Step 1: Add game strings to `strings.xml`**

```xml
<!-- Game screen -->
<string name="stock_count">Tallone: %1$d</string>
<string name="redeal_button">↩ Ridistribuisci</string>
<string name="hint_button">💡</string>
<string name="foundation_empty_desc">Fondazione vuota %1$d</string>
<string name="pile_empty_desc">Mazzetto vuoto %1$d</string>
<string name="card_desc">%1$s di %2$s</string>
<string name="redeals_left">Ridistribuzioni: %1$d</string>
<string name="obbligato_hint">Devi prima mandare questa carta alla base!</string>
```

- [ ] **Step 2: Replace `game.xml` with Tiramisu layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/gameRoot"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Top bar: timer + difficulty badge -->
    <LinearLayout
        android:id="@+id/topBar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingHorizontal="12dp"
        android:paddingTop="8dp"
        android:paddingBottom="4dp"
        android:gravity="center_vertical"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <TextView
            android:id="@+id/tvTimer"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="00:00"
            android:textSize="18sp"
            android:fontFamily="monospace"
            android:contentDescription="Timer di gioco" />

        <TextView
            android:id="@+id/tvDifficulty"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="🟡 Normale"
            android:textSize="14sp"
            android:paddingHorizontal="8dp"
            android:paddingVertical="4dp"
            tools:ignore="HardcodedText" />

        <ImageButton
            android:id="@+id/btnMenu"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_more"
            android:background="@android:color/transparent"
            android:contentDescription="Menu" />
    </LinearLayout>

    <!-- Foundations row (4 slots) -->
    <LinearLayout
        android:id="@+id/foundationsRow"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingHorizontal="8dp"
        android:paddingVertical="6dp"
        android:gravity="center_vertical"
        app:layout_constraintTop_toBottomOf="@id/topBar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <FrameLayout android:id="@+id/foundation0"
            android:layout_width="0dp" android:layout_weight="1"
            android:layout_height="72dp" android:layout_marginHorizontal="4dp"
            android:minWidth="48dp" android:minHeight="48dp"
            android:background="@drawable/zero"
            android:clickable="true" android:focusable="true"
            android:contentDescription="@string/foundation_empty_desc" />

        <FrameLayout android:id="@+id/foundation1"
            android:layout_width="0dp" android:layout_weight="1"
            android:layout_height="72dp" android:layout_marginHorizontal="4dp"
            android:minWidth="48dp" android:minHeight="48dp"
            android:background="@drawable/zero"
            android:clickable="true" android:focusable="true"
            android:contentDescription="@string/foundation_empty_desc" />

        <FrameLayout android:id="@+id/foundation2"
            android:layout_width="0dp" android:layout_weight="1"
            android:layout_height="72dp" android:layout_marginHorizontal="4dp"
            android:minWidth="48dp" android:minHeight="48dp"
            android:background="@drawable/zero"
            android:clickable="true" android:focusable="true"
            android:contentDescription="@string/foundation_empty_desc" />

        <FrameLayout android:id="@+id/foundation3"
            android:layout_width="0dp" android:layout_weight="1"
            android:layout_height="72dp" android:layout_marginHorizontal="4dp"
            android:minWidth="48dp" android:minHeight="48dp"
            android:background="@drawable/zero"
            android:clickable="true" android:focusable="true"
            android:contentDescription="@string/foundation_empty_desc" />
    </LinearLayout>

    <!-- 4 pile columns (scrollable, fill remaining space) -->
    <LinearLayout
        android:id="@+id/pilesArea"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:orientation="horizontal"
        app:layout_constraintTop_toBottomOf="@id/foundationsRow"
        app:layout_constraintBottom_toTopOf="@id/bottomBar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ScrollView android:id="@+id/pileScroll0"
            android:layout_width="0dp" android:layout_weight="1"
            android:layout_height="match_parent"
            android:overScrollMode="never"
            android:scrollbars="none">
            <LinearLayout android:id="@+id/pileContainer0"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:padding="4dp" />
        </ScrollView>

        <ScrollView android:id="@+id/pileScroll1"
            android:layout_width="0dp" android:layout_weight="1"
            android:layout_height="match_parent"
            android:overScrollMode="never"
            android:scrollbars="none">
            <LinearLayout android:id="@+id/pileContainer1"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:padding="4dp" />
        </ScrollView>

        <ScrollView android:id="@+id/pileScroll2"
            android:layout_width="0dp" android:layout_weight="1"
            android:layout_height="match_parent"
            android:overScrollMode="never"
            android:scrollbars="none">
            <LinearLayout android:id="@+id/pileContainer2"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:padding="4dp" />
        </ScrollView>

        <ScrollView android:id="@+id/pileScroll3"
            android:layout_width="0dp" android:layout_weight="1"
            android:layout_height="match_parent"
            android:overScrollMode="never"
            android:scrollbars="none">
            <LinearLayout android:id="@+id/pileContainer3"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:padding="4dp" />
        </ScrollView>
    </LinearLayout>

    <!-- Tutorial overlay (hidden by default) -->
    <LinearLayout
        android:id="@+id/tutorialOverlay"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp"
        android:background="#CC000000"
        android:visibility="gone"
        app:layout_constraintBottom_toTopOf="@id/bottomBar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <TextView
            android:id="@+id/tvTutorialInstruction"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="#FFFFFF"
            android:textSize="16sp"
            android:paddingBottom="8dp" />

        <Button
            android:id="@+id/btnTutorialNext"
            android:layout_width="wrap_content"
            android:layout_height="48dp"
            android:layout_gravity="end"
            android:text="@string/tutorial_next"
            android:minWidth="48dp" android:minHeight="48dp" />
    </LinearLayout>

    <!-- Bottom bar: stock + redeal + hint -->
    <LinearLayout
        android:id="@+id/bottomBar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingHorizontal="8dp"
        android:paddingVertical="8dp"
        android:gravity="center_vertical"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <!-- Stock (tallone) -->
        <FrameLayout
            android:id="@+id/stockArea"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="72dp"
            android:layout_marginEnd="8dp"
            android:minWidth="48dp"
            android:minHeight="48dp"
            android:clickable="true"
            android:focusable="true"
            android:contentDescription="Tallone — tocca per distribuire">

            <ImageView
                android:id="@+id/stockImage"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:scaleType="fitCenter"
                android:contentDescription="Dorso carta" />

            <TextView
                android:id="@+id/tvStockCount"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="bottom|end"
                android:textSize="12sp"
                android:padding="2dp"
                android:background="#88000000"
                android:textColor="#FFFFFF" />
        </FrameLayout>

        <!-- Redeal button (shown only when eligible) -->
        <Button
            android:id="@+id/btnRedeal"
            android:layout_width="wrap_content"
            android:layout_height="48dp"
            android:text="@string/redeal_button"
            android:textSize="14sp"
            android:visibility="gone"
            android:minWidth="48dp"
            android:minHeight="48dp"
            android:layout_marginEnd="8dp" />

        <!-- Redeals remaining label -->
        <TextView
            android:id="@+id/tvRedealsLeft"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="13sp"
            android:layout_marginEnd="8dp" />

        <!-- Hint button -->
        <Button
            android:id="@+id/btnHint"
            android:layout_width="wrap_content"
            android:layout_height="48dp"
            android:text="@string/hint_button"
            android:textSize="18sp"
            android:minWidth="48dp"
            android:minHeight="48dp" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 3: Verify build**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 12: GameActivity — full implementation

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt` *(full replacement)*

- [ ] **Step 1: Replace `GameActivity.kt` with full implementation**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt
package com.bottazzini.tiramisu

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bottazzini.tiramisu.settings.*
import com.bottazzini.tiramisu.utils.*

class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TUTORIAL_MODE = "tutorial_mode"
        /** Visible height of each non-top card in a pile (dp). Top card shows fully. */
        private const val CARD_PEEK_DP = 24
    }

    // ---- ViewModel & Repos ----
    private val vm: TiramisuViewModel by lazy { ViewModelProvider(this)[TiramisuViewModel::class.java] }
    private lateinit var settingsHandler: SettingsHandler
    private lateinit var gameStateRepo: GameStateRepository
    private lateinit var gameLogRepo: GameLogRepository

    // ---- UI refs ----
    private lateinit var tvTimer: TextView
    private lateinit var tvDifficulty: TextView
    private lateinit var tvStockCount: TextView
    private lateinit var tvRedealsLeft: TextView
    private lateinit var stockImage: ImageView
    private lateinit var stockArea: FrameLayout
    private lateinit var btnRedeal: Button
    private lateinit var btnHint: Button
    private lateinit var btnMenu: ImageButton
    private lateinit var gameRoot: View
    private val foundationViews = arrayOfNulls<FrameLayout>(4)
    private val pileContainers  = arrayOfNulls<LinearLayout>(4)
    private val pileScrollViews = arrayOfNulls<ScrollView>(4)
    private lateinit var tutorialOverlay: LinearLayout
    private lateinit var tvTutorialInstruction: TextView
    private lateinit var btnTutorialNext: Button

    // ---- State ----
    private var isTutorialMode = false
    private var tutorialEngine: TiramisuTutorialEngine? = null
    private var hintsUsedThisGame = 0
    private var cardType = "piacentine"
    private var cardBackKey = "bg2"
    private var hintedPileIdx: Int? = null     // index to highlight for hint
    private var mediaPlayer: MediaPlayer? = null

    // ---- Timer ----
    private val timerHandler  = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsUtils.applySystemBarInsets(window, null)
        setContentView(R.layout.game)

        settingsHandler = SettingsHandler(applicationContext)
        gameStateRepo   = GameStateRepository(applicationContext)
        gameLogRepo     = GameLogRepository(applicationContext)

        bindViews()
        setupListeners()

        isTutorialMode = intent.getBooleanExtra(EXTRA_TUTORIAL_MODE, false)
        val resume     = intent.getBooleanExtra("resume", false)

        cardType    = settingsHandler.readValue(Configuration.CARD_TYPE.value)    ?: "piacentine"
        cardBackKey = settingsHandler.readValue(Configuration.CARD_BACK.value)    ?: "bg2"
        val bg      = settingsHandler.readValue(Configuration.BACKGROUND.value)   ?: "bordeaux"
        gameRoot.background = ContextCompat.getDrawable(this, ResourceUtils.getDrawableByName(resources, packageName, bg))

        when {
            isTutorialMode -> startTutorial()
            resume         -> resumeGame()
            else           -> startNewGame()
        }
    }

    // ---- Initialisation ----

    private fun bindViews() {
        tvTimer        = findViewById(R.id.tvTimer)
        tvDifficulty   = findViewById(R.id.tvDifficulty)
        tvStockCount   = findViewById(R.id.tvStockCount)
        tvRedealsLeft  = findViewById(R.id.tvRedealsLeft)
        stockImage     = findViewById(R.id.stockImage)
        stockArea      = findViewById(R.id.stockArea)
        btnRedeal      = findViewById(R.id.btnRedeal)
        btnHint        = findViewById(R.id.btnHint)
        btnMenu        = findViewById(R.id.btnMenu)
        gameRoot       = findViewById(R.id.gameRoot)
        tutorialOverlay       = findViewById(R.id.tutorialOverlay)
        tvTutorialInstruction = findViewById(R.id.tvTutorialInstruction)
        btnTutorialNext       = findViewById(R.id.btnTutorialNext)

        for (i in 0..3) {
            foundationViews[i] = findViewById(resources.getIdentifier("foundation$i", "id", packageName))
            pileContainers[i]  = findViewById(resources.getIdentifier("pileContainer$i", "id", packageName))
            pileScrollViews[i] = findViewById(resources.getIdentifier("pileScroll$i", "id", packageName))
        }
    }

    private fun setupListeners() {
        stockArea.setOnClickListener { onStockTapped() }
        btnRedeal.setOnClickListener { onRedealTapped() }
        btnHint.setOnClickListener   { onHintTapped() }
        btnMenu.setOnClickListener   { showMenuDialog() }
        btnTutorialNext.setOnClickListener { advanceTutorial() }

        for (i in 0..3) {
            foundationViews[i]?.setOnClickListener { onFoundationViewTapped(i) }
        }
    }

    private fun startNewGame() {
        val diffKey    = settingsHandler.readValue(Configuration.DIFFICULTY.value) ?: Difficulty.NORMALE.key
        val difficulty = Difficulty.fromKey(diffKey)
        vm.newGame(difficulty)
        gameStateRepo.clear()
        hintsUsedThisGame = 0
        startTimer()
        renderAll()
    }

    private fun resumeGame() {
        val saved = gameStateRepo.load()
        if (saved != null) {
            vm.restoreState(saved)
            restoreTimer(saved)
        } else {
            startNewGame()
        }
        renderAll()
    }

    private fun startTutorial() {
        vm.newTutorialGame()
        val steps = TiramisuTutorialSteps.steps(resources)
        tutorialEngine = TiramisuTutorialEngine(steps)
        hintsUsedThisGame = 0
        startTimer()
        renderAll()
        showTutorialStep()
    }

    // ---- Game interactions ----

    private fun onStockTapped() {
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return
            if (!eng.isStockDealStep()) return  // tutorial controls when you can deal
        }
        if (vm.dealFromStock()) {
            playSound(R.raw.card_flip)
            renderAll()
            checkWin()
        } else if (vm.canRedeal()) {
            // Prompt handled by btnRedeal visibility
        }
    }

    private fun onRedealTapped() {
        if (vm.redeal()) {
            playSound(R.raw.card_flip)
            renderAll()
        }
    }

    private fun onPileCardTapped(pileIdx: Int) {
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return
            val card    = vm.state!!.topOfPile(pileIdx)
            val allowed = eng.isPileTapAllowed(pileIdx, card)
            if (!allowed) return
        }

        val result = vm.onPileTapped(pileIdx)
        when (result) {
            TapResult.MOVED   -> { playSound(R.raw.card_flip); renderAll(); checkWin()
                                   if (isTutorialMode) advanceTutorial() }
            TapResult.INVALID -> showInvalidMoveToast()
            else               -> renderAll()  // SELECTED, DESELECTED, NOTHING
        }
    }

    private fun onFoundationViewTapped(foundationIdx: Int) {
        val sel = vm.selectedPileIndex
        if (sel != null) {
            if (vm.onFoundationTapped(sel)) {
                playSound(R.raw.card_flip)
                renderAll()
                checkWin()
                if (isTutorialMode) advanceTutorial()
            }
        }
    }

    private fun onHintTapped() {
        val s    = vm.state ?: return
        val hint = TiramisuSolver.findHint(s)
        hintsUsedThisGame++
        if (hint == null) {
            Toast.makeText(this, "Nessuna mossa disponibile", Toast.LENGTH_SHORT).show()
            return
        }
        hintedPileIdx = hint.fromPile
        renderAll()
        timerHandler.postDelayed({ hintedPileIdx = null; renderAll() }, 1500)
    }

    // ---- Rendering ----

    private fun renderAll() {
        val s = vm.state ?: return
        renderFoundations(s)
        for (i in 0..3) renderPile(i, s)
        renderBottomBar(s)
        tvDifficulty.text = s.difficulty.displayName
    }

    private fun renderFoundations(s: TiramisuGameState) {
        for (i in 0..3) {
            val view = foundationViews[i] ?: continue
            val top  = s.foundations[i]
            if (top == "zero") {
                view.setBackgroundResource(R.drawable.zero)
                view.contentDescription = getString(R.string.foundation_empty_desc, i + 1)
            } else {
                val resId = ResourceUtils.getCardDrawable(resources, packageName, cardType, top)
                view.setBackgroundResource(resId)
                view.contentDescription = cardDescription(top)
            }
        }
    }

    private fun renderPile(pileIdx: Int, s: TiramisuGameState) {
        val container = pileContainers[pileIdx] ?: return
        container.removeAllViews()
        val pile   = s.piles[pileIdx]
        val peekPx = (CARD_PEEK_DP * resources.displayMetrics.density).toInt()

        if (pile.isEmpty()) {
            val placeholder = ImageView(this)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, peekPx * 3)
            placeholder.layoutParams = params
            placeholder.setImageResource(R.drawable.zero)
            placeholder.scaleType = ImageView.ScaleType.FIT_XY
            placeholder.contentDescription = getString(R.string.pile_empty_desc, pileIdx + 1)
            placeholder.alpha = 0.4f
            container.addView(placeholder)
            return
        }

        val isSelected    = vm.selectedPileIndex == pileIdx
        val isObbligato   = vm.obbligatoTargets().contains(pileIdx)
        val isHinted      = hintedPileIdx == pileIdx
        val cardHeightPx  = (72 * resources.displayMetrics.density).toInt()

        pile.forEachIndexed { cardIdx, card ->
            val imageView = ImageView(this)
            val height    = if (cardIdx == pile.lastIndex) cardHeightPx else peekPx
            val params    = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
            if (cardIdx > 0) params.topMargin = 0  // zero margin: peek height IS the visible strip
            imageView.layoutParams = params
            imageView.scaleType    = ImageView.ScaleType.FIT_XY

            val resId = ResourceUtils.getCardDrawable(resources, packageName, cardType, card)
            imageView.setImageResource(resId)
            imageView.contentDescription = cardDescription(card)

            // Accessibility: only top card is focusable/clickable
            if (cardIdx == pile.lastIndex) {
                imageView.isFocusable  = true
                imageView.isClickable  = true
                imageView.setOnClickListener { onPileCardTapped(pileIdx) }

                // Visual feedback
                when {
                    isSelected  -> imageView.alpha = 0.7f
                    isObbligato -> imageView.setColorFilter(0x88FF0000.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    isHinted    -> imageView.setColorFilter(0x8800FF00.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    else        -> { imageView.alpha = 1f; imageView.clearColorFilter() }
                }
            } else {
                imageView.isFocusable = false
                imageView.isClickable = false
            }

            container.addView(imageView)
        }

        // Auto-scroll to top of pile
        pileScrollViews[pileIdx]?.post {
            pileScrollViews[pileIdx]?.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun renderBottomBar(s: TiramisuGameState) {
        tvStockCount.text  = "${s.stock.size}"
        tvRedealsLeft.text = getString(R.string.redeals_left, s.redealsLeft)

        val canRedeal = vm.canRedeal()
        btnRedeal.isVisible = canRedeal

        if (s.stock.isEmpty() && !canRedeal) {
            stockImage.setImageResource(R.drawable.zero)
            stockImage.alpha = 0.3f
        } else {
            val backResId = ResourceUtils.getDrawableByName(resources, packageName, cardBackKey)
            stockImage.setImageResource(backResId)
            stockImage.alpha = 1f
        }
    }

    private fun cardDescription(card: String): String {
        val suitName = when (TiramisuMoveValidator.suit(card)) {
            "b" -> "Bastoni"
            "c" -> "Coppe"
            "d" -> "Denari"
            "s" -> "Spade"
            else -> card.substring(0, 1)
        }
        val rankName = when (TiramisuMoveValidator.rank(card)) {
            1  -> "Asso"
            else -> TiramisuMoveValidator.rank(card).toString()
        }
        return getString(R.string.card_desc, rankName, suitName)
    }

    // ---- Win / End ----

    private fun checkWin() {
        if (!vm.isWon()) return
        stopTimer()
        val s = vm.state!!
        val durationMs = System.currentTimeMillis() - s.gameStartTimeMillis
        gameLogRepo.insert(GameLog(
            timestamp   = System.currentTimeMillis(),
            durationMs  = durationMs,
            won         = true,
            hintsUsed   = hintsUsedThisGame,
            difficulty  = s.difficulty.key,
            redealsUsed = s.difficulty.redeals - s.redealsLeft
        ))
        gameStateRepo.clear()
        val intent = Intent(this, YouWonActivity::class.java).apply {
            putExtra("duration_ms",  durationMs)
            putExtra("difficulty",   s.difficulty.key)
        }
        startActivity(intent)
        finish()
    }

    // ---- Tutorial ----

    private fun showTutorialStep() {
        val eng  = tutorialEngine ?: return
        if (eng.isComplete()) { endTutorial(); return }
        val step = eng.currentStep()
        tutorialOverlay.visibility = View.VISIBLE
        tvTutorialInstruction.text = getString(step.instructionResId)
        btnTutorialNext.isVisible  = step.requiredMove == null
    }

    private fun advanceTutorial() {
        val eng = tutorialEngine ?: return
        eng.advanceToNext()
        if (eng.isComplete()) { endTutorial(); return }
        showTutorialStep()
        renderAll()
    }

    private fun endTutorial() {
        tutorialOverlay.visibility = View.GONE
        tutorialEngine             = null
        isTutorialMode             = false
        Toast.makeText(this, "Tutorial completato! Buon gioco 🎉", Toast.LENGTH_LONG).show()
    }

    private fun showInvalidMoveToast() {
        val obbMsg = vm.obbligatoTargets().isNotEmpty()
        val msg = if (obbMsg) getString(R.string.obbligato_hint) else getString(R.string.invaild_move)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // ---- Menu ----

    private fun showMenuDialog() {
        stopTimer()
        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(arrayOf("↩ Riprendi", "🏠 Abbandona")) { _, which ->
                when (which) {
                    0 -> startTimer()
                    1 -> abandonGame()
                }
            }
            .setOnCancelListener { startTimer() }
            .show()
    }

    private fun abandonGame() {
        val s = vm.state
        if (s != null) {
            val durationMs = System.currentTimeMillis() - s.gameStartTimeMillis
            gameLogRepo.insert(GameLog(
                timestamp   = System.currentTimeMillis(),
                durationMs  = durationMs,
                won         = false,
                hintsUsed   = hintsUsedThisGame,
                difficulty  = s.difficulty.key,
                redealsUsed = s.difficulty.redeals - s.redealsLeft
            ))
        }
        gameStateRepo.clear()
        finish()
    }

    // ---- Timer ----

    private fun startTimer() {
        val s = vm.state ?: return
        if (s.gameStartTimeMillis == 0L) s.gameStartTimeMillis = System.currentTimeMillis()
        s.isTimerPaused = false
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - s.gameStartTimeMillis - s.timerPausedMs)
                tvTimer.text = TimeUtils.formatElapsed(elapsed)
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        val s = vm.state ?: return
        if (::timerRunnable.isInitialized) timerHandler.removeCallbacks(timerRunnable)
        s.isTimerPaused = true
    }

    private fun restoreTimer(s: TiramisuGameState) {
        if (s.isTimerPaused) {
            s.timerPausedMs += System.currentTimeMillis() - (s.gameStartTimeMillis + s.timerPausedMs)
        }
        startTimer()
    }

    // ---- Lifecycle ----

    override fun onPause() {
        super.onPause()
        stopTimer()
        val s = vm.state
        if (s != null && s.hasActiveGame && !isTutorialMode) {
            gameStateRepo.save(s)
        }
    }

    override fun onResume() {
        super.onResume()
        if (vm.state?.hasActiveGame == true) startTimer()
    }

    override fun onDestroy() {
        gameStateRepo.close()
        gameLogRepo.close()
        settingsHandler.close()
        mediaPlayer?.release()
        super.onDestroy()
    }

    // ---- Helpers ----

    private fun playSound(resId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resId).also {
                it.setOnCompletionListener { mp -> mp.release() }
                it.start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
```

- [ ] **Step 2: Verify build and launch on emulator/device**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. Install and tap "Nuova partita" — you should see 4 pile columns, foundations, and stock.

---

## Task 13: StatsActivity — per-difficulty stats

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/StatsActivity.kt`
- Modify: `app/src/main/res/layout/activity_stats.xml` *(add difficulty tabs or sections)*
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add stats strings**

```xml
<string name="stats_total_played">Partite giocate</string>
<string name="stats_wins">Vinte</string>
<string name="stats_win_pct">%% vittorie</string>
<string name="stats_best_time">Tempo migliore</string>
<string name="stats_avg_time">Tempo medio (vinte)</string>
<string name="stats_section_facile">🟢 Facile</string>
<string name="stats_section_normale">🟡 Normale</string>
<string name="stats_section_difficile">🔴 Difficile</string>
<string name="stats_section_all">Tutto</string>
<string name="stats_no_data">—</string>
```

- [ ] **Step 2: Update `StatsActivity.kt`**

Replace the body of `onCreate` and add helper methods. The existing `StatsActivity` layout uses `item_stat_row.xml` rows — reuse that pattern. Add three sections (Facile / Normale / Difficile):

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_stats)

    gameLogRepo = GameLogRepository(applicationContext)
    recordsHandler = RecordsHandler(applicationContext)

    renderDifficultySection(Difficulty.FACILE)
    renderDifficultySection(Difficulty.NORMALE)
    renderDifficultySection(Difficulty.DIFFICILE)
    renderOverallRecords()
    applyTheme()
}

private fun renderDifficultySection(diff: Difficulty) {
    val container = findContainerForDifficulty(diff)  // map diff to a LinearLayout in the XML
    val played  = gameLogRepo.countByDifficulty(diff.key)
    val wins    = gameLogRepo.countWinsByDifficulty(diff.key)
    val pct     = if (played > 0) (wins * 100 / played) else 0
    val bestMs  = gameLogRepo.bestWinTimeMs(diff.key)
    val avgMs   = gameLogRepo.avgWinDurationMs(diff.key)

    addStatRow(container, getString(R.string.stats_total_played), "$played")
    addStatRow(container, getString(R.string.stats_wins), "$wins ($pct%)")
    addStatRow(container, getString(R.string.stats_best_time),
               if (bestMs != null) TimeUtils.formatElapsed(bestMs) else getString(R.string.stats_no_data))
    addStatRow(container, getString(R.string.stats_avg_time),
               if (avgMs != null) TimeUtils.formatElapsed(avgMs) else getString(R.string.stats_no_data))
}
```

The `activity_stats.xml` needs three `LinearLayout` containers (one per difficulty) and the `addStatRow` helper inflates `item_stat_row.xml` (already in the project). Add them by updating the XML to include `@+id/statsContainerFacile`, `@+id/statsContainerNormale`, `@+id/statsContainerDifficile`.

- [ ] **Step 3: Verify build**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 14: Tutorial — TiramisuTutorialSteps + TiramisuTutorialEngine

**Files:**
- Create: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialSteps.kt`
- Create: `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialEngine.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/bottazzini/tiramisu/MainActivity.kt`

- [ ] **Step 1: Add tutorial strings**

```xml
<!-- Tutorial steps -->
<string name="tut_intro">Benvenuto in Tiramisù! Obiettivo: sposta tutte le carte sulle 4 basi in alto, per seme dall\'Asso al 10. Tocca Avanti.</string>
<string name="tut_deal">Tocca il Tallone in basso per distribuire le prime carte.</string>
<string name="tut_deal_confirm">✓ Carte distribuite. L\'Asso è andato automaticamente alla base!</string>
<string name="tut_same_suit">Le carte dello stesso seme si possono spostare liberamente tra i mazzetti. Sposta la carta colorata sul mazzetto indicato.</string>
<string name="tut_same_suit_confirm">✓ Ottimo! Stesso seme, qualsiasi valore.</string>
<string name="tut_foundation">Tocca un mazzetto per selezionarlo, poi tocca la base per portarci la carta giusta.</string>
<string name="tut_foundation_confirm">✓ Perfetto! La carta è a casa.</string>
<string name="tut_empty_pile">Un mazzetto vuoto accetta qualsiasi carta. Prova a spostare una carta in uno slot libero.</string>
<string name="tut_redeal">Quando il tallone finisce, puoi ridistribuire raccogliendo i mazzetti. Tocca Ridistribuisci.</string>
<string name="tut_finish">Bravo! Ora conosci le regole di Tiramisù. Buon gioco! 🎉</string>
```

- [ ] **Step 2: Create `TiramisuTutorialSteps.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialSteps.kt
package com.bottazzini.tiramisu.utils

import android.content.res.Resources
import com.bottazzini.tiramisu.R

data class TiramisuTutorialStep(
    val label:             String,
    val instructionResId:  Int,
    val confirmationResId: Int?,
    /** Pile indices to highlight (0-3), or empty for none. */
    val highlightPiles:    List<Int> = emptyList(),
    /** If non-null, the only tap action allowed (pileIdx, targetPileIdx or -1 for foundation/stock). */
    val requiredMove:      TiramisuTutorialMove? = null
)

data class TiramisuTutorialMove(
    val sourcePile: Int,   // 0-3, or -1 for stock tap
    val targetPile: Int    // 0-3, or -1 for foundation, or -2 for redeal
)

object TiramisuTutorialSteps {
    fun steps(res: Resources): List<TiramisuTutorialStep> = listOf(
        TiramisuTutorialStep(
            label            = "intro",
            instructionResId = R.string.tut_intro,
            confirmationResId = null,
            requiredMove     = null   // info step, advance via "Avanti" button
        ),
        TiramisuTutorialStep(
            label            = "deal",
            instructionResId = R.string.tut_deal,
            confirmationResId = R.string.tut_deal_confirm,
            requiredMove     = TiramisuTutorialMove(sourcePile = -1, targetPile = -1) // stock tap
        ),
        TiramisuTutorialStep(
            label            = "same_suit",
            instructionResId = R.string.tut_same_suit,
            confirmationResId = R.string.tut_same_suit_confirm,
            highlightPiles   = listOf(1, 2),
            requiredMove     = TiramisuTutorialMove(sourcePile = 1, targetPile = 2)
        ),
        TiramisuTutorialStep(
            label            = "foundation",
            instructionResId = R.string.tut_foundation,
            confirmationResId = R.string.tut_foundation_confirm,
            requiredMove     = TiramisuTutorialMove(sourcePile = 1, targetPile = -1) // to foundation
        ),
        TiramisuTutorialStep(
            label            = "finish",
            instructionResId = R.string.tut_finish,
            confirmationResId = null,
            requiredMove     = null
        )
    )
}
```

- [ ] **Step 3: Create `TiramisuTutorialEngine.kt`**

```kotlin
// app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuTutorialEngine.kt
package com.bottazzini.tiramisu.utils

class TiramisuTutorialEngine(private val steps: List<TiramisuTutorialStep>) {

    private var index = 0
    private var lastMoveExecuted = false

    fun currentStep(): TiramisuTutorialStep = steps[index.coerceAtMost(steps.lastIndex)]
    fun isComplete(): Boolean = index >= steps.size

    fun isCurrentStepComplete(): Boolean {
        if (isComplete()) return true
        val step = currentStep()
        return step.requiredMove == null || lastMoveExecuted
    }

    /** Returns true if a stock tap is the expected move for the current step. */
    fun isStockDealStep(): Boolean {
        if (isComplete()) return false
        val move = currentStep().requiredMove ?: return false
        return move.sourcePile == -1
    }

    /** Returns true if tapping [pileIdx] is allowed in the current tutorial step. */
    fun isPileTapAllowed(pileIdx: Int, card: String): Boolean {
        if (isComplete()) return false
        val move = currentStep().requiredMove ?: return false
        if (move.sourcePile == -1) return false  // only stock tap allowed
        return move.sourcePile == pileIdx
    }

    /** Call after a pile-to-pile or pile-to-foundation move is executed. */
    fun onMoveExecuted(srcPile: Int, dstPile: Int) {
        if (isComplete()) return
        val required = currentStep().requiredMove ?: return
        if (required.sourcePile == srcPile && required.targetPile == dstPile) {
            lastMoveExecuted = true
        }
    }

    fun advanceToNext() {
        if (isComplete()) return
        val step = currentStep()
        if (step.requiredMove != null && !lastMoveExecuted) return
        index++
        lastMoveExecuted = false
    }
}
```

- [ ] **Step 4: Update `MainActivity.kt` — tutorial prompt**

In `MainActivity.kt`, update the `showTutorialPromptDialog` strings and the tutorial launch to pass `EXTRA_TUTORIAL_MODE = true`. The existing `showTutorialPromptDialog()` and `launchGameActivity(tutorial)` methods already do this — just verify that:

1. `EXTRA_TUTORIAL_MODE` references `GameActivity.EXTRA_TUTORIAL_MODE`
2. `isTutorialSeen()` / `markTutorialSeen()` uses `"tiramisu_prefs"` (rename from `"trasloco_prefs"`):

```kotlin
// In MainActivity.kt — change "trasloco_prefs" → "tiramisu_prefs" in all getSharedPreferences calls
private fun isTutorialSeen(): Boolean =
    getSharedPreferences("tiramisu_prefs", MODE_PRIVATE).getBoolean("tutorial_seen", false)

private fun markTutorialSeen() =
    getSharedPreferences("tiramisu_prefs", MODE_PRIVATE).edit().putBoolean("tutorial_seen", true).apply()
```

Also update the `deck_chosen` pref key:
```kotlin
val prefs = getSharedPreferences("tiramisu_prefs", MODE_PRIVATE)
```

- [ ] **Step 5: Verify build**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 15: YouWonActivity + final wiring

**Files:**
- Modify: `app/src/main/java/com/bottazzini/tiramisu/YouWonActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/build.gradle` (double-check version)

- [ ] **Step 1: Update `YouWonActivity.kt` to show difficulty**

Find the section where `YouWonActivity` reads the intent extras and add difficulty display:

```kotlin
// In YouWonActivity.onCreate():
val durationMs  = intent.getLongExtra("duration_ms", 0L)
val diffKey     = intent.getStringExtra("difficulty") ?: Difficulty.NORMALE.key
val difficulty  = Difficulty.fromKey(diffKey)

// Display difficulty badge in the YouWon screen
val tvDiff = findViewById<TextView>(R.id.tvDifficultyWon)  // add this view if not present
tvDiff?.text = difficulty.displayName
```

Add `R.id.tvDifficultyWon` to `activity_you_won.xml` if it doesn't exist:
```xml
<TextView
    android:id="@+id/tvDifficultyWon"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textSize="18sp"
    android:layout_gravity="center" />
```

- [ ] **Step 2: Add import to YouWonActivity**

```kotlin
import com.bottazzini.tiramisu.utils.Difficulty
```

- [ ] **Step 3: Final build and full smoke test**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
./gradlew assembleDebug
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, all unit tests PASS.

- [ ] **Step 4: Manual smoke test on device/emulator**

Install and verify:
1. App launches with splash → DeckPicker (first run) → Main menu showing "Tiramisù"
2. Settings screen shows difficulty radio (Facile / Normale / Difficile) and saves selection
3. Nuova partita (Normale) → GameActivity loads, stock shows 40 cards, 4 piles empty, 4 foundations empty
4. Tap stock → 4 cards dealt; if any is Asso → auto-moves to foundation
5. Tap a pile card → gets selected (alpha changes); tap same-suit destination → card moves
6. Tap different-suit destination → "Mossa non valida" toast
7. Drain stock → redeal button appears; tap redeal → stock repopulated
8. Win all 4 foundations → YouWonActivity with difficulty shown
9. Tutorial: launches, overlay shows, scripted steps advance correctly
10. Statistics screen shows per-difficulty rows

- [ ] **Step 5: Create git repository**

```bash
cd /Users/bottazzini/Documents/Progetti/Tiramisu
git init
git add .
git commit -m "feat: Tiramisù v1.0.0 — initial release

- Solitario Tiramisù con 3 livelli di difficoltà (Facile / Normale / Difficile)
- Motore di gioco: TiramisuMoveValidator, TiramisuDeckSetup, TiramisuViewModel
- Tiramisu Obbligato in modalità Difficile
- Tutorial guidato a 5 passi
- Statistiche per livello di difficoltà
- Salva e riprendi partita
- Accessibilità: sp fonts, 48dp touch targets, content descriptions
- Fork da Trasloco — stesso mazzo italiano, stessi assets

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Self-Review

### Spec coverage check

| Spec requirement | Task |
|---|---|
| Hard fork da Trasloco | Task 1 |
| Regole Tiramisu (stesso seme, qualsiasi valore) | Task 6 |
| Deal dal tallone | Task 7 |
| Ridistribuzione destra→sinistra | Task 7 |
| Auto-move Aces | Task 7 |
| 3 livelli difficoltà | Task 3 |
| Difficoltà nelle Impostazioni | Task 4 |
| Layout 4 pile + stock + 4 basi | Task 11 |
| Accessibilità (sp, 48dp, content desc) | Task 11 + 12 |
| Tiramisu Obbligato (Difficile) | Task 7 + 12 |
| Salva e riprendi partita | Task 10 + 12 |
| Statistiche per difficoltà | Task 9 + 13 |
| Tutorial dedicato 7 passi | Task 14 |
| Tutorial nelle impostazioni di primo avvio | Task 14 |
| DB rinominato Tiramisu.db | Task 1 + 9 |
| App name "Tiramisù" | Task 1 |
| Selezione mazzo (DeckPicker invariato) | Task 1 (ereditato) |
| Temi e sfondi (invariati) | Task 1 (ereditato) |
| YouWonActivity con difficoltà | Task 15 |

All spec requirements covered. ✅
