# CLAUDE.md — Reglas del proyecto Fluenta (léelas cada sesión)

## 🔑 PROTOCOLO "MODO ADMIN FLUENTA" (actívalo cuando el usuario lo pida)
Cuando el usuario diga **"modo admin Fluenta"** (o "modo admin"), activa este flujo de trabajo profesional:
1. **Lee `ESTADO_ACTUAL.md`** (tareas vivas + "próximo paso") + este CLAUDE.md + memorias `fluenta-mision`, `fluenta-admin-testing`, `fluenta-score-tracker`. Recrea las tareas con TaskCreate y **CONTINÚA desde "Próximo paso"** sin esperar instrucciones. Mantén `ESTADO_ACTUAL.md` actualizado al avanzar.
2. **Trabaja en LOOP de mejora continua** (no esperes que te pidan cada paso):
   a. Elige el área de menor nota / mayor ROI en `EVALUACION_UNICORNIO.md`.
   b. Si no sé hacerlo de clase mundial, **investigo a fondo ANTES** de codear.
   c. Produzco el cambio → `./gradlew assembleDebug` (debe pasar) → `testDebugUnitTest`.
   d. Despliego `beta.apk` + commit en la rama `admin/*` (PR, no master directo).
   e. **Verifico en dispositivo REAL** (Firebase Test Lab, auto en cada PR). NUNCA datos falsos.
   f. Re-califico el área honestamente; actualizo scorecard + memoria.
   g. Vuelvo a (a). No declaro "listo" sin evidencia real.
3. **Cada cambio relevante**: compila + pasa tests + se prueba en dispositivo real (el pipeline lo hace solo).
4. **Guarda en memoria** lo no obvio (decisiones, hallazgos, restricciones) para la próxima sesión.
5. **KPI único:** que un usuario real sienta una app de clase mundial. No "cantidad de cambios".

Pipeline ya montado: CI compila + corre tests (bloquea si fallan) + Firebase Test Lab en cada PR (`fluenta-testlab-2026`). Falta: más cobertura de tests UI, staging.

## Misión / único KPI
Convertir Fluenta en una **app de idiomas líder mundial / unicornio en el menor tiempo posible**, empezando por este MVP. El KPI no es "cantidad de cambios" — es que **un usuario real abra la app y sienta calidad de clase mundial**: bonita, rápida al valor, que retiene. Hoy el usuario la califica ~4/100 por estética/UX pobre. El problema central es falta de un **sistema de diseño cohesivo con personalidad** (nivel Duolingo), no bugs sueltos.

## Reglas de oro (NO romper)
1. **Verifica contra la REALIDAD, nunca con datos falsos.** Los screenshots de Roborazzi usan `previewState` inventado y NO reproducen el flujo real (esa fue la gran lección: ~50 cambios "se veían bien" pero no tocaban el problema real). Para validar UX real usa Firebase Test Lab o el backend real.
2. **Reproduce el bug antes de arreglarlo, y verifica después.** No te fíes del "verde fácil".
3. **Compila antes de decir "listo":** `./gradlew assembleDebug` debe pasar.
4. **Honestidad cliente vs backend.** No finjas lo que depende del servidor (coach en L1, default de idioma, liga, traducciones i18n en/pt, SRS de 3 niveles). Márcalo y avanza en lo que controlas.
5. **Produce, no solo documentes.** Bucles cortos: construir → aplicar → verificar en dispositivo real → iterar.

## Hechos del producto (memorízalos)
- **Stack:** Android, Kotlin, Jetpack Compose, MVVM. Tema en `app/src/main/java/com/alturya/fluenta/ui/theme/`.
- **El default de idioma de una cuenta NUEVA es INGLÉS, no chino** (verificado por API). El "se va a chino" que reporta el usuario es por su CUENTA VIEJA con `l2=zh` guardado → se arregla borrando datos / cerrando sesión.
- **Backend real:** `https://fluenta.alturya.com/` = `localhost:3010` en el VPS. Verificable por `curl` (auth: `Authorization: Bearer <token>`; cuenta de prueba vía `POST /api/auth/device`).
- **Mascota:** 3 poses ya existen sin usar: `ic_fluenta_hola/_saluda/_celebra`. Úsala en bienvenida/acierto/error/vacío.
- **Paleta de marca** ya definida en `ui/theme/Color.kt` (teal/coral/ámbar/púrpura/sky) — pero las pantallas no la usaban con cohesión.

## Comandos
- Build: `./gradlew assembleDebug`  → APK en `app/build/outputs/apk/debug/app-debug.apk`
- Deploy beta: `cp app/build/outputs/apk/debug/app-debug.apk /var/www/fluenta/download/beta.apk` (URL: https://fluenta.alturya.com/download/beta.apk)
- Screenshots aislados (solo layout, datos falsos): `./gradlew testDebugUnitTest --tests "com.alturya.fluenta.ScreenshotTest" -Proborazzi.test.record=true` → `app/build/screens/*.png`
- Tests: `./gradlew testDebugUnitTest`

## Modo admin Fluenta — testear la app REAL
Ver memoria `fluenta-admin-testing` y `fluenta-mision`. Dos vías:
- **Firebase Test Lab (QA automatizado, recomendado):** proyecto `fluenta-testlab-2026`, gcloud en `/opt/google-cloud-sdk/bin`. Robo test:
  `gcloud firebase test android run --type robo --app app/build/outputs/apk/debug/app-debug.apk --device model=MediumPhone.arm,version=34,locale=es,orientation=portrait --timeout 90s`
  Resultados (video+capturas+logcat) en bucket GCS → bajar con `gcloud storage cp` → **redimensionar PNG con `convert -resize 600x`** antes de leerlos.
- **ADB en vivo (PC del usuario por Tailscale):** `adb -H 100.64.40.23 -P 5037 ...`. Helper: `/opt/fluenta-android/agent-adb.sh`. Limitación: link lento para transferencias grandes.

## Sistema de diseño (listón unicornio) — en producción
Plan: `DISENO_UNICORNIO.md`. Componente firma creado: `ui/FluentaButton.kt` (botón 3D táctil estilo Duolingo). Reglas estéticas: botón 3D en vez de plano, mascota con personalidad, espacio generoso, acentos vibrantes coherentes, micro-animaciones, UN acción primaria por pantalla, ilustración sobre texto, dark mode pulido.

## Checklists de trabajo
- `CAMBIOS_REALES.md` — 251 mejoras funcionales priorizadas.
- `DISENO_UNICORNIO.md` — rebuild del sistema de diseño.
- `AUDITORIA_FLUENTA.md` — análisis de raíz del flujo.

## Pipeline (montado / falta)
Montado: backend por API · CI que compila APK (`.github/workflows/build-apk.yml`) · Firebase Test Lab + secrets (`FIREBASE_SA_KEY`, `FIREBASE_PROJECT`) · ADB/Tailscale · agente que itera.
Falta: tests unit/UI que bloqueen regresiones · auto-Test-Lab en cada push · PRs · staging.
