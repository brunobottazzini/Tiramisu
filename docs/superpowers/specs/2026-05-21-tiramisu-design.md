# Tiramisù — Design Spec
**Data**: 2026-05-21  
**Stato**: Approvato  

---

## Panoramica

Nuova app Android chiamata **Tiramisù** — un solitario con carte italiane. Il progetto prende come scheletro l'app Trasloco (hard fork), riutilizzando tutti gli assets, l'infrastruttura UI e il database, ma sostituendo completamente il motore di gioco.

---

## 1. Struttura del progetto

Approccio: **hard fork** da Trasloco. Il progetto Trasloco rimane intatto.

| Cosa | Da | A |
|---|---|---|
| Cartella progetto | `/Progetti/Trasloco/` | `/Progetti/Tiramisu/` |
| Package Android | `com.bottazzini.trasloco` | `com.bottazzini.tiramisu` |
| App name (strings.xml) | `"Trasloco"` | `"Tiramisù"` |
| App ID (build.gradle) | `com.bottazzini.trasloco` | `com.bottazzini.tiramisu` |
| Directory sorgenti | `.../trasloco/` | `.../tiramisu/` |

Il repo git verrà creato dall'utente dopo la prima versione funzionante.

---

## 2. Regole del gioco

### Setup
- Mazzo italiano standard da 40 carte (4 semi × 10 valori: 1–10)
- 4 **Mazzetti** (tableau) inizialmente vuoti
- 1 **Tallone** (stock) con tutte le 40 carte mescolate
- 4 **Basi** (fondazioni) vuote

### Distribuzione iniziale
Il giocatore tocca il tallone per iniziare: vengono distribuite 4 carte, 1 per ciascun mazzetto.

### Movimenti validi
- **Mazzetto → Mazzetto**: si può spostare la carta in cima se ha lo **stesso seme** della carta in cima al mazzetto di destinazione (qualsiasi valore).
- **Mazzetto → Base**: la carta va in fondazione se è lo stesso seme e il valore è esattamente quello successivo (costruzione ascendente 1→10). Gli Assi vanno automaticamente in fondazione.
- **Mazzetto vuoto**: accetta qualsiasi carta.

### Deal dal tallone
Tap sul tallone → distribuisce 1 carta a ciascun mazzetto (da sinistra a destra). Se il tallone è esaurito e sono rimaste ridistribuzioni, compare il bottone "Ridistribuisci".

### Ridistribuzione
Quando il tallone è vuoto: raccoglie le carte dei mazzetti da **destra a sinistra** senza mescolare → forma un nuovo tallone. Il contatore delle ridistribuzioni scende di 1.

### Vittoria
Tutte e 4 le basi complete con tutte le 10 carte del proprio seme (1→10).

---

## 3. Livelli di difficoltà

| Livello | Ridistribuzioni | Regola Obbligato |
|---|---|---|
| 🟢 Facile | 2 | No |
| 🟡 Normale | 1 | No |
| 🔴 Difficile | 1 | Sì — se una carta può andare in fondazione, devi spostarla prima di fare qualsiasi altra mossa |

La difficoltà si sceglie in due modi:
1. **Prima di iniziare una partita** — dialog di selezione al tap su "Nuova partita"
2. **Nelle Impostazioni** (`SettingsActivity`) — voce "Livello di difficoltà" che cambia il default per le partite future

Se si cambia la difficoltà nelle impostazioni mentre una partita è in corso, la modifica ha effetto dalla partita successiva (quella in corso mantiene la difficoltà con cui è stata iniziata).

La difficoltà corrente è sempre visibile nel badge in alto a destra nella GameActivity. È salvata nel GameLog per le statistiche.

---

## 4. Layout del gioco (GameActivity)

Layout verticale (portrait):

```
┌──────────────────────────────┐
│  ⏱️ 00:42          [DIFF]    │  ← timer + badge difficoltà
├──────────────────────────────┤
│  [B♠] [B♣] [B♦] [B♥]        │  ← 4 Basi (fondazioni)
├──────────────────────────────┤
│                              │
│  ┌──┐ ┌──┐ ┌──┐ ┌──┐        │
│  │c5│ │b3│ │d7│ │s1│        │  ← 4 Mazzetti
│  │c3│ │b9│ │d2│             │     carte impilate con offset verticale
│  │c8│ │b1│                  │     solo la carta in cima è interattiva
│  └──┘                       │
│                              │
├──────────────────────────────┤
│  [TALLONE 28]  [💡 Suggerisci]│  ← stock con contatore + hint
└──────────────────────────────┘
```

- **Mazzetti**: carte impilate con piccolo offset verticale — si vede il seme di ogni carta sotto. Solo la carta in cima è selezionabile.
- **Fondazioni**: mostrano la carta più alta del seme, o uno slot vuoto con simbolo del seme.
- **Tallone**: mostra il dorso + numero di carte rimanenti. Tap per distribuire.
- **Difficile / Obbligato**: le carte che devono obbligatoriamente andare in fondazione si evidenziano (bordo colorato).

### Accessibilità
- Tutti i testi usano **sp** (scalano con le impostazioni di sistema)
- Aree di tocco minimo **48×48 dp** per ogni carta e controllo
- Ogni carta ha una **content description** per TalkBack (es. "Cinque di coppe")
- Etichette non si sovrappongono mai alle carte, anche con font XXL
- Temi compatibili con high-contrast mode

---

## 5. Architettura — classi nuove

Queste classi sostituiscono la logica di gioco di Trasloco:

| Classe | Responsabilità |
|---|---|
| `Difficulty` (enum) | `FACILE(redeals=2)`, `NORMALE(redeals=1)`, `DIFFICILE(redeals=1, obbligato=true)` |
| `TiramisuMoveValidator` | Valida mosse: stesso seme per tableau-to-tableau; ordine ascendente per fondazione |
| `TiramisuDeckSetup` | Mescola 40 carte → 1 tallone; nessun sub-deck |
| `TiramisuGameState` | Stato completo: `piles[4]`, `stock: List<String>`, `foundations[4]`, `redealsLeft: Int`, `difficulty: Difficulty` |
| `TiramisuViewModel` | Gestisce mosse, deal, ridistribuzione, timer, selezione carta, hint |
| `TiramisuSolver` | BFS/DFS leggero per trovare mosse disponibili (usato da hint engine) |

### Logica deal (pseudocodice)
```
fun onTalloneTap():
    if stock.isNotEmpty():
        for i in 0..3:
            piles[i].push(stock.removeFirst())
    else if redealsLeft > 0:
        showRedealButton()

fun onRedeal():
    stock = (piles[3] + piles[2] + piles[1] + piles[0]).flatten()
    redealsLeft--
```

### Modalità Difficile (Obbligato)
Dopo ogni mossa o deal, il sistema controlla se la cima di qualsiasi mazzetto può andare in fondazione. Se sì → quella carta viene evidenziata e le altre mosse sono bloccate finché non viene spostata in fondazione.

---

## 6. Statistiche e salvataggio

### GameLog (campo aggiuntivo rispetto a Trasloco)

| Campo | Tipo | Note |
|---|---|---|
| `won` | Boolean | partita vinta o persa |
| `duration_ms` | Long | durata totale |
| `difficulty` | String | `"facile"` / `"normale"` / `"difficile"` |
| `redeals_used` | Int | quante ridistribuzioni usate (0, 1 o 2) |
| `timestamp` | Long | data/ora inizio partita |

### StatsActivity — record mostrati
- Partite giocate / vinte / % vittoria — **per livello di difficoltà**
- Tempo migliore — per livello
- Serie consecutiva di vittorie
- Media durata partite vinte

### GameStateRepository — stato salvato
- Stato dei 4 mazzetti (ordine completo delle carte)
- Stato delle 4 fondazioni (carta più alta per seme)
- Tallone rimanente (lista ordinata)
- `redealsLeft`
- Difficoltà corrente
- Timer (millisecondi trascorsi al momento del salvataggio)

---

---

## 8. Tutorial dedicato

Il tutorial è una partita guidata con mazzo predeterminato (come in Trasloco), che insegna passo per passo le meccaniche di Tiramisù.

### Struttura del tutorial
Il tutorial viene proposto al primo avvio (dialog: "Vuoi vedere il tutorial?") ed è sempre rigiocabile dal menu principale.

### Passi del tutorial (sequenza scripted)
1. **Presentazione**: spiegazione del tallone, dei mazzetti e delle basi con overlay testuali
2. **Deal**: freccia animata sul tallone → "Tocca il tallone per distribuire le carte"
3. **Mossa stesso seme**: evidenzia due carte dello stesso seme → "Puoi spostare questa carta qui"
4. **Asso in fondazione**: quando compare un asso → "L'asso va subito alla base!"
5. **Mazzetto vuoto**: mostra come usare uno slot libero → "Uno slot vuoto accetta qualsiasi carta"
6. **Ridistribuzione**: tallone esaurito → "Il tallone è finito — ridistribuisci!"
7. **Fine tutorial**: messaggio di completamento → inizia partita normale

### Classi coinvolte
| Classe | Ruolo |
|---|---|
| `TiramisuTutorialSteps` | Lista ordinata dei passi scripted (testo + carta evidenziata + mossa attesa) |
| `TiramisuTutorialEngine` | Gestisce avanzamento passi, blocca mosse non consentite, mostra overlay |

Il tutorial usa lo stesso mazzo deterministico ogni volta (deck predefinito che garantisce i passi sopra). Fuori dal tutorial, il gioco è completamente libero.

---

## 9. Cosa rimane identico da Trasloco

| Componente | Note |
|---|---|
| `CardDeck`, `CardDeckRegistry` | Stesso mazzo italiano a 40 carte |
| `DeckPickerActivity` | Stessa UI selezione tipo di carte |
| `DatabaseHandler` | Stesso pattern SQLite |
| `ThemeUtils`, `ResourceUtils` | Stessi temi e sfondi |
| `AchievementBanner` | Stessa UI banner achievement |
| `SplashActivity` | Stessa splash screen |
| Tutti i drawable (carte, sfondi) | Assets identici — nessuna modifica |
| Sounds (`R.raw.*`) | Stessi effetti sonori |
| `WindowInsetsUtils` | Edge-to-edge support |

### SettingsActivity — modifiche rispetto a Trasloco
La `SettingsActivity` viene estesa con una nuova voce:
- **Livello di difficoltà** — spinner/radio con le 3 opzioni (Facile / Normale / Difficile), salvato in `SettingsHandler`

---

## 10. Fuori scope (v1.0)

- Achievement/trofei (da aggiungere in versione successiva)
- Daily challenge
- Modalità online / leaderboard
