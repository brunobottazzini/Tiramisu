# Tiramisù — Cycle/Stall Detection Design Spec
**Data**: 2026-05-22
**Stato**: Approvato

---

## Panoramica

Sotto la nuova regola severa (vedi `2026-05-21-difficulty-rework-design.md`), il giocatore può finire in stalli ciclici: ci sono mosse legali (es. `c3` ↔ `c5`) ma esse non portano mai a nuove carte in fondazione. Il rilevamento attuale di `isLost()` controlla `TiramisuSolver.findHint() == null`, che è `false` durante i cicli — quindi il dialog "Hai perso" non viene mai mostrato e il giocatore è obbligato a iniziare manualmente una nuova partita.

Questa modifica sostituisce il check con un'analisi semantica: *esiste una sequenza di mosse di lunghezza ≤ N che porta una carta in più in fondazione?*

---

## 1. Definizione formale di stallo

Lo stato S è considerato **in stallo** se e solo se:
1. `s.stock` è vuoto, **E**
2. Nessuna ridistribuzione disponibile (`!canRedeal()`), **E**
3. **Non esiste** alcuna sequenza di mosse legali di lunghezza ≤ `MAX_LOOKAHEAD` che produca uno stato S' con `foundationCardCount(S') > foundationCardCount(S)`.

`foundationCardCount(s)` = somma dei rank delle 4 cime di fondazione (carte vuote = 0). Es: fondazioni `[b3, c1, "zero", s10]` → `3+1+0+10 = 14`.

Le condizioni 1-2 sono pre-requisito (vale già oggi). La condizione 3 è il nuovo check, che sostituisce `findHint() == null`.

`MAX_LOOKAHEAD = 30` (vedi §3).

---

## 2. Algoritmo

BFS sul grafo degli stati raggiungibili, con `visited` set per evitare ricicli:

```
fun canProgress(start, maxDepth):
    initial = foundationCardCount(start)
    visited = { canonicalKey(start) }
    queue   = [(start, 0)]

    while queue not empty:
        (s, depth) = queue.removeFirst()
        if depth >= maxDepth: continue
        for move in enumerateLegalMoves(s):
            next = applyMove(s, move)
            if foundationCardCount(next) > initial:
                return true                # progress! not stalled
            key = canonicalKey(next)
            if visited.add(key):           # only enqueue unseen states
                queue.add((next, depth + 1))
    return false                           # exhausted lookahead with no progress
```

### Mosse legali enumerate

Per ogni stato S, `enumerateLegalMoves(s)` produce:
- **Foundation moves**: ogni cima di pila che soddisfa `canMoveToFoundation` rispetto a una qualsiasi fondazione.
- **Tableau-to-tableau moves**: ogni cima di pila che soddisfa `canMoveToTableau(strict = s.difficulty.strictTableau)` rispetto a una qualsiasi altra pila — **anche** mosse "single card → empty pile" (`findHint` le skippa come hint inutile, ma nel BFS sono potenzialmente utili per sbloccare altre mosse).

**Esclusioni**: `stock`/`redeal` non sono mosse legali da considerare nel BFS, perché il check viene fatto solo quando `stock` è vuoto e `!canRedeal()`.

### Auto-mosse dell'Asso

Quando una mossa scopre un asso (rank 1) in cima a una pila, l'engine reale lo sposta automaticamente alla fondazione. Nel BFS dobbiamo simulare questo: dopo ogni `applyMove`, eseguire `autoMoveAces` finché nessun asso resta in cima.

### Canonical key

Per il `visited` set serializziamo solo i campi rilevanti per la solvibilità:
```
piles[0].joinToString(",") + "|" +
piles[1].joinToString(",") + "|" +
piles[2].joinToString(",") + "|" +
piles[3].joinToString(",") + ";" +
foundations.joinToString(",")
```
`stock`, `redealsLeft`, `difficulty`, timer e `initialDeck` non entrano nel hash perché sono invarianti durante il BFS (lo stock è vuoto, niente redeal, difficoltà costante).

Le pile sono distinguibili per indice — non normalizziamo l'ordine (semplicità + corrispondenza 1:1 con l'engine reale).

---

## 3. Scelta di `MAX_LOOKAHEAD`

Bilanciamento tra correttezza (no falsi positivi) e performance.

| Lookahead | Falsi positivi attesi | Tempo nel caso peggiore |
|---|---|---|
| 10 | Possibili (sequenze "oneste" lunghe esistono) | ~5 ms |
| 20 | Molto rari | ~20 ms |
| **30** | **Praticamente zero su Tiramisù** | **~50-80 ms** |
| 50 | Zero | ~200 ms (rischio percepibile UX) |

Scegliamo **30** come default con margine. La costante è in `TiramisuSolver` ed è facile da aumentare in futuro se compaiono casi limite.

**Razionale**: in un solitario a 4 pile con 40 carte, ogni progresso "onesto" della fondazione richiede tipicamente <10 mosse (devi solo portare la prossima carta del seme in cima a una pila). 30 step coprono ampiamente i casi peggiori legittimi.

---

## 4. Integrazione

### `TiramisuViewModel.isLost()` (modificato)

```kotlin
fun isLost(): Boolean {
    val s = state ?: return false
    if (s.isWon()) return false
    if (s.stock.isNotEmpty()) return false
    if (canRedeal()) return false
    return !TiramisuSolver.canProgress(s, MAX_LOOKAHEAD)
}
```

`MAX_LOOKAHEAD` è un'estensione di `TiramisuSolver` (costante interna).

### `TiramisuSolver.canProgress` (nuovo)

Vedi §2 per l'algoritmo. Esposto come funzione pubblica per testabilità.

### Helper privati esposti (per test)

`enumerateLegalMoves(state)` e `applyMove(state, move)` rimangono `internal` o `private` ma devono essere testabili. Strategia: `internal` con `@VisibleForTesting`, oppure resi `public` se è troppo invasivo.

---

## 5. Test anti-falso-positivo

Tutti i test in `TiramisuSolverTest`. Convenzioni:
- Stato impostato con il helper `state()` esistente.
- Default difficulty in `state()` è `NORMALE` (strict).
- Default lookahead nei test = 30 dove non diversamente specificato.

### Test "progresso rilevato" (devono ritornare `canProgress = true`)

1. **`canProgress true when foundation move available immediately`**
   - Setup: foundation `[b1, "zero", "zero", "zero"]`, pile 0 = `b2`. Una mossa porta b2 in fondazione.
   - Asserzione: `canProgress(s, 30) == true`.

2. **`canProgress true when ace is reachable in 2 moves`**
   - Setup: pile 0 = `[c1, c5]`, pile 1 = `c7`. c5 si sposta su c7 (5<7 strict-valid) → c1 in cima → asso in fondazione.
   - Asserzione: `canProgress(s, 30) == true`. Anche con `maxDepth = 2` deve trovarlo.

3. **`canProgress true when sequence of 5 tableau moves leads to foundation`**
   - Setup: stato costruito ad-hoc dove servono 5 mosse intermedie prima di poter mettere una carta in fondazione.
   - Asserzione: `canProgress(s, 30) == true`. Verifica che il BFS naviga sequenze multi-step.

4. **`canProgress true at boundary depth N-1`**
   - Setup: stato dove il primo progresso fondazione richiede esattamente `maxDepth - 1` mosse.
   - Asserzione: `canProgress(s, maxDepth) == true`. Verifica che la condizione di terminazione `depth >= maxDepth` è inclusiva del confine.

5. **`canProgress true when single card to empty pile unblocks a foundation move`**
   - Setup: pile 0 = `s5`, pile 1 = `[s1, s3]`, pile 2 vuota, pile 3 = vuota, foundation tutte vuote. Per portare s1 in fondazione: muovere s3 su pile vuota (single → empty), poi s1 in cima e in fondazione.
   - Asserzione: `canProgress(s, 30) == true`. Verifica che le mosse "single → empty" sono considerate.

6. **`canProgress true when foundation already at rank 9 and 10 reachable in few moves`**
   - Setup: foundation `[b9, "zero", "zero", "zero"]`, pile 0 = `b10` direttamente, oppure coperta.
   - Asserzione: `canProgress(s, 30) == true`.

### Test "stallo rilevato" (devono ritornare `canProgress = false`)

7. **`no progress in classic 2-pile cycle (c3 c5 strict)`**
   - Setup: pile 0 = `c3`, pile 1 = `c5`, pile 2 = vuota, pile 3 = vuota, foundation tutte vuote, stock vuoto, redeal 0.
   - Asserzione: `canProgress(s, 30) == false`. Il classico ciclo segnalato dall'utente.

8. **`no progress when no aces reachable and stock empty`**
   - Setup: stato dove tutti gli assi sono sepolti sotto carte più grandi che non possono essere spostate.
   - Asserzione: `canProgress(s, 30) == false`.

9. **`no progress when foundation full and remaining cards lower rank`**
   - Setup: foundation `[b5, c3, "zero", "zero"]`, pile cards = `[b2, b3, c1, c2]` o simili (carte sotto i livelli di fondazione raggiunti).
   - Asserzione: `canProgress(s, 30) == false`.

### Test boundary / correctness

10. **`canProgress respects strict rule when difficulty is NORMALE`**
    - Setup: stato dove sotto lax esiste una sequenza progressiva ma sotto strict no.
    - Asserzione: con `difficulty = NORMALE` ritorna `false`, con `difficulty = FACILE` ritorna `true`. Verifica che il BFS usa la regola corretta.

11. **`canProgress terminates within 100ms on dense state`**
    - Setup: stato "ricco" (molte mosse possibili a ogni step) — es. 4 pile con 5-7 carte ciascuna.
    - Asserzione: durata `canProgress(s, 30)` < 100 ms. Performance regression test.

12. **`canProgress true on freshly dealt initial state`**
    - Setup: stato standard di inizio partita (`newGame`), poi una distribuzione iniziale.
    - Asserzione: `canProgress(s, 30) == true`. Sanity check.

### Test sull'integrazione `isLost()`

13. **`isLost true when in classic cycle stall`** (in `TiramisuViewModelTest`)
    - Setup: stesso scenario del test #7 ma applicato attraverso il ViewModel.
    - Asserzione: `vm.isLost() == true`.

14. **`isLost false when foundation move possible despite cycle alternative`**
    - Setup: stato dove `findHint` potrebbe suggerire una mossa ciclica MA c'è anche una mossa fondazione (es. asso scoperto).
    - Asserzione: `vm.isLost() == false`. Importante per evitare il falso positivo in cui l'utente *può* progredire.

---

## 6. Performance e fallback

- **Costo per chiamata**: BFS limitato a 30 livelli. Caso peggiore stimato sotto 100ms su un Pixel medio. La chiamata avviene dopo ogni mossa utente quindi è in un percorso interattivo, ma 100ms è sotto la soglia di percezione UX (50ms target, 100ms accettabile).
- **Caso peggiore**: stato con ~16 mosse legali per livello × 30 livelli con deduping = qualche migliaio di stati nel `visited` set. Memoria minimale.
- **Test #11** garantisce regressione di performance.

In caso di problemi di performance percepibili sul device, possibilità futura:
- Aumentare `MAX_LOOKAHEAD` solo "su richiesta" (es. quando `findHint == null`) — alternativa: fare `findHint` prima, se != null fai canProgress, altrimenti perso direttamente.
- Spostare il check su un Coroutine in background.

Non implementiamo queste ottimizzazioni adesso (YAGNI).

---

## 7. Fuori scope

- Auto-mossa "Mi arrendo" pulsante UI — non richiesto. Il rilevamento automatico è la soluzione voluta.
- Aumento dinamico di `MAX_LOOKAHEAD` o fallback a euristiche — YAGNI.
- Diagnostica per il giocatore (es. "il gioco è in stallo, vuoi continuare a provare?") — non richiesto.

---

## 8. Verifica

1. Tutti i nuovi unit test passano (`./gradlew :app:testDebugUnitTest`).
2. Tutti i test esistenti continuano a passare (no regressione).
3. Build APK passa (`./gradlew :app:assembleDebug`).
4. Test manuale: provocare uno stallo ciclico sotto Difficile, verificare che il dialog "Hai perso" appaia entro 1 mossa dall'ingresso nel ciclo.
