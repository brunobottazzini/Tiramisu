# Tiramisù — Game Animations Design Spec

**Data**: 2026-05-21
**Stato**: Approvato (concept) — pronto per writing-plans

---

## 1. Scope

Due animazioni complementari sulla `GameActivity`:

1. **Redeal animation** — quando l'utente tap **Ridistribuisci**, le carte volano dai mazzetti al tallone, una pila alla volta in ordine `3 → 2 → 1 → 0`.
2. **Auto-ace animation** — ogni volta che `autoMoveAces()` muove uno o più assi automaticamente verso una fondazione, gli assi volano dalla loro posizione di partenza (tallone se l'azione era un deal-from-stock, oppure top della pila in altri casi) alla fondazione di destinazione.

Entrambe usano ghost `ImageView` aggiunti dinamicamente al `gameRoot` (`ConstraintLayout`) per evitare il clipping dei `ScrollView` delle pile, e `ObjectAnimator` su `translationX/Y` su unità di tempo di Android (ms).

**Fuori scope** (YAGNI):
- Animazione di `dealFromStock` (l'utente l'ha esplicitamente esclusa: "il resto si fa a mano e va bene così")
- Animazione di drag/drop (il drag già usa `DragShadowBuilder` nativo che è suo proprio feedback visivo)
- Animazione di vittoria/sconfitta (gli activity dedicati sono fuori scope)
- Effetti particle/glow/sound aggiuntivi per ogni carta

---

## 2. Timing tokens (costanti)

Centralizzate in un `companion object` di `GameActivity.kt`:

| Costante | Valore | Uso |
|---|---|---|
| `REDEAL_CARD_DURATION_MS` | 200L | durata animazione singola carta nel redeal |
| `REDEAL_CARD_STAGGER_MS` | 60L | offset di partenza tra carte della stessa pila |
| `REDEAL_PILE_GAP_MS` | 150L | pausa addizionale tra l'ultima carta di una pila e la prima della successiva |
| `ACE_DURATION_MS` | 250L | durata animazione singolo asso |
| `ACE_STAGGER_MS` | 80L | offset di partenza tra assi multipli |

---

## 3. Animazione 1 — Redeal

### 3.1 Trigger

`onRedealTapped()` in `GameActivity.kt`. Sostituisce l'implementazione attuale (instant) con la versione animata.

### 3.2 Flusso

1. Lock UI: `isAnimating = true`, disabilita interazioni
2. Cattura coord schermo di `stockArea` + di OGNI ImageView carta in ognuna delle 4 pile (sorted: pile 3 prima, all'interno di ogni pila dal top — `last()` — fino al bottom)
3. Per ogni carta: crea un ghost ImageView su `gameRoot`, posizionato (`translationX/Y`) alle coordinate dell'originale, drawable identico, dimensioni identiche
4. Carte originali → alpha 0 (sparite ma layout invariato)
5. `vm.redeal()` — state muta (pile vuote, stock pieno, redealsLeft--)
6. NON `renderAll()` ancora
7. Per ogni ghost in ordine (pile3 carta1, pile3 carta2, ..., pile2 carta1, ...): `ObjectAnimator.ofPropertyValuesHolder()` su translationX + translationY verso le coord di `stockArea`, duration `REDEAL_CARD_DURATION_MS`, `startDelay` cumulativo
8. End-callback dell'ultimo ghost: rimuovi tutti i ghost da `gameRoot`, `renderAll()`, `isAnimating = false`

### 3.3 Calcolo dello startDelay cumulativo

```
delay = 0
per ogni pile P in [3,2,1,0]:
  per ogni card C in pile P (top → bottom):
    ghost[P,C].startDelay = delay
    delay += REDEAL_CARD_STAGGER_MS
  // dopo l'ultima carta di una pila, aggiungi REDEAL_PILE_GAP_MS
  // (il pile gap si somma allo stagger già accumulato, non lo sostituisce)
  delay += REDEAL_PILE_GAP_MS
```

Totale animazione = `(N_carte_totali × REDEAL_CARD_STAGGER_MS) + (3 × REDEAL_PILE_GAP_MS) + REDEAL_CARD_DURATION_MS`. Esempio con 18 carte totali: `18×60 + 3×150 + 200 = 1730ms`.

### 3.4 Suono

`playSound(R.raw.flipcard)` invocato una volta al passo 1 (come oggi). Niente suoni per-card.

### 3.5 Edge case redeal

- Una o più pile vuote: skip (zero ghost per quella pila, niente gap aggiuntivo)
- Tutte le pile vuote: `canRedeal()` ritorna false a monte (perché il redeal richiede `stock.isEmpty()` MA non controlla le pile — in realtà se tutte le pile sono vuote dopo aver svuotato il tallone, lo stato è di vittoria o stallo, non redeal). Comunque difensivamente: se la cattura coord ritorna 0 ghosts, salta direttamente al `renderAll()` finale per evitare lock UI senza animazione

---

## 4. Animazione 2 — Auto-ace

### 4.1 Cambio API ViewModel

Nuovo dataclass in `TiramisuViewModel.kt`:

```kotlin
enum class AceSource { STOCK, PILE_TOP }
data class AceMove(
    val fromPile: Int,           // pila da cui l'asso è uscito
    val toFoundation: Int,       // fondazione di destinazione
    val card: String,            // es. "b1"
    val source: AceSource        // origine visiva per l'animazione
)
```

Nuovo campo:

```kotlin
private var _lastAutoAceMoves: List<AceMove> = emptyList()
val lastAutoAceMoves: List<AceMove> get() = _lastAutoAceMoves
```

`autoMoveAces()` diventa parametrizzato:

```kotlin
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

Aggiornamento dei 3 chiamanti:
- `dealFromStock()`: `autoMoveAces(AceSource.STOCK)`
- `onFoundationTapped()`: `autoMoveAces(AceSource.PILE_TOP)`
- `movePileToPile()`: `autoMoveAces(AceSource.PILE_TOP)`

Reset `_lastAutoAceMoves = emptyList()` all'inizio di `newGame`, `newTutorialGame`, `restoreState`, `retrySameGame`, `undo`.

### 4.2 GameActivity hook

Dopo ogni `renderAll()` chiamato da una user action che può aver invocato `autoMoveAces`, controlla `vm.lastAutoAceMoves`. Se non vuoto, lancia `animateAutoAces(moves)`.

Punti hook (modifiche post-`renderAll()`, prima di `checkWin/checkLost`):
- `onStockTapped` (after `dealFromStock`)
- `onPileCardTapped` (TapResult.MOVED branch)
- `onFoundationViewTapped` (after `onFoundationTapped`)
- `handlePileDrop` (drag → pile move)
- `handleFoundationDrop` (drag → foundation move)

NON in `onRedealTapped` (redeal non triggera autoMoveAces). NON in `onUndoTapped` (undo ripristina state precedente, `_lastAutoAceMoves` viene resettato a empty).

**Ordering**: `maybeAnimateAutoAces()` viene chiamato PRIMA di `checkWin()/checkLost()`. La dialog/transition di end-game arriva dopo. Conseguenza: se la mossa finale che vince/perde include un auto-ace move, l'animazione parte ma la dialog appare subito coprendola visivamente (limite noto v1, accettabile perché edge case raro). Per evitare del tutto: gli `checkWin/checkLost` andrebbero spostati nel end-callback dell'animazione — task possibile in follow-up se l'utente lo segnala.

### 4.3 Flusso animazione auto-ace

1. Lock UI: `isAnimating = true`
2. Per ogni `AceMove m` in `vm.lastAutoAceMoves`:
   - Calcola source coord schermo:
     - `m.source == STOCK` → `stockArea.locationOnScreen`
     - `m.source == PILE_TOP` → `pileContainers[m.fromPile]`'s last child ImageView screen position (= dove l'asso era prima di sparire, ora c'è la carta sotto)
   - Calcola dest coord schermo: `foundationViews[m.toFoundation].locationOnScreen`
   - Hide la carta nella fondazione destination: `foundationViews[m.toFoundation].alpha = 0f`
   - Crea ghost ImageView su `gameRoot` con drawable di `m.card` (via `${cardType}_${m.card}`), dimensioni = `foundationViews[m.toFoundation].width × .height`, posizione iniziale = source
   - `ObjectAnimator` translationX da `sourceX` a `destX`, translationY analogamente, duration `ACE_DURATION_MS`, startDelay = `index × ACE_STAGGER_MS`
3. End callback ultimo ghost:
   - Rimuovi tutti i ghost
   - Ripristina alpha 1 su tutte le `foundationViews` destinations
   - `isAnimating = false`

### 4.4 Reset di `_lastAutoAceMoves`

Dopo che GameActivity ha consumato la lista, va resettata a empty. Pattern: `vm.consumeAutoAceMoves(): List<AceMove>` che ritorna la lista e svuota internal state. Evita di rianimare gli stessi assi su un secondo `renderAll`.

Implementazione in ViewModel:

```kotlin
fun consumeAutoAceMoves(): List<AceMove> {
    val moves = _lastAutoAceMoves
    _lastAutoAceMoves = emptyList()
    return moves
}
```

GameActivity:

```kotlin
private fun maybeAnimateAutoAces() {
    val moves = vm.consumeAutoAceMoves()
    if (moves.isEmpty()) return
    animateAutoAces(moves)
}
```

### 4.5 Edge case auto-ace

- `lastAutoAceMoves.isEmpty()`: il maybe-helper non fa nulla, niente lock UI
- Più aces simultanei (rarissimo: max 4): tutti animati con stagger 80ms tra loro, tempo totale `~570ms`
- `cardType` cambiato durante un'animazione: improbabile in pratica; il ghost viene creato al momento e mantiene il suo drawable. Nessun bug.

---

## 5. UI lock condiviso (isAnimating)

Singolo flag boolean privato in `GameActivity.kt`:

```kotlin
private var isAnimating: Boolean = false
```

Quando `true`, le user action vengono bloccate a monte:

```kotlin
private fun onStockTapped() {
    if (isAnimating) return
    // ... rest
}
```

Stessa guard in: `onRedealTapped`, `onPileCardTapped`, `onFoundationViewTapped`, `onHintTapped`, `onUndoTapped`, `handlePileDrop`, `handleFoundationDrop`, `startCardDrag`.

`btnMenu` (pausa) NON bloccato — l'utente deve poter sempre andare in pausa anche durante animazione.

---

## 6. Test plan manuale

- Tap "Ridistribuisci" → vedi le carte volare dai 4 mazzetti al tallone in ordine 3→2→1→0
- Tap stock dopo redeal → se appare un asso, viene visivamente trascinato dal tallone alla fondazione corrispondente (animazione asso)
- Muovi una carta che scopre un asso sotto → l'asso vola dalla sua pila alla fondazione
- Durante un'animazione, tap su altri bottoni / drag carte → niente succede (UI lock funziona)
- Tap btnMenu durante animazione → menu Pausa si apre comunque (eccezione corretta)
- Win/Lose: condizioni continuano a funzionare (animazione non interferisce con `checkWin`/`checkLost` che sono chiamati AFTER `renderAll` ma prima di `maybeAnimateAutoAces`; se vinto/perso durante animazione, dialog mostrato dopo che lo state si stabilizza)

---

## 7. File modificati

| File | Cambiamento |
|---|---|
| `app/src/main/java/com/bottazzini/tiramisu/TiramisuViewModel.kt` | nuovo dataclass `AceMove`, enum `AceSource`, field `_lastAutoAceMoves`, `consumeAutoAceMoves()`, autoMoveAces parametrizzato |
| `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt` | costanti animation timing, `isAnimating` flag + guards, `onRedealTapped` riscritto con `animateRedeal()`, `maybeAnimateAutoAces()` chiamato dopo renderAll nei 5 punti hook |

Nessuna modifica a layout XML, drawables, manifest, gradle, theme, strings.

---

## 8. Rollback

Tutte le modifiche sono in 2 file Kotlin. Revert dei commit ripristina lo stato precedente. Niente migrazione di dati, niente schema changes.
