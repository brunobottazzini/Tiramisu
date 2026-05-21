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
  density=$(echo "$entry" | cut -d: -f1)
  size=$(echo "$entry" | cut -d: -f2)
  out_dir="$RES_DIR/mipmap-$density"
  mkdir -p "$out_dir"

  echo "[$density] ${size}x${size} -> $out_dir/ic_launcher.png"
  rsvg-convert -w "$size" -h "$size" "$SVG" -o "$out_dir/ic_launcher.png"

  echo "[$density] ${size}x${size} -> $out_dir/ic_launcher_round.png"
  rsvg-convert -w "$size" -h "$size" "$SVG" -o "$out_dir/ic_launcher_round.png"
done

echo
echo "✓ Generati 10 PNG mipmap. Verifica dimensioni con:"
echo "  find $RES_DIR -name 'ic_launcher*.png' -exec file {} \\;"
