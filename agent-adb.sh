#!/usr/bin/env bash
# ============================================================
#  agent-adb.sh — control del emulador Android remoto (PC del usuario
#  vía Tailscale) para que el agente teste la app Fluenta.
#
#  El emulador corre en el PC Windows (Tailscale 100.64.40.23) que expone
#  un adb server en :5037. Este VPS se conecta con adb -H/-P.
#
#  Uso:
#    source agent-adb.sh
#    fa_devices            # lista dispositivos
#    fa_install [apk]      # instala (por defecto el app-debug.apk del build)
#    fa_launch             # abre la app
#    fa_clear              # borra datos (arranca de cero, sin idioma guardado)
#    fa_shot [nombre]      # screenshot a /tmp/<nombre|fluenta_TS>.png (imprime ruta)
#    fa_tap X Y            # toque en coordenada
#    fa_text "hola"        # escribe texto
#    fa_key KEYCODE        # ej: fa_key 4  (atrás)
#    fa_swipe X1 Y1 X2 Y2 [ms]
#    fa_ui                 # uiautomator dump → imprime el XML de la UI
# ============================================================

export PATH="$PATH:/opt/android-sdk/platform-tools"
FA_HOST="100.64.40.23"
FA_PORT="5037"
FA_PKG="com.alturya.fluenta"
FA_APK="/opt/fluenta-android/app/build/outputs/apk/debug/app-debug.apk"
ADB="adb -H $FA_HOST -P $FA_PORT"

fa_devices() { $ADB devices; }

fa_install() {
  local apk="${1:-$FA_APK}"
  echo "Instalando $apk ..."
  $ADB install -r "$apk"
}

fa_launch() {
  $ADB shell monkey -p "$FA_PKG" -c android.intent.category.LAUNCHER 1
}

fa_clear() {
  $ADB shell pm clear "$FA_PKG"
}

fa_shot() {
  local name="${1:-fluenta_$(date +%H%M%S)}"
  local out="/tmp/${name}.png"
  $ADB exec-out screencap -p > "$out"
  echo "$out"
}

fa_tap()   { $ADB shell input tap "$1" "$2"; }
fa_text()  { $ADB shell input text "$1"; }
fa_key()   { $ADB shell input keyevent "$1"; }
fa_swipe() { $ADB shell input swipe "$1" "$2" "$3" "$4" "${5:-300}"; }

fa_ui() {
  $ADB shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  $ADB exec-out cat /sdcard/ui.xml
}

echo "agent-adb.sh cargado. Emulador: $FA_HOST:$FA_PORT · app: $FA_PKG"
