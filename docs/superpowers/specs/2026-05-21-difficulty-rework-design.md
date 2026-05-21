# Tiramisù — Difficulty Rework Design Spec
**Data**: 2026-05-21
**Stato**: Da approvare

---

## Panoramica

Il gioco attuale è troppo facile. Questa modifica:

1. Introduce una **regola di movimento più severa** (mazzetto → mazzetto) per i livelli Normale e Difficile.
2. **Ridistribuisce le difficoltà** spostando tutto di un gradino: l'attuale Normale diventa Facile, e il nuovo Difficile rimuove l'ultima ridistribuzione.
3. Aggiorna il **tutorial** affinché insegni la regola severa (il default di Normale) e menzioni che a Facile la regola è più permissiva.
4. **Resetta record e statistiche storiche** al primo avvio della nuova versione, perché il significato delle difficoltà cambia.

---

## 1. Regole del gioco

### Regola larga (Facile — invariata rispetto a oggi)

Mazzetto → Mazzetto: si può spostare la carta in cima se ha lo **stesso seme** della carta in cima al mazzetto di destinazione (qualsiasi valore).

### Regola severa (Normale e Difficile — nuova)

Mazzetto → Mazzetto: si può spostare la carta in cima **solo se**:
- ha lo stesso seme della carta in cima al mazzetto di destinazione, **E**
- ha un valore **strettamente minore** della carta di destinazione.

Esempi (cima destinazione = 6 di coppe):
- ✅ 1, 2, 3, 4, 5 di coppe
- ❌ 6 di coppe (uguale)
- ❌ 7, 8, 9, 10 di coppe (maggiore)
- ❌ qualsiasi carta di bastoni / denari / spade

### Regole invariate a tutti i livelli

- **Mazzetto → Fondazione**: stesso seme, rank = `top + 1`. Asso (rank 1) va in fondazione automaticamente.
- **Mazzetto vuoto**: accetta qualsiasi carta.
- **Distribuzione iniziale**: tap sul tallone → 4 carte, una per mazzetto.
- **Deal dal tallone**: tap → 1 carta a ciascun mazzetto da sinistra a destra.
- **Ridistribuzione**: tallone esaurito → raccoglie le carte dei mazzetti da destra a sinistra senza mescolare. Il contatore scende di 1.
- **Vittoria**: tutte e 4 le fondazioni complete (1→10).

---

## 2. Schema dei livelli di difficoltà

| Livello | Ridistribuzioni | Regola tableau | Obbligato |
|---|---|---|---|
| 🟢 Facile | 1 | larga | no |
| 🟡 Normale | 1 | **severa** | no |
| 🔴 Difficile | **0** | **severa** | sì |

**Mapping vs. versione attuale:**

| Livello attuale | Diventa |
|---|---|
| Facile (2 redeals, larga, no obbligato) | (rimosso) |
| Normale (1 redeal, larga, no obbligato) | 🟢 Facile |
| Difficile (1 redeal, larga, sì obbligato) | (rimosso — la "stretta + obbligato" del nuovo Difficile rimpiazza questa configurazione) |

Il Facile attuale (2 redeals) e il Difficile attuale (regola larga + obbligato) scompaiono — la nuova mappatura ha 3 livelli ortogonali e ben distinti.

**Difficoltà di default in Settings**: rimane `Normale`. Significa che gli utenti partiranno automaticamente con la regola severa.

### Definizione di Obbligato (invariata)

Dopo ogni mossa o deal, il sistema controlla se la cima di qualsiasi mazzetto può andare in fondazione. Se sì → la carta viene evidenziata e ogni altra mossa è bloccata finché non viene spostata in fondazione.

---

## 3. Architettura — modifiche al codice

### `Difficulty.kt`

Aggiungere il campo `strictTableau: Boolean` e rimappare i valori:

```kotlin
enum class Difficulty(
    val key: String,
    val displayName: String,
    val redeals: Int,
    val obbligato: Boolean,
    val strictTableau: Boolean
) {
    FACILE    ("facile",    "🟢 Facile",    redeals = 1, obbligato = false, strictTableau = false),
    NORMALE   ("normale",   "🟡 Normale",   redeals = 1, obbligato = false, strictTableau = true),
    DIFFICILE ("difficile", "🔴 Difficile", redeals = 0, obbligato = true,  strictTableau = true);

    companion object {
        fun fromKey(key: String): Difficulty =
            entries.firstOrNull { it.key == key } ?: NORMALE
    }
}
```

### `TiramisuMoveValidator.canMoveToTableau()`

Aggiunge un parametro `strict: Boolean`:

```kotlin
fun canMoveToTableau(
    movingCard: String,
    destinationTop: String,
    strict: Boolean
): Boolean {
    if (movingCard == "zero") return false
    if (destinationTop == "zero") return true  // empty pile accepts any card
    if (suit(movingCard) != suit(destinationTop)) return false
    return if (strict) {
        rank(movingCard) < rank(destinationTop)
    } else {
        true
    }
}
```

Tutti i call site (ViewModel, Solver, Tutorial Engine) devono passare `state.difficulty.strictTableau`.

### `TiramisuSolver.findHint()`

Passa `strict` derivato da `state.difficulty.strictTableau` a ogni invocazione di `canMoveToTableau`. La priorità (foundation prima, tableau dopo) e la regola "salta singola carta su pila vuota" rimangono invariate.

### `TiramisuViewModel`

Ogni validazione di mossa tableau-to-tableau deve passare il flag `strict` derivato dalla difficoltà corrente.

### `TiramisuGameState`

Nessun campo nuovo: `difficulty: Difficulty` già contiene l'informazione necessaria via `strictTableau`.

### Persistenza partita (`GameStateRepository`)

Nessuna modifica strutturale: la difficoltà è già serializzata nello stato. Al caricamento di una partita salvata la regola corretta viene applicata automaticamente.

---

## 4. Tutorial

Il tutorial scriptato in `TiramisuTutorialSteps` viene aggiornato per insegnare la **regola severa** (default di Normale).

### Modifiche ai passi

- **Passo 3 — "Mossa stesso seme"**: il testo e il mazzo predeterminato vengono adattati per mostrare un esempio valido di **regola severa**. Es: "Puoi spostare il 3 di coppe sul 5 di coppe (stesso seme, valore minore)". Mostra anche un controesempio: "Non puoi spostarlo sul 2 di coppe (valore maggiore)".
- **Nuovo passo finale — "Modalità Facile"**: testo informativo (no mossa richiesta): "A Facile la regola è più permissiva: puoi spostare qualsiasi carta dello stesso seme, indipendentemente dal valore."

Il numero totale di passi passa da 7 a 8. Il tutorial usa lo stesso mazzo deterministico (vincoli del mazzo aggiornati per illustrare la regola severa).

### Classi coinvolte (invariate)

- `TiramisuTutorialSteps` — lista dei passi aggiornata
- `TiramisuTutorialEngine` — nessuna modifica

---

## 5. UI

### Dialog "Nuova partita"

Il dialog di selezione difficoltà aggiorna le descrizioni:

- 🟢 **Facile** — 1 ridistribuzione. Sposta qualsiasi carta dello stesso seme.
- 🟡 **Normale** — 1 ridistribuzione. Sposta carte dello stesso seme solo se di valore minore.
- 🔴 **Difficile** — 0 ridistribuzioni, regola severa, modalità Obbligato attiva.

### Settings → "Livello di difficoltà"

Stesse descrizioni del dialog "Nuova partita".

### Badge difficoltà in GameActivity

Invariato (testo + colore già coprono tutti e 3 i livelli).

---

## 6. Reset record/statistiche

### Strategia

Bump `DB_VERSION` di `DatabaseHandler`. La migration azzera (DROP + recreate) le tabelle:

- `game_log` (storia partite)
- qualsiasi tabella che memorizza record per difficoltà

### Cosa NON viene resettato

- `SharedPreferences` (impostazioni utente: tema, audio, mazzo, difficoltà default)
- Stato del tutorial (visto / non visto)
- Achievement sbloccati (se la tabella esiste in v1.X — confermare in fase di plan)

### Verifica

Il primo avvio della nuova versione deve mostrare `StatsActivity` vuota e `RecordActivity` vuota. Nessun dialog di conferma — il reset è silenzioso (l'app non è ancora pubblicata).

---

## 7. Fuori scope

- Cambiamenti all'engine degli achievement (eventuale rivalutazione di achievement legati alla difficoltà va affrontata separatamente).
- Modifiche ad animazioni, suoni, layout.
- Daily challenge, modalità a tempo, leaderboard online — restano nel wishlist v1.12+.
- Migrazione "intelligente" dei record vecchi: scartata in favore di reset pulito.

---

## 8. Verifica della modifica

Test richiesti prima di considerare il rework completato:

1. **Unit test `TiramisuMoveValidator`** per la regola severa: copertura di tutti i casi (stesso seme + minore = OK; stesso seme + uguale/maggiore = NO; seme diverso = NO; pila vuota = sempre OK).
2. **Unit test `TiramisuSolver`** con stati che richiedono regola severa: verificare che gli hint suggeriti rispettino la nuova regola.
3. **Test manuale** delle 3 difficoltà: partita completa a ciascun livello.
4. **Test manuale tutorial**: percorrere tutti i passi, verificare il nuovo passo finale.
5. **Test manuale reset**: installazione sopra versione precedente → record/stats vuoti, impostazioni preservate.
6. **Test manuale persistenza**: avviare partita a Normale, chiudere app, riaprire → regola severa ancora attiva.
