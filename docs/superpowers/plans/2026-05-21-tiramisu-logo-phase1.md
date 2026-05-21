# Tiramisù Logo — Fase 1 (Launcher Icon Android) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sostituire l'icona launcher attuale (path vettoriale default Android Studio) con un tiramisù stilizzato cornice oro su radial bordeaux, sia in versione adaptive (API 26+) sia PNG legacy (API 24-25), riusando i color token esistenti dell'app.

**Architecture:** Un singolo SVG sorgente (`tools/launcher_icon.svg`) è la verità autorevole; da lì generiamo: (a) due vector drawable Android — uno per il foreground adaptive (solo cake, no sfondo) e uno per il background adaptive (radial bordeaux); (b) 10 PNG mipmap legacy (5 densità × `ic_launcher` + `ic_launcher_round`) renderizzati via `rsvg-convert`. Lo stesso SVG verrà riusato in Fase 2 per gli asset Play Store.

**Tech Stack:** Android vector drawables (XML), shape drawables, `librsvg` (`rsvg-convert`) per la pipeline di export PNG. Nessuna dipendenza Gradle aggiuntiva.

**Spec di riferimento:** `docs/superpowers/specs/2026-05-21-tiramisu-logo-design.md`.

---

## File Structure

**Nuovi:**
- `tools/launcher_icon.svg` — SVG sorgente unico (108×108 viewport con bordeaux + cake)
- `tools/render_launcher_icons.sh` — script bash che chiama `rsvg-convert` per le 5 densità
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — vector drawable: solo cake (cornice oro + 5 strati), no sfondo
- `app/src/main/res/drawable/ic_launcher_bg_gradient.xml` — vector drawable: radial gradient bordeaux full canvas

**Modificati:**
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — punta a nuovi foreground+background
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — idem
- `app/src/main/res/values/colors.xml` — `ic_launcher_background` da `#FFFFFFFF` a `#FF8B0000`

**Eliminati:**
- `app/src/main/res/drawable/ic_launcher_background.xml` — vecchio vector path complesso default Android Studio, non più referenziato dopo il Task 4

**Rigenerati (10 file):**
- `app/src/main/res/mipmap-mdpi/ic_launcher.png` (48px) e `ic_launcher_round.png` (48px)
- `app/src/main/res/mipmap-hdpi/ic_launcher.png` (72px) e `ic_launcher_round.png` (72px)
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png` (96px) e `ic_launcher_round.png` (96px)
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` (144px) e `ic_launcher_round.png` (144px)
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (192px) e `ic_launcher_round.png` (192px)

---

## Geometria di riferimento (canvas viewport 108×108)

Coordinate riusate sia in SVG sia nei vector drawable Android (stesso viewport):

- **Cornice oro**: rounded rect outer `(22,28)→(86,80)`, r=4. Spessore visibile 4px (i layer interni partono a 4px dall'outer).
- **Strato 1 cacao**: rect `(26,32)→(82,39)`, h=7, color `#1A1A1A`
- **Strato 2 mascarpone**: rect `(26,39)→(82,48)`, h=9, color `#F5E6B3`
- **Strato 3 savoiardi**: rect `(26,48)→(82,57)`, h=9, color `#D4AF37`
- **Strato 4 mascarpone**: rect `(26,57)→(82,66)`, h=9, color `#F5E6B3`
- **Strato 5 savoiardi base**: rect `(26,66)→(82,76)`, h=10, color `#8B7129`
- **Sfondo radiale**: bordeaux `#8B0000` al centro `(54,54)` → `#4A0000` a raggio 60 (viewport units)

Cornice path-data (rounded rect r=4):
```
M26,28 L82,28 A4,4 0 0 1 86,32 L86,76 A4,4 0 0 1 82,80 L26,80 A4,4 0 0 1 22,76 L22,32 A4,4 0 0 1 26,28 Z
```

---

### Task 1: Pre-flight — verifica `rsvg-convert`

**Files:** nessuno (controllo ambiente)

- [ ] **Step 1: Verifica se `rsvg-convert` è installato**

Run:
```bash
which rsvg-convert
```

Expected: percorso eseguibile (es. `/opt/homebrew/bin/rsvg-convert`). Se vuoto, vai allo Step 2.

- [ ] **Step 2: Installa `librsvg` via Homebrew se mancante**

Run (solo se Step 1 non ha output):
```bash
brew install librsvg
```

Expected: BUILD/INSTALL successful. Riverifica con `which rsvg-convert`.

- [ ] **Step 3: Verifica versione**

Run:
```bash
rsvg-convert --version
```

Expected: output del tipo `rsvg-convert version X.Y.Z`. Versione ≥ 2.40 sufficiente.

---

### Task 2: Crea SVG sorgente

**Files:**
- Create: `/Users/bottazzini/Documents/misc/Tiramisu/tools/launcher_icon.svg`

- [ ] **Step 1: Verifica esiste la directory `tools/`**

Run:
```bash
ls -ld /Users/bottazzini/Documents/misc/Tiramisu/tools
```

Expected: directory esistente (già contiene altri tool). Se non esiste, `mkdir -p /Users/bottazzini/Documents/misc/Tiramisu/tools`.

- [ ] **Step 2: Scrivi l'SVG sorgente**

Crea `/Users/bottazzini/Documents/misc/Tiramisu/tools/launcher_icon.svg` con questo contenuto esatto:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">
  <defs>
    <radialGradient id="bg" cx="54" cy="54" r="60" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#8B0000"/>
      <stop offset="1" stop-color="#4A0000"/>
    </radialGradient>
  </defs>
  <!-- Sfondo bordeaux radiale a tutto canvas -->
  <rect x="0" y="0" width="108" height="108" fill="url(#bg)"/>
  <!-- Cornice oro (rounded rect r=4) -->
  <path d="M26,28 L82,28 A4,4 0 0 1 86,32 L86,76 A4,4 0 0 1 82,80 L26,80 A4,4 0 0 1 22,76 L22,32 A4,4 0 0 1 26,28 Z" fill="#D4AF37"/>
  <!-- Strato 1 cacao -->
  <rect x="26" y="32" width="56" height="7"  fill="#1A1A1A"/>
  <!-- Strato 2 mascarpone -->
  <rect x="26" y="39" width="56" height="9"  fill="#F5E6B3"/>
  <!-- Strato 3 savoiardi -->
  <rect x="26" y="48" width="56" height="9"  fill="#D4AF37"/>
  <!-- Strato 4 mascarpone -->
  <rect x="26" y="57" width="56" height="9"  fill="#F5E6B3"/>
  <!-- Strato 5 savoiardi base -->
  <rect x="26" y="66" width="56" height="10" fill="#8B7129"/>
</svg>
```

- [ ] **Step 3: Verifica il file è SVG valido**

Run:
```bash
rsvg-convert -w 200 /Users/bottazzini/Documents/misc/Tiramisu/tools/launcher_icon.svg -o /tmp/test_logo.png && file /tmp/test_logo.png
```

Expected: `/tmp/test_logo.png: PNG image data, 200 x 200, 8-bit/color RGBA, non-interlaced` (o simile). Nessun errore di parsing.

- [ ] **Step 4: Commit**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add tools/launcher_icon.svg
git commit -m "feat(logo): add launcher icon SVG source

Tiramisù cross-section: gold-bordered cake (5 alternating layers) over
bordeaux radial gradient. Sole source for both Android vector drawables
and Play Store PNG assets."
```

---

### Task 3: Crea vector drawable foreground (solo cake)

**Files:**
- Create: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/drawable/ic_launcher_foreground.xml`

- [ ] **Step 1: Scrivi il vector drawable**

Crea `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/drawable/ic_launcher_foreground.xml` con questo contenuto:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Cornice oro (rounded rect r=4) -->
    <path
        android:pathData="M26,28 L82,28 A4,4 0 0 1 86,32 L86,76 A4,4 0 0 1 82,80 L26,80 A4,4 0 0 1 22,76 L22,32 A4,4 0 0 1 26,28 Z"
        android:fillColor="#FFD4AF37"/>

    <!-- Strato 1 cacao -->
    <path android:pathData="M26,32 L82,32 L82,39 L26,39 Z" android:fillColor="#FF1A1A1A"/>
    <!-- Strato 2 mascarpone -->
    <path android:pathData="M26,39 L82,39 L82,48 L26,48 Z" android:fillColor="#FFF5E6B3"/>
    <!-- Strato 3 savoiardi -->
    <path android:pathData="M26,48 L82,48 L82,57 L26,57 Z" android:fillColor="#FFD4AF37"/>
    <!-- Strato 4 mascarpone -->
    <path android:pathData="M26,57 L82,57 L82,66 L26,66 Z" android:fillColor="#FFF5E6B3"/>
    <!-- Strato 5 savoiardi base -->
    <path android:pathData="M26,66 L82,66 L82,76 L26,76 Z" android:fillColor="#FF8B7129"/>
</vector>
```

- [ ] **Step 2: Verifica con build (aapt2 valida i vector drawable)**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:processDebugResources
```

Expected: `BUILD SUCCESSFUL`. Se errori di parsing path-data, riguarda lo Step 1.

---

### Task 4: Crea vector drawable background gradient

**Files:**
- Create: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/drawable/ic_launcher_bg_gradient.xml`

- [ ] **Step 1: Scrivi il background vector**

Crea `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/drawable/ic_launcher_bg_gradient.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <path android:pathData="M0,0 L108,0 L108,108 L0,108 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="54"
                android:centerY="54"
                android:gradientRadius="60"
                android:startColor="#FF8B0000"
                android:endColor="#FF4A0000"/>
        </aapt:attr>
    </path>
</vector>
```

- [ ] **Step 2: Verifica build**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:processDebugResources
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 5: Aggiorna adaptive icon XML (entrambi: standard + round)

**Files:**
- Modify: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Modify: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

- [ ] **Step 1: Aggiorna `ic_launcher.xml`**

Sostituisci il contenuto di `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` con:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_bg_gradient"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

- [ ] **Step 2: Aggiorna `ic_launcher_round.xml`**

Sostituisci il contenuto di `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` con (identico al precedente — l'adaptive icon API non richiede artwork diverso per round):

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_bg_gradient"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

- [ ] **Step 3: Verifica build**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:processDebugResources
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 6: Aggiorna colors.xml (fallback color)

**Files:**
- Modify: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/values/colors.xml:11`

- [ ] **Step 1: Cambia il colore `ic_launcher_background`**

Nel file `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/values/colors.xml`, sostituisci la riga:

```xml
    <color name="ic_launcher_background">#FFFFFFFF</color>
```

con:

```xml
    <color name="ic_launcher_background">#FF8B0000</color>
```

- [ ] **Step 2: Verifica build**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:processDebugResources
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 7: Elimina il vecchio vector `ic_launcher_background.xml`

**Files:**
- Delete: `/Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/drawable/ic_launcher_background.xml`

- [ ] **Step 1: Verifica che il file non sia più referenziato**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
grep -rn "@drawable/ic_launcher_background" app/src/main/res/
```

Expected: nessun risultato (nessun riferimento residuo). Se ci sono risultati, NON eliminare — investigare prima.

- [ ] **Step 2: Elimina il file**

Run:
```bash
rm /Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res/drawable/ic_launcher_background.xml
```

- [ ] **Step 3: Verifica build**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. Se errore "cannot find resource ic_launcher_background", qualcosa lo referenzia ancora.

- [ ] **Step 4: Commit checkpoint adaptive icon completo**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add app/src/main/res/drawable/ic_launcher_foreground.xml \
        app/src/main/res/drawable/ic_launcher_bg_gradient.xml \
        app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml \
        app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml \
        app/src/main/res/values/colors.xml \
        app/src/main/res/drawable/ic_launcher_background.xml
git commit -m "feat(logo): replace adaptive launcher icon with tiramisù design

Vector drawables for foreground (cake) and background (bordeaux radial)
take effect on API 26+. Legacy PNG mipmaps replaced in follow-up commit."
```

---

### Task 8: Crea lo script di rendering PNG

**Files:**
- Create: `/Users/bottazzini/Documents/misc/Tiramisu/tools/render_launcher_icons.sh`

- [ ] **Step 1: Scrivi lo script**

Crea `/Users/bottazzini/Documents/misc/Tiramisu/tools/render_launcher_icons.sh` con questo contenuto:

```bash
#!/usr/bin/env bash
#
# Rende l'SVG sorgente del logo Tiramisù nei PNG mipmap legacy (API 24-25).
# Genera anche le varianti _round (Android applica la maschera in launcher
# pre-API 26, quindi usiamo la stessa immagine quadrata).
#
# Dipendenza: librsvg (brew install librsvg)
# Usage: ./tools/render_launcher_icons.sh

set -euo pipefail

if ! command -v rsvg-convert >/dev/null 2>&1; then
  echo "Errore: rsvg-convert non trovato. Installa con: brew install librsvg" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SVG="$SCRIPT_DIR/launcher_icon.svg"
RES_DIR="$PROJECT_ROOT/app/src/main/res"

if [[ ! -f "$SVG" ]]; then
  echo "Errore: $SVG non trovato" >&2
  exit 1
fi

# densità → lato in px per launcher icon Android
declare -a DENSITIES=("mdpi:48" "hdpi:72" "xhdpi:96" "xxhdpi:144" "xxxhdpi:192")

for entry in "${DENSITIES[@]}"; do
  density="${entry%%:*}"
  size="${entry##*:}"
  out_dir="$RES_DIR/mipmap-$density"
  mkdir -p "$out_dir"

  echo "[$density] $size×$size → $out_dir/ic_launcher.png"
  rsvg-convert -w "$size" -h "$size" "$SVG" -o "$out_dir/ic_launcher.png"

  echo "[$density] $size×$size → $out_dir/ic_launcher_round.png"
  rsvg-convert -w "$size" -h "$size" "$SVG" -o "$out_dir/ic_launcher_round.png"
done

echo
echo "✓ Generati 10 PNG mipmap. Verifica dimensioni con:"
echo "  find $RES_DIR -name 'ic_launcher*.png' -exec file {} \\;"
```

- [ ] **Step 2: Rendi lo script eseguibile**

Run:
```bash
chmod +x /Users/bottazzini/Documents/misc/Tiramisu/tools/render_launcher_icons.sh
```

- [ ] **Step 3: Verifica permessi**

Run:
```bash
ls -l /Users/bottazzini/Documents/misc/Tiramisu/tools/render_launcher_icons.sh
```

Expected: prima colonna include `x` per owner (es. `-rwxr--r--`).

---

### Task 9: Esegui lo script e sostituisci i PNG mipmap

**Files:** sostituiti 10 file `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher{,_round}.png`

- [ ] **Step 1: Esegui lo script**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./tools/render_launcher_icons.sh
```

Expected: 10 righe di output `[densità] dimensione → path`, poi `✓ Generati 10 PNG mipmap`. Nessun errore.

- [ ] **Step 2: Verifica dimensioni di tutti i PNG**

Run:
```bash
find /Users/bottazzini/Documents/misc/Tiramisu/app/src/main/res -name 'ic_launcher*.png' -exec file {} \;
```

Expected: 10 righe, una per file. Dimensioni attese:
- `mipmap-mdpi/...`: `48 x 48`
- `mipmap-hdpi/...`: `72 x 72`
- `mipmap-xhdpi/...`: `96 x 96`
- `mipmap-xxhdpi/...`: `144 x 144`
- `mipmap-xxxhdpi/...`: `192 x 192`

Esempio output atteso per una riga:
```
.../mipmap-xxxhdpi/ic_launcher.png: PNG image data, 192 x 192, 8-bit/color RGBA, non-interlaced
```

- [ ] **Step 3: Verifica build finale**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit della pipeline PNG + asset rigenerati**

```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git add tools/render_launcher_icons.sh \
        app/src/main/res/mipmap-mdpi/ic_launcher.png \
        app/src/main/res/mipmap-mdpi/ic_launcher_round.png \
        app/src/main/res/mipmap-hdpi/ic_launcher.png \
        app/src/main/res/mipmap-hdpi/ic_launcher_round.png \
        app/src/main/res/mipmap-xhdpi/ic_launcher.png \
        app/src/main/res/mipmap-xhdpi/ic_launcher_round.png \
        app/src/main/res/mipmap-xxhdpi/ic_launcher.png \
        app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png \
        app/src/main/res/mipmap-xxxhdpi/ic_launcher.png \
        app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
git commit -m "feat(logo): regenerate legacy PNG mipmaps from SVG source

Pre-API 26 devices now show the same tiramisù icon via PNG mipmaps in
5 densities (48..192 px). Generated via tools/render_launcher_icons.sh."
```

---

### Task 10: Verifica finale + gate di approvazione visiva

**Files:** nessuna modifica — solo verifica.

- [ ] **Step 1: Stato git pulito**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git status
```

Expected: `nothing to commit, working tree clean`.

- [ ] **Step 2: Riepilogo asset cambiati**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
git log --oneline -5
git diff HEAD~3 --stat | head -30
```

Expected: ultimi 3 commit visibili (SVG + adaptive + PNG), con ~15 file modificati.

- [ ] **Step 3: Build release per certezza**

Run:
```bash
cd /Users/bottazzini/Documents/misc/Tiramisu
./gradlew :app:assembleDebug :app:lintDebug
```

Expected: `BUILD SUCCESSFUL`. Eventuali warning lint sono accettabili se non legati al logo.

- [ ] **Step 4: Gate di approvazione visiva (utente)**

Comunica all'utente:

> Fase 1 implementata. Installa l'apk debug con
> `./gradlew :app:installDebug` (richiede device/emulatore connesso)
> oppure `~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36 &`
> e poi `./gradlew :app:installDebug`.
>
> Conferma visivamente che l'icona launcher è quella nuova (cake con
> cornice oro su bordeaux). Se approvata, partiamo con la Fase 2 (Play
> Store: app icon 512×512 + feature graphic 1024×500).

**Questa è la fine della Fase 1.** La Fase 2 ha il suo brainstorming + spec + plan separati e parte solo dopo l'approvazione visiva esplicita dell'utente.

---

## Self-Review (post-scrittura del piano)

**Spec coverage** (rispetto a `2026-05-21-tiramisu-logo-design.md` §3):
- §3.1 file creati → Task 2 (SVG), 3 (foreground), 4 (background), 8 (script) ✓
- §3.2 file modificati → Task 5 (adaptive xml), 6 (colors), 7 (delete old), 9 (PNG) ✓
- §3.3 dimensioni PNG legacy → Task 8 (script con densities array), 9 verifica con `file` ✓
- §3.4 pipeline → Task 8 (script), 9 (esecuzione) ✓
- §3.5 verifica → Task 7, 9, 10 (assembleDebug + gate utente) ✓
- Fase 2 esplicitamente esclusa dal plan → Task 10 Step 4 documenta gate ✓

**Placeholder scan:** nessun TBD/TODO. Tutti gli step hanno comandi/file content concreti.

**Type consistency:** nomi file coerenti tra task (`ic_launcher_foreground.xml`, `ic_launcher_bg_gradient.xml`, `render_launcher_icons.sh`). Path data della cornice identico in SVG (Task 2) e vector drawable (Task 3). Coordinate dei layer identiche.
