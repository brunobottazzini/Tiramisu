# Tiramisù — Final Touches Design Spec
**Data**: 2026-05-22
**Stato**: Da approvare

---

## Panoramica

Tre rifiniture finali alle impostazioni utente:

1. **Setting "Suoni"** — nuova chiave per disabilitare globalmente i suoni dell'app.
2. **Rename + implementazione "Completamento automatico"** — l'attuale toggle "Auto-muovi su end deck" viene rinominato e collegato realmente al gameplay: quando ON, qualsiasi carta in cima a un mazzetto che possa andare in fondazione viene spostata automaticamente (oggi accade solo per gli Assi).
3. **Implementazione "Trasferisci intero mazzo"** — il toggle `FAST_DEAL` esiste in DB ma non è collegato a nulla. Quando ON, un singolo tap sul tallone svuota tutto lo stock con animazione "educativa" a ondate.

Tutte le modifiche sono additive: nessuna migration DB, nessuna rottura di partite salvate, nessun reset di record/achievement.

---

## 1. Setting "Suoni"

### Configurazione

Nuovo valore nell'enum `Configuration` in `settings/SettingsHandler.kt`:

```kotlin
SOUND_ENABLED("soundEnabled"),
```

Default in `insertDefaultSettings()`: `"enabled"`. Preserva il comportamento attuale dell'app per chi installa la prima volta.

### UI

In `res/layout/settings.xml`, nuova riga switch sotto il blocco "Completamento automatico" (riusa il template `LinearLayout` orizzontale dei toggle esistenti). ID nuovi: `soundRow`, `switchSound`. Handler XML: `android:onClick="changeSoundEnabled"`.

Stringhe in `res/values/strings.xml`:

```xml
<string name="settings_label_sound">Sounds</string>
<string name="sound_with_icon">🔊 Sounds</string>
```

Italiano in `res/values-it/strings.xml`:

```xml
<string name="settings_label_sound">Suoni</string>
<string name="sound_with_icon">🔊 Suoni</string>
```

Tutte le 17 directory locali con stringhe (`values`, `values-de`, `values-es`, `values-fr`, `values-hi`, `values-it`, `values-ja`, `values-ko`, `values-nl`, `values-pl`, `values-pt`, `values-pt-rBR`, `values-pt-rPT`, `values-ru`, `values-th`, `values-tr`, `values-zh-rCN`) ricevono le due stringhe con traduzioni naturali del termine "Suoni"/"Sounds". `values-night` non contiene stringhe e va ignorata.

### Handler in `SettingsActivity`

```kotlin
fun changeSoundEnabled(view: View) {
    val switch = view as Switch
    val value = if (switch.isChecked) "enabled" else "disabled"
    settingsHandler.updateSetting(Configuration.SOUND_ENABLED.value, value)
}
```

In `readConfigurations()`:

```kotlin
val sound = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) ?: "enabled"
findViewById<Switch>(R.id.switchSound).isChecked = (sound == "enabled")
```

### Punti di consumo

Quattro Activity riproducono suoni. Ognuna legge il setting in `onCreate`/`onResume` e cachea il valore in un campo `soundsEnabled: Boolean`. Il `playSound(...)` esistente diventa:

```kotlin
private fun playSound(resId: Int) {
    if (!soundsEnabled) return
    // ... corpo esistente invariato
}
```

File toccati:
- `GameActivity.kt` (flipcard, multipli call site)
- `MainActivity.kt` (change_activity, navigazione menu)
- `YouWonActivity.kt` (youwin) — gate prima di `MediaPlayer.create(...)`
- `SplashActivity.kt` — gate prima di `MediaPlayer.create(this, R.raw.shuffle)` alla riga 81

Il refresh in `onResume()` garantisce che cambi del setting da `SettingsActivity` siano effettivi senza riavviare l'Activity.

---

## 2. Rename + implementazione "Completamento automatico"

### Rename (testo, non chiave)

La chiave DB resta `AUTO_MOVE("autoMove")` per preservare il valore salvato dagli utenti dev. Cambiare la chiave forzerebbe un reset del setting.

ID layout (`switchAutoMove`, `autoMoveRow`) e handler `changeAutoMove` **invariati** (rinominarli non porta benefici).

Stringhe in `res/values/strings.xml`:

```xml
<string name="settings_label_auto_move">Auto-complete</string>
<string name="auto_move_with_icon">🎯 Auto-complete</string>
```

Italiano:

```xml
<string name="settings_label_auto_move">Completamento automatico</string>
<string name="auto_move_with_icon">🎯 Completamento automatico</string>
```

Tutte le 17 directory locali con stringhe aggiornate con traduzione naturale di "Auto-complete"/"Completamento automatico".

### Comportamento

Default `"disabled"` invariato (preserva l'esperienza attuale per chi aggiorna).

Quando il setting è **disabled**: solo gli Assi (rank 1) vengono spostati automaticamente in fondazione dopo deal/move (comportamento attuale, regressione zero).

Quando il setting è **enabled**: il loop di auto-move si estende a qualsiasi rank — ogni top-pile che può andare in fondazione (rank = foundation top + 1, stesso seme) viene spostato automaticamente, ripetendo finché stabile.

### Logica nel ViewModel

In `TiramisuViewModel.kt`, il metodo privato `autoMoveAces(defaultSource: AceSource)` viene **rinominato** in `autoMoveToFoundation(defaultSource: AutoFoundationSource)` e generalizzato:

```kotlin
var autoCompleteEnabled: Boolean = false   // settato da GameActivity in onResume

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
            // Default disabled: solo Assi. Enabled: qualsiasi rank.
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

Rinomini collegati:
- `AceMove` → `AutoFoundationMove` (data class, stessi campi)
- `AceSource` → `AutoFoundationSource` (enum, valori `STOCK` / `PILE_TOP` invariati)
- `consumeAutoAceMoves()` → `consumeAutoFoundationMoves()`
- `_lastAutoAceMoves` → `_lastAutoFoundationMoves`

I tre call-site (`dealFromStock`, `onFoundationTapped`, `movePileToPile`) chiamano la versione rinominata: nessun cambio logico nei call-site.

### Iniezione del flag dal `GameActivity`

`GameActivity` legge il setting in `onResume()` (così le modifiche dal `SettingsActivity` sono effettive senza ricreare l'Activity):

```kotlin
override fun onResume() {
    super.onResume()
    vm.autoCompleteEnabled = settingsHandler.readValue(Configuration.AUTO_MOVE.value) == "enabled"
    soundsEnabled = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) != "disabled"
    fastDealEnabled = settingsHandler.readValue(Configuration.FAST_DEAL.value) == "enabled"
}
```

### Animazione

La pipeline di animazione esistente in `GameActivity` (oggi gestisce `consumeAutoAceMoves()` con un'animazione card-to-foundation) viene riusata 1:1 con i nomi nuovi. Nessun nuovo asset.

Durata e stagger (`ACE_DURATION_MS = 250`, `ACE_STAGGER_MS = 80`) restano invariati.

### Interazione con Obbligato (DIFFICILE)

- Setting ON: il sistema esegue la mossa che Obbligato avrebbe forzato, prima che l'evidenziatura sul top-pile compaia. L'utente non vede mai il blocco Obbligato attivo.
- Setting OFF: Obbligato funziona come oggi (evidenzia + blocca finché l'utente non manda la carta in fondazione).

Le due feature restano logicamente distinte: Obbligato è una regola di gameplay (DIFFICILE), auto-completion è una preferenza utente (tutte le difficoltà).

### Achievement

Le stringhe `achievement_no_assist_desc` e `achievement_perfectionist_desc` menzionano "auto-move" ma il motore (`AchievementEngine.kt`) non lo controlla — la menzione è inerte. Le descrizioni vengono semplificate:

- `achievement_no_assist_desc`: "Vinci senza hint o ridistribuzioni" (EN: "Win without hints or redeals")
- `achievement_perfectionist_desc`: "Vinci 3 partite di fila senza hint o ridistribuzioni" (EN: "Win 3 games in a row without hints or redeals")

Criterio nel motore (`game.hintsUsed == 0 && game.redealsUsed == 0`) invariato. Auto-completion **non penalizza** achievement: è comfort, non un aiuto strategico (non vince partite altrimenti perse).

---

## 3. Implementazione "Trasferisci intero mazzo" (`FAST_DEAL`)

### Comportamento

Default `"enabled"` invariato.

Quando il setting è **enabled**: un tap singolo sul tallone (stockArea) → il sistema esegue tutte le ondate di deal rimanenti in catena, con animazione "educativa". Tap doppio non necessario.

Quando il setting è **disabled**: comportamento attuale (un tap = una ondata di max 4 carte).

### Animazione "educativa"

Lo stagger esistente per `dealCard` (`DEAL_CARD_STAGGER_MS = 80`) è mantenuto **intra-ondata**. Tra ondate consecutive viene introdotto un gap costante `FAST_DEAL_WAVE_GAP_MS = 120` (4 cards * 80ms + 120ms ≈ 440ms per ondata). Risultato: 10 ondate (40 carte) si svuotano in ~4 secondi con cadenza ritmica e leggibile.

### Logica nel ViewModel

Nuovo metodo pubblico in `TiramisuViewModel`:

```kotlin
data class DealWave(
    val cardsDealt: List<Pair<Int, String>>,        // (pileIdx, card) per ogni carta dealata
    val autoFoundationMoves: List<AutoFoundationMove>
)

fun dealAllFromStock(): List<DealWave> {
    val s = state ?: return emptyList()
    if (s.stock.isEmpty()) return emptyList()
    val snapshot = s.deepCopy()   // unico snapshot per l'intera catena
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
        val moves = consumeAutoFoundationMoves()
        waves.add(DealWave(cards, moves))
        // Stop se Obbligato attivo e non risolto da auto-completion
        if (s.difficulty.obbligato && obbligatoTargets().isNotEmpty()) break
    }
    selectedPileIndex = null
    previousState = snapshot
    return waves
}
```

Note:
- `previousState` salvato **una sola volta** all'inizio: l'undo del fast deal riporta allo stato pre-fast-deal (coerente con "1 azione utente = 1 undo").
- Se auto-completion è ON, `autoMoveToFoundation` svuota gli obbligati subito → la condizione di stop non scatta mai e la catena va fino a stock vuoto.
- Se auto-completion è OFF e DIFFICILE attivo, la catena può fermarsi a metà — comportamento desiderato.

### Animazione in `GameActivity`

In `handleStockTap()` (o equivalente):

```kotlin
if (fastDealEnabled && stockHasMultipleWavesRemaining()) {
    val waves = vm.dealAllFromStock()
    isAnimating = true
    animateFastDealChain(waves) { isAnimating = false }
} else {
    // path esistente: una ondata
}
```

`animateFastDealChain` riusa `animateDealWave` (esistente) per ogni `DealWave`, schedulando con `Handler.postDelayed`:

```
T=0:    animateDealWave(waves[0])
T=W:    animateDealWave(waves[1])
T=2W:   animateDealWave(waves[2])
...
T=nW:   onComplete()
```

dove `W = 4 * DEAL_CARD_STAGGER_MS + FAST_DEAL_WAVE_GAP_MS`.

Le auto-foundation moves emesse da ogni ondata vengono animate dopo la fine dell'ondata (riusando la pipeline `consumeAutoFoundationMoves` esistente).

`isAnimating = true` blocca interazioni utente per tutta la catena; rimesso a `false` nel callback finale.

### Suoni

Ogni ondata mantiene il suo `playSound(R.raw.flipcard)` (rispettando il setting Suoni). Una ondata di 4 carte ≈ un singolo "flip" sonoro come oggi.

### `TiramisuSolver.kt`

Commento esistente "mirroring TiramisuViewModel.autoMoveAces()" → "mirroring TiramisuViewModel.autoMoveToFoundation()". Il solver simula solo l'Ace auto-move per la stall detection, NON l'auto-completion completo (l'auto-completion è una preferenza utente, non una regola di gioco — il solver deve giudicare i finali stallo indipendentemente dal setting dell'utente).

---

## 4. File toccati

**Modificati:**

- `app/src/main/java/com/bottazzini/tiramisu/settings/SettingsHandler.kt` — `SOUND_ENABLED` nell'enum + default.
- `app/src/main/java/com/bottazzini/tiramisu/SettingsActivity.kt` — handler `changeSoundEnabled`, lettura in `readConfigurations`.
- `app/src/main/res/layout/settings.xml` — riga switch suoni.
- `app/src/main/res/values*/strings.xml` (×17, escluso `values-night`) — `settings_label_sound`, `sound_with_icon`, testo nuovo per `settings_label_auto_move` / `auto_move_with_icon`, semplificazione `achievement_no_assist_desc` / `achievement_perfectionist_desc`.
- `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt` — rename `autoMoveAces` → `autoMoveToFoundation`, property `autoCompleteEnabled`, rename `AceMove`/`AceSource` → `AutoFoundationMove`/`AutoFoundationSource`, nuovo `dealAllFromStock()` + data class `DealWave`, rename `consumeAutoAceMoves` → `consumeAutoFoundationMoves`, rename campo `_lastAutoAceMoves` → `_lastAutoFoundationMoves`.
- `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt` — campi `soundsEnabled`, `autoCompleteEnabled`, `fastDealEnabled` letti in `onResume`; gate `playSound`; branch fast deal in stockTap; animazione catena; sostituzione riferimenti `AceMove` → `AutoFoundationMove`; rinomino `animateAutoAces(moves: List<AceMove>)` → `animateAutoFoundation(moves: List<AutoFoundationMove>)`; sostituzione `AceSource.STOCK` / `AceSource.PILE_TOP` con `AutoFoundationSource.STOCK` / `AutoFoundationSource.PILE_TOP`.
- `app/src/main/java/com/bottazzini/tiramisu/MainActivity.kt` — gate `playSound`.
- `app/src/main/java/com/bottazzini/tiramisu/YouWonActivity.kt` — gate prima di `MediaPlayer.create(this, R.raw.youwin)`.
- `app/src/main/java/com/bottazzini/tiramisu/SplashActivity.kt` — gate prima di `MediaPlayer.create(this, R.raw.shuffle)` (riga 81 attuale).
- `app/src/main/java/com/bottazzini/tiramisu/utils/TiramisuSolver.kt` — solo aggiornamento commento (riga 102: "mirroring TiramisuViewModel.autoMoveAces()" → "mirroring TiramisuViewModel.autoMoveToFoundation()").
- `app/src/test/java/com/bottazzini/tiramisu/TiramisuViewModelTest.kt` — aggiornamento delle chiamate `consumeAutoAceMoves` → `consumeAutoFoundationMoves` e dei riferimenti `AceSource.STOCK` → `AutoFoundationSource.STOCK` (rename meccanico; le asserzioni esistenti restano valide perché coprono il caso Assi, che è preservato anche con `autoCompleteEnabled = false`).

**NON toccati:**

- `Difficulty.kt`, `TiramisuMoveValidator.kt`, `TiramisuGameState.kt`, `TiramisuTutorialEngine.kt`, `TiramisuTutorialSteps.kt`, `AchievementEngine.kt`, `AchievementCatalog.kt`, `GameStateRepository.kt`, `GameLogRepository.kt`, `RecordsHandler.kt`, `DatabaseHandler.kt`.

Nessuna migration DB: lo schema settings è chiave-valore, le nuove chiavi entrano via `setDefaultSetting` al primo avvio post-update.

---

## 5. Verifica della modifica

### Unit test

1. **`TiramisuViewModel.autoMoveToFoundation` con `autoCompleteEnabled = false`**: stato con 2-of-cups in cima a pile 0, foundation cuori a 1 → l'asso parte (se presente) ma il 2 resta. Regressione zero del comportamento attuale.

2. **`TiramisuViewModel.autoMoveToFoundation` con `autoCompleteEnabled = true`**: stato con 2-of-cups in cima a pile 0, foundation cuori a 1 → il 2 va in fondazione. Loop continua: se sotto al 2 c'è un 3-of-cups, anche il 3 parte.

3. **`TiramisuViewModel.dealAllFromStock` — stock pieno, nessun blocco**: 8 carte in stock, partita su NORMALE → restituisce 2 `DealWave`, stock svuotato, `previousState` = snapshot pre-fast-deal.

4. **`TiramisuViewModel.dealAllFromStock` — Obbligato attivo, auto-completion OFF**: stato DIFFICILE con stock di 8 carte costruito in modo che la 2ª ondata produca un top-pile foundation-able → la chain si ferma a 2 ondate, stock non svuotato, top-pile evidenziato per Obbligato.

5. **`TiramisuViewModel.dealAllFromStock` — Obbligato attivo, auto-completion ON**: stesso stato del test 4 ma `autoCompleteEnabled = true` → la chain procede fino a stock vuoto perché auto-completion risolve l'Obbligato a ogni ondata.

### Test manuali

1. **Setting Suoni**: toggle OFF in `SettingsActivity` → ritorno in `GameActivity`, tap deal: nessun flipcard. Naviga al menu: nessun change_activity. Vinci una partita: schermata vittoria muta. Toggle ON: tutti i suoni tornano.

2. **Setting Completamento automatico OFF**: stato a metà partita con 2-of-cups in cima a pile e foundation pronta → il 2 resta in cima finché l'utente non fa tap (o regola Obbligato in DIFFICILE).

3. **Setting Completamento automatico ON**: stesso stato → il 2 vola da solo in fondazione, e qualsiasi catena successiva (3, 4, ...) parte a cascata.

4. **Setting Fast Deal ON**: stock pieno, tap singolo sul tallone → tutte le ondate si svuotano in ~3-4 secondi con animazione visibile.

5. **Setting Fast Deal ON + Completamento automatico OFF + DIFFICILE**: tap fast deal → catena si ferma alla prima ondata che produce un Obbligato. Top-pile evidenziato.

6. **Persistenza**: chiudi app durante partita, riapri → settings preservati, partita resumed con stesso comportamento.

7. **Undo dopo fast deal**: tap fast deal → undo → stato torna a pre-fast-deal (stock pieno, mazzetti come prima).

---

## 6. Fuori scope

- Rinominare la chiave DB `autoMove` → `autoComplete` (preserva il setting esistente per gli utenti dev attuali).
- Volume granulare / mute separato per categoria.
- Auto-completion che gestisce anche tableau→tableau (solo foundation, come `autoMoveAces` oggi).
- Modifiche al motore achievement (criterio già non legato ad auto-move).
- Animazione di "victory lap" dedicata: con auto-completion ON, l'end-game si automatizza già naturalmente.
- Long-press / tap doppio per fast deal: il comportamento singolo-tap è sufficiente quando il setting è ON.
