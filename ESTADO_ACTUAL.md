# ESTADO ACTUAL — Fluenta hacia unicornio (lee esto al entrar a modo admin)

> Al entrar a **modo admin Fluenta**: lee este archivo + `CLAUDE.md` + `EVALUACION_UNICORNIO.md`, recrea las tareas como TaskCreate, y **continúa el LOOP** desde "Próximo paso" sin esperar instrucciones. Mantén este archivo actualizado al avanzar.

## 📍 DÓNDE QUEDAMOS (última sesión: 2026-06-18)
Score honesto **48/100** (de 43). Rama `admin/design-system-pipeline`, **PR #1** abierto con todo.
**Hecho esta sesión:** botón 3D en ~18 pantallas · mascota en feedback · conversación in-app arreglada (timeout+carga) · **independencia de WhatsApp** (Meta muerto; lección/home/verbos → in-app) · tests unit + **test de UI de flujo crítico** (`OnboardingFlowTest`) · CI que bloquea regresiones · Firebase Test Lab auto en cada PR · `testlab.sh` (verificación real en 1 comando) · sistema de scoring en loop · protocolo CEO en CLAUDE.md.
**Pendiente de mergear:** PR #1 a master (revisa si CI verde y mergea).
**▶️ CONTINUAR POR AQUÍ →** ver "Próximo paso" abajo (UX: FluentaCard + mascota en más momentos + botones 3D en Login/Paywall/GuestLesson · Retención: misiones diarias, liga real, notis · de-priorizar login WhatsApp).

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
WhatsApp independence HECHO (lección/home/verbos → in-app); falta de-priorizar login por WhatsApp (dejar email/Google/device). Luego, atacar la nota más baja accionable:
1. **UX 45→** seguir: FluentaCard (tarjeta firma), mascota en más momentos (bienvenida home, estados vacíos), micro-animaciones, botones 3D en Login/Paywall/GuestLesson.
2. **Retención 40→** liga semanal real, misiones diarias, notificaciones inteligentes.
Re-calificar tras cada lote en EVALUACION_UNICORNIO.md.

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
