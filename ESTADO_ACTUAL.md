# ESTADO ACTUAL — Fluenta hacia unicornio (lee esto al entrar a modo admin)

> Al entrar a **modo admin Fluenta**: lee este archivo + `CLAUDE.md` + `EVALUACION_UNICORNIO.md`, recrea las tareas como TaskCreate, y **continúa el LOOP** desde "Próximo paso" sin esperar instrucciones. Mantén este archivo actualizado al avanzar.

## 📍 DÓNDE QUEDAMOS (última sesión: 2026-06-18, iter 3)
Score honesto **~50/100**. **PR #1 MERGEADO a master.** Trabajo nuevo en rama `admin/*` (PR).
**Hecho esta sesión (iter 3):**
- 🔧 **Robo CI reparado**: fallaba SIEMPRE por `--no-record-video=false` (gcloud lo rechaza) → `--record-video`. Ahora **OUTCOME Passed** en dispositivo real.
- ✅ **PR #1 mergeado** a master (build + robo verdes).
- 🎯 **Retención — Misiones diarias** en Home: 3 quests (racha/meta/repaso) derivadas de señales REALES del backend (`todayXp`, `cardsDueToday`); tarjeta con mascota + progreso + check; cada misión navega a su acción.
- 🧪 **Test de UI DIRIGIDO** (`DailyMissionsUiTest`, Robolectric, 4 casos) que hace assert de que las 3 misiones renderizan con su progreso real por el camino de producción — **reemplaza la dependencia del crawl genérico** del robo. + `DailyMissionsTest` (7 casos de lógica pura).
- 🛠️ **Motor formalizado**: creado `MOTOR_UNICORNIO.md` (loop autónomo robusto + 🔒 Regla de oro anti reward-hacking) y enganchado a `CLAUDE.md` + memoria.
**Corrección de honestidad:** subí Retención antes de tener el assert dirigido; corregido a bump conservador anclado a evidencia (40→46). La nota sube solo con señal externa dura.
- 🎨 **UX — Login 3D** (iter 4): los 2 CTA hero de Login pasaron de planos a `FluentaButton` 3D. **Verificado en dispositivo real** (screenshot del robo ci-17). Era la última pantalla de entrada con botones planos. UX 45→48.
- ✅ **PR #2 y #3 mergeados** a master (motor/verificación dirigida + UX Login).

**▶️ CONTINUAR POR AQUÍ →** ver "Próximo paso".

## KPI único
App de idiomas líder mundial / unicornio. Score honesto hoy: **~43/100** (UX visual ~25 = cuello de botella). Scorecard: `EVALUACION_UNICORNIO.md`.

## Restricción crítica
**WhatsApp/Meta MUERTO** (Meta bloqueó plantillas; token expirado). La app debe ser **100% independiente de WhatsApp**. La Conversación in-app es el reemplazo del bot.

## Tareas vivas (el LOOP)
1. **[EN CURSO] UX 25→80: botón 3D en TODA la app.** Hecho en ~18 pantallas (Onboarding, Lección, Diagnóstico, Home, Match, Mapa, Repaso, Script, Perfil, Verbos). Falta: Login, GuestLesson, Paywall, ReferralCard + FluentaCard + mascota en momentos clave + micro-animaciones.
2. **[PENDIENTE] Independencia de WhatsApp:** quitar/reemplazar CTAs muertos por equivalentes in-app. Ubicaciones: `verbs/VerbsTodayScreen.kt:84` (Practicar en WhatsApp), `home/HomeScreen.kt:523` (acción WhatsApp), `lesson/LessonPlayerScreen.kt:1248` (Continuar en WhatsApp), `login/LoginScreen.kt` (OTP por WhatsApp), `home/HomeViewModel.kt:48` (practiceWaUrl). Reemplazar "continuar practicando" por la Conversación in-app.
3. **[HECHO esta sesión] Conversación in-app robusta:** estado de carga con mascota + timeout 20s + reintentar (arregló "Conectando…" vacío). Verificar en Test Lab.

## Score actual (loop): Global ~48/100 (iteración 2). Áreas más bajas: Contenido 35 (backend), Retención 40, UX 45.

## Próximo paso (continúa por aquí)
Sigue el ciclo de `MOTOR_UNICORNIO.md` (test dirigido primero → código → verificación en capas → re-score honesto → PR/merge). Por prioridad:
1. **MOTOR (cierra la brecha de verificación):** elevar el assert dirigido de misiones a **instrumentation en Test Lab (dispositivo real)** — hoy corre en JVM (Robolectric). Ese es el gate que permite subir Retención más allá de 46 con evidencia de dispositivo real. Falta: añadir `androidTestImplementation` (compose-ui-test + espresso runner) + workflow `--type instrumentation`.
2. **UX 48→** (Login 3D ✅ HECHO iter 4) siguiente: 3D en Paywall/GuestLesson + botones OTP de Login; **FluentaCard** (tarjeta firma); mascota en bienvenida home + estados vacíos; micro-animaciones.
3. **Retención 46→** liga semanal real (hoy placeholder), notificaciones inteligentes, recompensa de misión con XP server-side.
Re-calificar tras cada lote en EVALUACION_UNICORNIO.md, **solo con señal externa dura** (🔒 Regla de oro).

## Pipeline (se cuida solo)
CI (`build-apk.yml`) compila + corre `testDebugUnitTest` (bloquea si falla). `testlab.yml` corre Firebase Test Lab (Robo, dispositivo real) en cada PR. Rama de trabajo: `admin/design-system-pipeline` (PR #1). Tests: `app/src/test/.../LevelLabelsTest.kt`.

## Cómo verificar (sin datos falsos)
Firebase Test Lab manual: ver `fluenta-admin-testing` en memoria. Deploy: `cp app/build/outputs/apk/debug/app-debug.apk /var/www/fluenta/download/beta.apk`.

_Última actualización: 2026-06-18 por el agente en modo admin._

---

## 🚀 AUTO-PROMPT DEL MOTOR UNICORNIO (mi directiva como CEO en modo admin)
> Soy el **CEO de Fluenta**. KPI único: **app líder mundial / unicornio en mínimo tiempo.**
> Cada sesión, sin esperar instrucciones:
> 1. **Re-califico honestamente** cada área en `EVALUACION_UNICORNIO.md` con evidencia de dispositivo real (no datos falsos).
> 2. **Ataco la nota más baja accionable** (lo client-side); marco lo de backend/contenido/mercado y lo escalo al usuario.
> 3. **Produzco → compilo → testeo (unit+UI) → verifico real (`./testlab.sh` o Test Lab auto en PR) → re-califico.**
> 4. **Cierro gaps del motor**: cobertura de tests, verificación recurrente de punta a punta.
> 5. **Nunca declaro "listo" sin evidencia real.** Guardo en memoria lo no obvio.
> 6. **Soy honesto**: el sistema es el motor confiable; el unicornio además necesita contenido A1→C2 en 20 idiomas, mercado y tiempo — lo digo claro y avanzo en lo que controlo.
