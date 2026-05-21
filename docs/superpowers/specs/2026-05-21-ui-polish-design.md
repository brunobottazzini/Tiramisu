# Tiramisù — UI polish (gameplay screen) Design Spec

**Data**: 2026-05-21
**Stato**: Approvato (concept) — pronto per writing-plans

---

## 1. Scope

Cinque fix visivi sulla `GameActivity` (file: `app/src/main/res/layout/game.xml`, `app/src/main/java/com/bottazzini/tiramisu/GameActivity.kt`, e supporto):

1. **Tallone** (`stockArea`) ora schiacciato orizzontale → forma carta verticale 56:102.
2. **Colori testo** allineati al tema casino (oro/oro-chiaro) invece del default nero.
3. **Icona menu** `@android:drawable/ic_menu_more` (interpretazione ambigua) → icona pausa custom.
4. **Fondazioni** troppo piccole e senza indicazione visiva di drop-zone → slot card-shape 78×142 con cornice `casino_slot_frame`.
5. **Carte nelle pile** larghe tutta la colonna → ristrette al ~70% per impressione "deck".

Tutti i cambiamenti riusano i color token esistenti (`@color/casino_gold`, `@color/casino_gold_light`, `@color/casino_bordeaux_dark`, `@color/white`). Nessun nuovo token introdotto. Nessun refactor del theme.

---

## 2. Mockup target

```
┌────────────────────────────────────────────┐
│ ⏱ 00:00       🟡 Normale            ⏸     │  TOP BAR
├────────────────────────────────────────────┤
│  ┌──┐  ┌──┐  ┌──┐  ┌──┐                   │
│  │░░│  │░░│  │░░│  │░░│                   │  FONDAZIONI 78×142
│  │  │  │  │  │  │  │  │                   │  cornice oro visibile sempre
│  └──┘  └──┘  └──┘  └──┘                   │
├────────────────────────────────────────────┤
│   ┌──┐    ┌──┐    ┌──┐    ┌──┐            │
│   │c1│    │b3│    │d7│    │s2│            │  PILE ~66×120 card-aspect
│   │c4│    │b9│    │d2│    │  │            │  (padding orizzontale)
│   │c8│    │b1│    │  │    │  │            │
├────────────────────────────────────────────┤
│ ┌──┐                                       │
│ │░░│ ↩ Ridist  R:1   ↶   💡               │  BOTTOM BAR
│ │░░│ tallone 50×91 + bottoni allineati a destra
│ └──┘                                       │
└────────────────────────────────────────────┘
```

---

## 3. Cambiamenti per item

### 3.1 Item 1 — Tallone card-shape

`game.xml`, sezione `bottomBar`:

- `stockArea` (FrameLayout): da `layout_width="0dp" layout_weight="1"` a `layout_width="46dp"`. Altezza da `72dp` a `84dp` (46 × 102/56 ≈ 84, aspect carta 56:102).
- `stockImage` resta `match_parent` × `match_parent` con `scaleType="fitCenter"`. La cornice del dorso è il dorso stesso (es. `bg2`).
- `tvStockCount` resta in basso a destra.
- I bottoni a destra (`btnRedeal`, `tvRedealsLeft`, `btnUndo`, `btnHint`) restano `wrap_content`. Inserisco un `<Space android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1"/>` dopo `stockArea` per spingerli a destra.
- `bottomBar` `paddingVertical` da `8dp` a `4dp` per recuperare spazio verticale (vedi §4).

### 3.2 Item 2 — Colori testo coerenti con tema casino

Tutte le modifiche sono inline su `game.xml`. Nessun theme/style file nuovo.

| View id | Attributo | Token |
|---|---|---|
| `tvTimer` | `android:textColor` | `@color/casino_gold` |
| `tvDifficulty` | `android:textColor` | `@color/casino_gold_light` |
| `tvStockCount` | `android:textColor` | `@color/white` (resta) |
| `tvRedealsLeft` | `android:textColor` | `@color/casino_gold_light` |
| `btnMenu` | `android:tint` | `@color/casino_gold` |
| `btnRedeal` | `android:textColor` + `android:backgroundTint` | `@color/casino_gold` + `@color/casino_bordeaux_dark` |
| `btnUndo` | idem | idem |
| `btnHint` | idem | idem |
| `btnTutorialNext` | `android:textColor` | `@color/casino_gold` |

### 3.3 Item 3 — Icona pausa

Nuovo file `app/src/main/res/drawable/ic_pause.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:pathData="M6,5h4v14h-4z" android:fillColor="#FFD4AF37"/>
    <path android:pathData="M14,5h4v14h-4z" android:fillColor="#FFD4AF37"/>
</vector>
```

`game.xml`:
- `btnMenu`: `android:src="@drawable/ic_pause"` (sostituisce `@android:drawable/ic_menu_more`)
- `contentDescription` → stringa `pause_menu_desc` "Pausa / Menu"

Nuova stringa in `app/src/main/res/values/strings.xml`:
```xml
<string name="pause_menu_desc">Pausa / Menu</string>
```

### 3.4 Item 4 — Fondazioni più grandi con cornice

`game.xml`, le 4 `ImageView` `foundation0..3`:

- `android:layout_height` da `96dp` → `124dp` (su largheza ~68dp = aspect ~56:102; cfr. §4 per il bilancio verticale)
- Aggiungere `android:background="@drawable/casino_slot_frame"` (drawable già esistente nel progetto)
- `android:src="@drawable/zero"` resta come placeholder centrato (fitCenter già impostato)
- `android:scaleType="fitCenter"` resta
- `android:padding="6dp"` aggiunto in modo che la carta non tocchi la cornice quando piena

Nessuna modifica a `GameActivity.renderFoundations()` — già usa `setImageResource` su ImageView. Lo sfondo `casino_slot_frame` resta visibile in trasparenza dietro la carta.

### 3.5 Item 5 — Carte pile più strette

`game.xml`, le 4 `LinearLayout` `pileContainer0..3`:

- Sostituire `android:padding="4dp"` con `android:paddingVertical="4dp" android:paddingHorizontal="12dp"` (uso espliciti i due attributi separati per evitare ambiguità di precedenza).

Nessuna modifica a `GameActivity.renderPile()` — già calcola `cardWidth = container.width - paddingLeft - paddingRight`. Con paddingHorizontal=12dp invece di 4dp, `cardWidth` cala di 16dp totali (8dp per side), e `cardHeight = cardWidth × 1.82` cala proporzionalmente.

Su 360dp di larghezza schermo: colonna ~90dp → cardWidth attuale 82dp (90-8), nuovo 66dp (90-24). cardHeight da 149 a 120dp. Strato visibile (peek) resta 24dp invariato.

---

## 4. Verifica vincoli verticali

Tutte le decisioni dimensionali (fondazioni 124dp, tallone 46×84dp, bottom bar `paddingVertical="4dp"`) sono già riflesse in §3. Bilancio su device 360×640dp portrait:

| Sezione | Altezza |
|---|---|
| status bar (system) | ~24 |
| top bar | ~52 |
| foundations row (124dp + 12dp padding) | ~136 |
| pile area (residuo) | ~336 |
| bottom bar (84dp tallone + 8dp padV) | ~92 |
| nav bar | ~48 |
| **Totale** | **~688** (alcuni device hanno cutout/insets variabili) |

Per device 640dp: il pile area assorbe la differenza. Una pila piena di 8 carte = 120 (top) + 7×24 (peek) = 288dp, ben dentro 336dp. Pile >8 carte → ScrollView gestisce overflow.

Su device più piccoli (es. 600dp) lo ScrollView pile area si riduce di conseguenza; nessuno dei contenitori fissi viene compresso oltre il min sensato.

---

## 5. File modificati

| File | Modifiche |
|---|---|
| `app/src/main/res/layout/game.xml` | tutti gli item: top bar colors+menu icon, foundations height+background+padding, piles paddingHorizontal, bottom bar stock width/height + button colors |
| `app/src/main/res/drawable/ic_pause.xml` | **nuovo** — vector drawable icona pausa |
| `app/src/main/res/values/strings.xml` | nuova stringa `pause_menu_desc` |

Nessuna modifica a:
- File `.kt` (la logica resta invariata; `renderPile` adatta automaticamente le dimensioni)
- File `.gradle`, `AndroidManifest.xml`
- Altri layout activity (`activity_main.xml`, ecc.)
- Theme/style files
- Strings tradotte (`values-it/strings.xml` ecc.) — `pause_menu_desc` solo in `values/strings.xml` per ora (sarà tradotta in un task separato di i18n)

---

## 6. Verifica

- `./gradlew :app:assembleDebug :app:lintDebug` → BUILD SUCCESSFUL, no warning critici sulle modifiche
- Manuale (utente): apri il gioco. Verifica:
  - Top bar: timer in oro, difficoltà oro chiaro, icona ⏸ a destra
  - Fondazioni: 4 slot card-shape ben visibili con cornice oro tratteggiata, drop chiaro
  - Pile: carte ristrette al ~70% colonna, peek invariato
  - Bottom bar: tallone come carta verticale a sinistra, bottoni in oro su bordeaux a destra

---

## 7. Esclusioni dallo scope (YAGNI)

- ❌ Theme/style file globale (refactor troppo grande)
- ❌ Colori in altri activity (SettingsActivity, RecordActivity, ecc.)
- ❌ Landscape `layout-land/game.xml` (separato, da fare se serve)
- ❌ Traduzioni di `pause_menu_desc` in 20+ locale (i18n task separato)
- ❌ Animazioni di feedback su drop
- ❌ Material You / tema sistema light/dark switching

---

## 8. Rollback

Tutte le modifiche sono asset/XML. Revert di un singolo commit ripristina lo stato precedente. Nessuna migrazione di dati.
