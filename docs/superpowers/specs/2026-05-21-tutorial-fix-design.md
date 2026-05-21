# Tutorial Fix & Completamento — Design Spec
**Data:** 2026-05-21  
**Stato:** Approvato

---

## Problema

Il tutorial del gioco Tiramisù è rotto e incompleto:

1. **`onStockTapped()` non chiama `advanceTutorial()`** → lo step "tappa il tallone" non avanza mai.
2. **`eng.onMoveExecuted()` mai chiamato** da `GameActivity` → tutti i passi con mossa obbligata restano bloccati (anche se il motore è codificato correttamente).
3. **Step same-suit post-deal invalido** → il passo 3 originale richiedeva pile 1→pile 2, ma dopo la distribuzione pile 2 ha `d3` (denari) e pile 1 ha `c6` (coppe) — move invalida per le regole del gioco.
4. **`highlightPiles` definita ma mai usata nel rendering** → le pile non vengono mai evidenziate.
5. **Tutorial incompleto** → non insegna fondazione e ridistribuzione in modo interattivo.

---

## Obiettivo

Un tutorial completamente funzionante che insegna in 10 passi:
- Distribuire dal tallone
- Spostare carte dello stesso seme tra mazzetti
- Portare una carta alla fondazione
- Usare la ridistribuzione

---

## Architettura

### Nessuna nuova Activity / Fragment

Il tutorial continua a usare `TiramisuTutorialEngine` + `TiramisuTutorialSteps` + overlay in `GameActivity`. Nessuna nuova classe.

---

## Nuovo Mazzo Tutorial

**File:** `TiramisuDeckSetup.kt`, funzione `tutorialDeck()`

```
Distribuzione iniziale (4 carte → pile 0-3):
  "b1", "b2", "c3", "d8"
  → b1 auto-move alla fondazione bastoni → pile 0 = VUOTA
  → pile 1 = b2 (bastoni)
  → pile 2 = c3 (coppe)
  → pile 3 = d8 (denari)

Stock (4 carte — si svuota dopo 1 tap):
  "c7", "c5", "d3", "s4"

Stato dopo deal dal tallone (step 1):
  pile 0 = c7 (coppe)
  pile 1 = c5 (coppe) su b2 (bastoni)
  pile 2 = d3 (denari) su c3 (coppe)
  pile 3 = s4 (spade) su d8 (denari)
  stock  = VUOTO → canRedeal() diventa true
```

**Perché questo mazzo:**
- `c7` (pile 0) e `c5` (pile 1) sono entrambe coppe → same-suit move valida (pile 1 → pile 0)
- Dopo lo spostamento di `c5`, `b2` viene esposta in pile 1
- `b2` è la prossima carta bastoni per la fondazione (che ha già `b1`)
- Lo stock si svuota dopo 1 deal → il pulsante Ridistribuisci appare subito (ma è bloccato dal motore fino allo step apposito)

---

## Sequenza Passi Tutorial

| # | Tipo | String Key | Mossa richiesta | Highlight |
|---|------|-----------|-----------------|-----------|
| 0 | Info | `tut_intro` | — | — |
| 1 | Obbligato | `tut_deal` | `sourcePile=-1, targetPile=-1` (tap tallone) | — |
| 2 | Info | `tut_deal_confirm` | — | — |
| 3 | Obbligato | `tut_same_suit` | `sourcePile=1, targetPile=0` (c5→c7) | piles [0,1] |
| 4 | Info | `tut_same_suit_confirm` | — | — |
| 5 | Obbligato | `tut_foundation` | `sourcePile=1, targetPile=-1` (b2→fondazione) | piles [1] |
| 6 | Info | `tut_foundation_confirm` | — | — |
| 7 | Obbligato | `tut_redeal` | `sourcePile=-1, targetPile=-2` (tap Ridistribuisci) | — |
| 8 | Info | `tut_redeal_confirm` | — | — |
| 9 | Info | `tut_finish` | — | — |

**Convenzione `TiramisuTutorialMove`:**
- `sourcePile = -1, targetPile = -1` → tap tallone
- `sourcePile = -1, targetPile = -2` → tap Ridistribuisci *(nuovo)*
- `sourcePile >= 0, targetPile >= 0` → spostamento pile→pile
- `sourcePile >= 0, targetPile = -1` → spostamento pile→fondazione

---

## Modifiche al Motore (`TiramisuTutorialEngine`)

### Rimosse
- Campo `moveExecuted: Boolean` (dead code — mai settato da GameActivity)
- Metodo `onMoveExecuted(srcPile, dstPile)` (dead code)

### Semplificate
- `advanceToNext()` avanza sempre senza check su `moveExecuted`; la protezione da mosse sbagliate è nei blocchi di `GameActivity`

### Aggiunte
- `isRedealStep(): Boolean` → true se `sourcePile == -1 && targetPile == -2`
- `isCorrectPileMove(src: Int, dst: Int): Boolean` → true se mossa corrisponde allo step corrente (o nessuna restrizione)
- `isCorrectFoundationMove(src: Int): Boolean` → true se il pile src corrisponde allo step corrente

---

## Modifiche a `GameActivity`

### `onStockTapped()`
```kotlin
if (vm.dealFromStock()) {
    playSound(R.raw.flipcard)
    renderAll()
    maybeAnimateAutoAces()
    checkWin()
    checkLost()
    if (isTutorialMode) advanceTutorial()   // ← aggiunto
}
```

### `onRedealTapped()`
```kotlin
private fun onRedealTapped() {
    if (isAnimating) return
    if (isTutorialMode) {
        val eng = tutorialEngine ?: return
        if (!eng.isRedealStep()) return       // ← blocca se non è lo step giusto
    }
    if (!vm.canRedeal()) return
    animateRedeal()
}
```

### `animateRedeal()` — callback di completamento
Aggiungere `if (isTutorialMode) advanceTutorial()` nel `postDelayed` di cleanup (dopo che l'animazione finisce e `renderAll()` è chiamata).

### `handlePileDrop(src, dst)`
```kotlin
if (isTutorialMode) {
    val eng = tutorialEngine ?: return false
    if (!eng.isCorrectPileMove(src, dst)) {
        showInvalidMoveToast(); return false
    }
}
```

### `handleFoundationDrop(src)`
```kotlin
if (isTutorialMode) {
    val eng = tutorialEngine ?: return false
    if (!eng.isCorrectFoundationMove(src)) {
        showInvalidMoveToast(); return false
    }
}
```

### `renderPile()` — evidenziazione tutorial
Aggiungere controllo `isTutorialHighlight`:
```kotlin
val isTutorialHighlight = isTutorialMode &&
    (tutorialEngine?.currentStep()?.highlightPiles?.contains(pileIdx) == true)
```
Nella fase di rendering dell'ultima carta della pila:
```kotlin
isTutorialHighlight -> imageView.setColorFilter(0x880000FF.toInt(), PorterDuff.Mode.SRC_ATOP)
```

---

## Modifiche alle Stringhe (`strings.xml`)

Aggiornare/aggiungere:

| Key | Testo |
|-----|-------|
| `tut_intro` | "Benvenuto in Tiramisù! Obiettivo: porta tutte le carte alle 4 basi, per seme dall'Asso al 10." |
| `tut_deal` | "Tocca il Tallone in basso per distribuire le carte." |
| `tut_deal_confirm` | "✓ Distribuito! L'Asso è andato automaticamente alla base bastoni." |
| `tut_same_suit` | "Carte dello stesso seme si spostano liberamente tra mazzetti. Sposta la carta evidenziata sul mazzetto vicino." |
| `tut_same_suit_confirm` | "✓ Ottimo! Stesso seme, qualsiasi valore." |
| `tut_foundation` | "Quella carta può andare alla base! Toccala per selezionarla, poi tocca la base in alto." |
| `tut_foundation_confirm` | "✓ Perfetto! Porta tutte le carte alle basi per vincere." |
| `tut_redeal` | "Lo stock è vuoto. Tocca il pulsante Ridistribuisci per raccogliere i mazzetti e continuare!" |
| `tut_redeal_confirm` | "✓ Puoi ridistribuire un numero limitato di volte, in base alla difficoltà." |
| `tut_finish` | "Bravo! Ora conosci le regole di Tiramisù. Buon gioco! 🎉" |

---

## File Modificati

| File | Tipo di modifica |
|------|-----------------|
| `utils/TiramisuDeckSetup.kt` | Nuovo mazzo tutorial (8 carte) |
| `utils/TiramisuTutorialSteps.kt` | Nuova sequenza 10 passi, convention redeal |
| `utils/TiramisuTutorialEngine.kt` | Rimozione dead code, aggiunta metodi helper |
| `GameActivity.kt` | Fix chiamate mancanti, highlight pile, blocco redeal |
| `res/values/strings.xml` | Aggiornamento stringhe tutorial |

---

## Testing

- Step 0: tappare Avanti avanza → step 1
- Step 1: tappare tallone → deal avviene → tutorial avanza a step 2
- Step 1: tappare pila durante step 1 → bloccato
- Step 3: tappare pile 1 (fonte) poi pile 0 (dest) → move valida → avanza a step 4
- Step 3: drag pile 1 → pile 2 (mossa sbagliata) → toast errore, non avanza
- Step 5: tappare pile 1 → selezionata → tappare fondazione → avanza a step 6
- Step 7: tappare Ridistribuisci → animazione → avanza a step 8
- Step 7: tappare Ridistribuisci prima dello step 7 → bloccato
- Step 9: tappare Avanti → `endTutorial()` → toast "Tutorial completato! Buon gioco!"
