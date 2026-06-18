# ESTADO ACTUAL — Fluenta hacia unicornio (lee esto al entrar a modo admin)

> Al entrar a **modo admin Fluenta**: lee este archivo + `CLAUDE.md` + `EVALUACION_UNICORNIO.md`, recrea las tareas como TaskCreate, y **continúa el LOOP** desde "Próximo paso" sin esperar instrucciones. Mantén este archivo actualizado al avanzar.

## KPI único
App de idiomas líder mundial / unicornio. Score honesto hoy: **~43/100** (UX visual ~25 = cuello de botella). Scorecard: `EVALUACION_UNICORNIO.md`.

## Restricción crítica
**WhatsApp/Meta MUERTO** (Meta bloqueó plantillas; token expirado). La app debe ser **100% independiente de WhatsApp**. La Conversación in-app es el reemplazo del bot.

## Tareas vivas (el LOOP)
1. **[EN CURSO] UX 25→80: botón 3D en TODA la app.** Hecho en ~18 pantallas (Onboarding, Lección, Diagnóstico, Home, Match, Mapa, Repaso, Script, Perfil, Verbos). Falta: Login, GuestLesson, Paywall, ReferralCard + FluentaCard + mascota en momentos clave + micro-animaciones.
2. **[PENDIENTE] Independencia de WhatsApp:** quitar/reemplazar CTAs muertos por equivalentes in-app. Ubicaciones: `verbs/VerbsTodayScreen.kt:84` (Practicar en WhatsApp), `home/HomeScreen.kt:523` (acción WhatsApp), `lesson/LessonPlayerScreen.kt:1248` (Continuar en WhatsApp), `login/LoginScreen.kt` (OTP por WhatsApp), `home/HomeViewModel.kt:48` (practiceWaUrl). Reemplazar "continuar practicando" por la Conversación in-app.
3. **[HECHO esta sesión] Conversación in-app robusta:** estado de carga con mascota + timeout 20s + reintentar (arregló "Conectando…" vacío). Verificar en Test Lab.

## Próximo paso (continúa por aquí)
Terminar tarea #2 (independencia WhatsApp): reemplazar el "Continuar en WhatsApp" del resultado de lección y la acción WhatsApp del Home por la **Conversación in-app** (que ya funciona); quitar el botón de WhatsApp de Verbos; de-priorizar el login por WhatsApp (dejar email/Google/device). Luego seguir UX (FluentaCard + mascota) y subir cobertura de tests.

## Pipeline (se cuida solo)
CI (`build-apk.yml`) compila + corre `testDebugUnitTest` (bloquea si falla). `testlab.yml` corre Firebase Test Lab (Robo, dispositivo real) en cada PR. Rama de trabajo: `admin/design-system-pipeline` (PR #1). Tests: `app/src/test/.../LevelLabelsTest.kt`.

## Cómo verificar (sin datos falsos)
Firebase Test Lab manual: ver `fluenta-admin-testing` en memoria. Deploy: `cp app/build/outputs/apk/debug/app-debug.apk /var/www/fluenta/download/beta.apk`.

_Última actualización: 2026-06-18 por el agente en modo admin._
