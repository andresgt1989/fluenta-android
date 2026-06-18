#!/usr/bin/env bash
# ============================================================
#  testlab.sh — verificación REAL recurrente en un comando.
#  Compila, corre un Robo test en un dispositivo REAL (Firebase
#  Test Lab) y deja las capturas listas para revisar (redimensionadas).
#
#  Uso:  ./testlab.sh           (locale es por defecto)
#        ./testlab.sh en        (otro locale)
#  Requiere: gcloud autenticado, proyecto fluenta-testlab-2026.
# ============================================================
set -e
cd "$(dirname "$0")"
export PATH="$PATH:/opt/google-cloud-sdk/bin"
LOCALE="${1:-es}"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT="/tmp/testlab_$STAMP"
RESULTS="ci-local-$STAMP"

echo "==> 1/4 Compilando APK..."
./gradlew assembleDebug -q

echo "==> 2/4 Robo test en dispositivo real (locale=$LOCALE)..."
gcloud firebase test android run \
  --type robo \
  --app app/build/outputs/apk/debug/app-debug.apk \
  --device model=MediumPhone.arm,version=34,locale="$LOCALE",orientation=portrait \
  --timeout 100s \
  --results-bucket "test-lab-fluenta" \
  --results-dir "$RESULTS" 2>&1 | tee /tmp/testlab_run.log | tail -8

BUCKET_PATH=$(grep -oP 'gs://[^ ]+' /tmp/testlab_run.log | head -1)
echo "==> 3/4 Bajando capturas de $BUCKET_PATH ..."
mkdir -p "$OUT/small"
gcloud storage cp "$BUCKET_PATH/**/artifacts/*.png" "$OUT/" 2>/dev/null || \
  gcloud storage cp -r "$BUCKET_PATH" "$OUT/raw" 2>/dev/null || true
gcloud storage cp "$BUCKET_PATH/**/video.mp4" /var/www/fluenta/download/fluenta-flujo-real.mp4 2>/dev/null || true

echo "==> 4/4 Redimensionando para revisar..."
for f in "$OUT"/*.png; do [ -f "$f" ] && convert "$f" -resize 600x "$OUT/small/$(basename "$f")" 2>/dev/null || true; done

echo ""
echo "LISTO. Capturas pequeñas en: $OUT/small/"
echo "Video real publicado: https://fluenta.alturya.com/download/fluenta-flujo-real.mp4"
echo "Revisa las capturas y re-califica EVALUACION_UNICORNIO.md (loop)."
