# Fluenta — Evaluación exigente hacia unicornio (panel experto)

Lentes: **PhD Pedagogía de idiomas · PhD UX · Builder de unicornios · CEO de startup**. Basado en la app REAL (Firebase Test Lab + código), no en datos falsos. Calificación dura: un inversor/usuario exigente no perdona.

> **Reset de honestidad:** la memoria vieja decía "~100/100". Eso estaba INFLADO porque se midió contra screenshots con datos falsos. La realidad, vista en dispositivo real, es mucho más baja. Esto es la línea base honesta.

## Áreas y nota actual (/100)

| # | Área | Nota | Por qué (evidencia real) |
|---|------|------|--------------------------|
| 1 | **UX / Diseño visual** | **25** | Material genérico plano, sin personalidad, mascota sin usar, inconsistente. (Subiendo con botón 3D.) Es el cuello de botella: si se ve barato, el usuario se va antes de ver lo bueno. |
| 2 | **Onboarding / primeros 3 min** | **50** | SÍ pregunta idioma (bien), pero cae en "Conectando…" con pantalla vacía (mata la 1ª impresión). No pregunta la meta ni el "por qué". |
| 3 | **Pedagogía / eficacia real** | **45** | Tiene SRS, diagnóstico CAT, conversación, errores por categoría. Pero gramática no explícita, profundidad A1→C2 sin probar, práctica oral rota en el arranque. |
| 4 | **Retención / hábito / gamificación** | **40** | Racha + XP + SRS + combo. Pero liga es placeholder, misiones ausentes, logros delgados, notificaciones recién con opt-in. |
| 5 | **Personalización / IA** | **55** | Coach proactivo, diagnóstico adaptativo, patrones de error. Fortaleza relativa. Pero el coach habla en L2 a principiantes (aterrador). |
| 6 | **Contenido** | **35** | Currículo solo en algunos pares (muchos "próximamente"), profundidad por nivel sin probar, ~16 destinos de 20. |
| 7 | **Tecnología / fiabilidad** | **55** | Sin crashes en logcat (bien). Pero 65 fallos silenciosos (mejorando), offline parcial, sin tests que bloqueen regresiones. |
| 8 | **Negocio / monetización** | **50** | Stripe + planes + paywall existen. Paywall día-1 spam, sin precios localizados (PPP), prueba sin recordatorio de cobro. |
| 9 | **Viralidad / growth loops** | **40** | Referidos + share card existen, pero referidos fallaba/oculto; sin loop fuerte ni k-factor medido. |
| 10 | **Accesibilidad / i18n** | **40** | i18n mejorando (era hardcodeado), RTL parcial, contraste/targets/contentDescription con huecos. |

### Nota global honesta: **~43/100**
Lectura de CEO: en el mercado, **el eslabón más débil (UX 25) tapa todo** — el usuario percibe ~4/100 como dices, porque churnea antes de descubrir la pedagogía. Por eso UX es la prioridad #1 absoluta.

## Qué es "100" en cada área (el listón unicornio)
1. **UX:** sistema de diseño cohesivo con firma (3D, mascota viva, deleite), indistinguible de Duolingo en pulido.
2. **Onboarding:** valor en <60s, pregunta meta, primer "win" hablado, cero pantallas vacías.
3. **Pedagogía:** ruta A1→C2 verificable, gramática + práctica + repaso espaciado con eficacia medida.
4. **Retención:** D1>50%, racha+liga real+misiones+notis inteligentes.
5. **IA:** dificultad siempre calibrada, coach en L1 para principiantes, plan personalizado por meta.
6. **Contenido:** 3→20 destinos con currículo completo y profundidad por nivel.
7. **Tech:** 0 fallos silenciosos, tests que bloquean regresiones, offline real, auto-QA en cada push.
8. **Negocio:** paywall tras valor, PPP, prueba con recordatorio, LTV/CAC sano.
9. **Viralidad:** loop de referidos que funciona, share con k-factor medido.
10. **a11y/i18n:** WCAG AA, 3 idiomas UI completos, RTL pulido.

## Guía priorizada a 100 (mayor ROI primero)
1. 🔴 **UX 25→80:** terminar el sistema de diseño (botón 3D en TODA la app + mascota en cada momento + tarjetas firma + micro-animaciones). *(En curso.)*
2. 🔴 **Onboarding 50→85:** arreglar "Conectando…", añadir pregunta de meta, primer win claro.
3. 🟠 **Retención 40→75:** liga real, misiones diarias, notis inteligentes.
4. 🟠 **Pedagogía 45→75:** gramática explícita por lección, objetivo CEFR visible, arreglar práctica oral.
5. 🟠 **Tech 55→85:** tests que bloqueen + auto-Test-Lab (ya montado) + matar fallos silenciosos.
6. 🟡 Contenido, Negocio, Viralidad, a11y según backend disponible.

## Método (toda la capacidad actual)
Cada subida de nota → verificar en dispositivo REAL (Firebase Test Lab, proyecto `fluenta-testlab-2026`, auto en cada PR). Sin datos falsos. Re-calificar el área tras cada lote.

---

# 🔁 SISTEMA DE MEJORA EN LOOP (calificación honesta continua)

**Regla del loop:** tras CADA lote de cambios, re-califico cada área honestamente (con evidencia de dispositivo real), actualizo la nota, y el **área de MENOR nota se vuelve el siguiente objetivo**. No declaro un área "lista" sin subir su nota con evidencia. La meta es 100/100 en cada área y total.

## Bitácora de re-calificación

### Iteración 1 (baseline honesto, 2026-06-18)
UX 25 · Onboarding 50 · Pedagogía 45 · Retención 40 · IA 55 · Contenido 35 · Tech 55 · Negocio 50 · Viralidad 40 · a11y/i18n 40 → **Global ~43/100**

### Iteración 2 (tras: botón 3D ×18 pantallas + mascota en feedback + conversación in-app arreglada + independencia WhatsApp + tests/CI/Test Lab)
- **UX 25→45** (+20): botón 3D en ~18 pantallas, mascota en feedback. Falta: FluentaCard, mascota en más momentos, micro-animaciones, Login/Paywall/GuestLesson.
- **Onboarding 50→58** (+8): arreglado "Conectando…" (carga con personalidad+timeout). Falta: pregunta de meta, primer win claro.
- **Tech 55→72** (+17): tests unitarios + CI que bloquea + Firebase Test Lab auto en cada PR. Falta: cobertura UI, staging.
- **Pedagogía 45→48** (+3): conversación in-app robusta (reemplazo WhatsApp). Falta: gramática explícita, A1→C2 (falta C2 en backend), objetivo CEFR visible.
- **IA, Contenido, Negocio, Viralidad, a11y** sin cambio significativo esta iteración.
- **Independencia WhatsApp:** transversal — quitada dependencia muerta (lección/home/verbos → in-app).
→ **Global ~48/100**

### Iteración 3 (tras: Misiones diarias + arreglo del robo CI + merge PR #1 + test de UI dirigido)
**Disciplina anti-inflación aplicada (ver "Regla de oro del score" abajo):** una nota sube SOLO con señal externa dura. El usuario detectó (con razón) que subí Retención antes de tener un assert dirigido → corregido a un bump conservador anclado a evidencia.

Evidencia dura de esta iteración:
- ✅ `DailyMissionsTest` (7 casos, lógica pura) — VERDE.
- ✅ `DailyMissionsUiTest` (4 casos, **test de UI DIRIGIDO** Robolectric): hace assert de que las 3 misiones renderizan con su progreso real por el camino de producción (`UserProgress`→`DailyMissions.build`→`DailyMissionsCard`): título, contador 0/3 · 1/3 · 3/3, y celebración al completar — VERDE. Esto reemplaza la dependencia del crawl genérico.
- ✅ Robo en dispositivo real (Pixel emu Android 14): **OUTCOME Passed** (no crashea con misiones). Botones 3D confirmados en test/paywall/repaso. (Home se capturó en shimmer — el crawl libre no esperó la carga; por eso el assert dirigido es la verificación válida, no el screenshot del crawl.)
- ✅ **PR #1 mergeado a master**; robo CI **arreglado** (`--no-record-video=false`→`--record-video`) — la auto-QA real corre end-to-end por 1ª vez.

Bumps conservadores anclados a esa evidencia:
- **Retención 40→46** (+6): misiones diarias shippeadas + verificadas por test dirigido (render) + no-crash en real. NO sube más porque: liga sigue placeholder, sin notis, recompensa sin XP server-side, y el assert dirigido aún corre en JVM (Robolectric), no en instrumentation sobre dispositivo real (próximo gate del motor).
- **Tech 72→76** (+4): robo CI reparado + auto-QA real end-to-end + 11 tests nuevos (incluido el 1er test de UI dirigido de una pantalla concreta).
→ **Global ~50/100**

## 🔒 Regla de oro del score (anti reward-hacking) — el motor honesto
Investigación 2026 (RLVR / verifiers): el mayor fallo de un loop de auto-mejora con IA es **hacer reward-hacking de su propio scorecard** (inflar la nota sin señal externa). Antídoto, ahora ley del loop:
1. **Una nota sube SOLO con señal externa, basada en reglas, difícil de falsear:** test dirigido VERDE + robo `Passed` + (idealmente) screenshot que muestre el cambio. Nunca por "se ve mejor".
2. **El test debe ejercer el camino REAL de producción** (isomorfismo): construir el estado como en la app (p.ej. `DailyMissions.build`), no objetos falsos a mano. Prohibido borrar/relajar asserts para pasar.
3. **La suite de evals CRECE con cada feature** (cada pantalla nueva → su test dirigido), o el techo lo pone la cobertura, no la capacidad real.
4. **Cero-crédito si el render es inválido** (crash, pantalla vacía) aunque el build pase.

## Próximo objetivo del loop = la nota más baja
Lo más bajo accionable client-side: **UX (45)** (cuello de botella percibido) y **Retención (46)**. Contenido (35) es BACKEND (escalado). → **Siguiente:**
1. **Motor:** elevar el assert dirigido de misiones a **instrumentation en Test Lab (dispositivo real)** → recién ahí Retención sube más. Es el upgrade que cierra la brecha "robo solo prueba no-crash".
2. **UX 45→:** botones 3D **planos aún en Login** (visto en robo), Paywall/GuestLesson, FluentaCard, mascota en bienvenida/estados vacíos.
3. **Retención:** liga real, notis inteligentes.

## Honestidad de método
Cada nota se sube SOLO con evidencia real (Firebase Test Lab / código verificado), nunca con datos falsos. Si una mejora no se puede verificar en dispositivo real, no cuenta para subir la nota.
