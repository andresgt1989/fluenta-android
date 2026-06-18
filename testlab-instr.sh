#!/usr/bin/env bash
# Verificación DIRIGIDA en DISPOSITIVO REAL (instrumentation, Firebase Test Lab).
# A diferencia del robo (crawl libre que no alcanza pantallas con estado), esto
# navega a propósito al componente y hace assert sobre hardware real.
# Uso: ./testlab-instr.sh   (requiere cuota de Test Lab disponible; resetea ~diario)
set -euo pipefail
export CLOUDSDK_PYTHON=${CLOUDSDK_PYTHON:-/usr/bin/python3}
export PATH="$PATH:/opt/google-cloud-sdk/bin"

echo "==> 1/2 Compilando app + androidTest APK..."
./gradlew assembleDebug assembleDebugAndroidTest -q

echo "==> 2/2 Instrumentation en dispositivo real (es, Android 34)..."
gcloud firebase test android run \
  --type instrumentation \
  --app app/build/outputs/apk/debug/app-debug.apk \
  --test app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device model=MediumPhone.arm,version=34,locale=es,orientation=portrait \
  --timeout 6m \
  --results-dir "instr-$(date +%s)"
