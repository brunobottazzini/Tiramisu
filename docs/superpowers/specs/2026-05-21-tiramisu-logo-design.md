# Tiramisù — Logo Design Spec

**Data**: 2026-05-21
**Stato**: Approvato (concept) — in attesa di approvazione visiva post-Fase 1

---

## 1. Scope e fasi

Il lavoro è suddiviso in due fasi sequenziali con gate di approvazione visiva tra le due:

- **Fase 1 — Launcher icon Android**: adaptive icon (API 26+) + PNG legacy per `mipmap-mdpi..xxxhdpi`.
- **Fase 2 — Asset Google Play Console**: app icon 512×512 + feature graphic 1024×500. Parte solo dopo che l'utente ha visto e approvato il risultato della Fase 1 installato su device.

Tutto deriva da un unico file SVG sorgente (`tools/launcher_icon.svg`) per garantire coerenza tra Android e Play Store.

---

## 2. Concept visivo (entrambe le fasi)

Fetta di tiramisù vista in sezione: cornice oro che racchiude cinque strati orizzontali (cacao polvere, mascarpone, savoiardi, mascarpone, savoiardi base) su sfondo bordeaux con sfumatura radiale. Nessun accessorio (no chicco di caffè, no piattino, no wordmark).

Palette interamente derivata dai color token esistenti in `app/src/main/res/values/colors.xml`, nessun nuovo colore introdotto.

| Elemento | Hex | Token `colors.xml` |
|---|---|---|
| Sfondo radiale centro | `#8B0000` | `casino_bordeaux` |
| Sfondo radiale bordo | `#4A0000` | `casino_bordeaux_dark` |
| Cornice cake | `#D4AF37` | `casino_gold` |
| Strato 1 cacao | `#1A1A1A` | `casino_ink` |
| Strato 2 mascarpone | `#F5E6B3` | `casino_gold_light` |
| Strato 3 savoiardi | `#D4AF37` | `casino_gold` |
| Strato 4 mascarpone | `#F5E6B3` | `casino_gold_light` |
| Strato 5 savoiardi base | `#8B7129` | `casino_gold_dark` |

Geometria sul canvas adaptive 108×108 (coordinate viewport):

- Cornice: rect `(22,28)→(86,80)`, raggio 4, fill `casino_gold`, spessore implicito 4px (differenza tra rect esterno e interno)
- Strato 1 cacao (spolverata): rect `(26,32)→(82,39)`, h=7
- Strato 2 mascarpone: rect `(26,39)→(82,48)`, h=9
- Strato 3 savoiardi: rect `(26,48)→(82,57)`, h=9
- Strato 4 mascarpone: rect `(26,57)→(82,66)`, h=9
- Strato 5 savoiardi base: rect `(26,66)→(82,76)`, h=10

Safe-zone adaptive icon (cerchio centrale di Ø66dp del canvas 108×108) interamente coperta dalla cornice: il logo resta intero anche sotto le maschere più aggressive del launcher (circle, squircle, teardrop).

---

## 3. Fase 1 — Launcher icon Android

### 3.1 File creati

| File | Tipo | Note |
|---|---|---|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Vector drawable | Cornice + 5 strati, viewport 108×108 |
| `app/src/main/res/drawable/ic_launcher_bg_gradient.xml` | Shape drawable | Radial gradient bordeaux 0.5/0.5, raggio 60 |
| `tools/launcher_icon.svg` | SVG sorgente | Mirror del vector drawable, 1024×1024 nominali per export. Usato sia per generare i PNG legacy sia per esportare gli asset Play Store in Fase 2. |
| `tools/render_launcher_icons.sh` | Script bash | `rsvg-convert` da SVG → PNG nelle 5 densità mipmap |

### 3.2 File modificati

| File | Modifica |
|---|---|
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | `<background>` → `@drawable/ic_launcher_bg_gradient`; `<foreground>` → `@drawable/ic_launcher_foreground` |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Idem |
| `app/src/main/res/values/colors.xml` | `ic_launcher_background` da `#FFFFFFFF` → `#FF8B0000` (fallback ragionevole se l'adaptive non viene applicato) |
| `app/src/main/res/drawable/ic_launcher_background.xml` | **Eliminato.** Era la vecchia path complessa default Android Studio, sostituita da `ic_launcher_bg_gradient.xml` |
| `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` | Sostituiti (5 file) |
| `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_round.png` | Sostituiti (5 file) |

### 3.3 Dimensioni PNG legacy

| Densità | Dimensione lato |
|---|---|
| mdpi | 48px |
| hdpi | 72px |
| xhdpi | 96px |
| xxhdpi | 144px |
| xxxhdpi | 192px |

`ic_launcher_round.png` viene generato dallo stesso SVG con maschera circolare applicata via `rsvg-convert` (oppure: lasciato quadrato, Android applica la maschera in launcher pre-API 26).

### 3.4 Pipeline di generazione

```
tools/launcher_icon.svg   ──┬── rsvg-convert -w 48  ──► mipmap-mdpi/ic_launcher.png
                            ├── rsvg-convert -w 72  ──► mipmap-hdpi/ic_launcher.png
                            ├── rsvg-convert -w 96  ──► mipmap-xhdpi/ic_launcher.png
                            ├── rsvg-convert -w 144 ──► mipmap-xxhdpi/ic_launcher.png
                            └── rsvg-convert -w 192 ──► mipmap-xxxhdpi/ic_launcher.png
                                                       (+ versioni _round)
```

Prerequisito: `brew install librsvg` per `rsvg-convert`. Lo script `tools/render_launcher_icons.sh` controlla la presenza del binario e mostra messaggio chiaro se mancante.

### 3.5 Verifica Fase 1

- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (aapt2 valida i vector drawable).
- Manuale (utente): installazione apk debug su device/emulatore, conferma che l'icona launcher è quella nuova. **Approvazione visiva di questa fase è gate per la Fase 2.**

---

## 4. Fase 2 — Asset Google Play Console (post-approvazione Fase 1)

Specifiche ufficiali da [Google Play Console help](https://support.google.com/googleplay/android-developer/answer/9866151) (verificato 2026-05-21):

| Asset | Dimensione | Formato | Alpha | Max size | Required |
|---|---|---|---|---|---|
| App icon | 512×512 px | 32-bit PNG | Sì | 1024 KB | Sì |
| Feature graphic | 1024×500 px | JPEG o 24-bit PNG | No | — | Sì |

### 4.1 App icon 512×512

Stesso SVG sorgente `tools/launcher_icon.svg` esportato a 512×512 px. La cornice + strati + radial gradient bordeaux occupano l'intero quadrato (no padding aggiuntivo: il Play Store non applica maschere adaptive, l'icona viene mostrata come quadrata o con angoli arrotondati dal client).

Output: `tools/play_store/app_icon_512.png` (PNG con alpha — anche se non strettamente necessario qui dato che lo sfondo è opaco, rispetta la specifica).

### 4.2 Feature graphic 1024×500

Banner orizzontale: lo stesso radial gradient bordeaux di sfondo, fetta di tiramisù centrata verticalmente leggermente a sinistra del centro, wordmark "Tiramisù" a destra in `casino_gold_light` (font da decidere — proposta default: famiglia serif italica con effetto "menu di ristorante", da rivedere in Fase 2). No alpha (sfondo solido).

Output: `tools/play_store/feature_graphic_1024x500.png` (PNG 24-bit, no alpha).

> **Nota di scope Fase 2**: il wordmark e il font esatto del feature graphic vengono brainstormati in un task separato all'avvio della Fase 2. Questo spec si limita a documentare le dimensioni e il pattern (cake + wordmark + bordeaux).

### 4.3 Pipeline Play Store

Esportazione una tantum tramite estensione di `tools/render_launcher_icons.sh` (oppure script separato `tools/render_play_store_assets.sh`):

```
tools/launcher_icon.svg     ──► rsvg-convert -w 512 -h 512  ──► tools/play_store/app_icon_512.png
tools/play_store_banner.svg ──► rsvg-convert -w 1024 -h 500 ──► tools/play_store/feature_graphic_1024x500.png
```

Il banner ha un SVG sorgente separato (`tools/play_store_banner.svg`) perché compone il cake con il wordmark — non è un semplice resize.

### 4.4 Verifica Fase 2

- Dimensioni esatte con `file` o `identify`.
- PNG alpha: `identify -format "%[opaque]"` su app_icon_512.png deve dire `False`.
- Feature graphic: `identify -format "%[opaque]"` deve dire `True` (no alpha).
- Caricamento su Play Console (manuale, utente): nessun errore di validazione.

---

## 5. Esclusioni dallo scope (YAGNI)

- ❌ Logo intra-app (splash, achievement banner, casino_gif_frame, ecc.) — il logo launcher non si propaga automaticamente lì.
- ❌ Notification icon (richiede silhouette bianca 24dp diversa).
- ❌ Wear OS / Android TV banner.
- ❌ Screenshot Play Store (richiedono catture dell'app, separato dal logo).
- ❌ Rinomina app, cambio nome pacchetto.
- ❌ Preview video YouTube (opzionale Play Store).

---

## 6. Rollback

Se la Fase 1 non convince visivamente:
- I PNG legacy sostituiti sono sotto git, recuperabili con `git checkout HEAD~1 -- app/src/main/res/mipmap-*/ic_launcher*.png`.
- Il vecchio `ic_launcher_background.xml` può essere ripristinato dal git history.
- L'`adaptive icon xml` torna ai puntatori originali con un revert.

Nessuna modifica al codice Kotlin / build.gradle / AndroidManifest — il rollback è puramente di asset.
