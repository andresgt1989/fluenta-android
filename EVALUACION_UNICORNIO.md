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
| 6 | **Contenido** | **42** | Motor de contenido research-grounded: competencias A1–C2 + exámenes oficiales + **gate PhD probado** (rechaza basura). Falta: generar a escala (20×6) y wirear a la app. (Era 35.) |
| 7 | **Tecnología / fiabilidad** | **55** | Sin crashes en logcat (bien). Pero 65 fallos silenciosos (mejorando), offline parcial, sin tests que bloqueen regresiones. |
| 8 | **Negocio / monetización** | **50** | Stripe + planes + paywall existen. Paywall día-1 spam, sin precios localizados (PPP), prueba sin recordatorio de cobro. |
| 9 | **Viralidad / growth loops** | **40** | Referidos + share card existen, pero referidos fallaba/oculto; sin loop fuerte ni k-factor medido. |
| 10 | **Accesibilidad / i18n** | **42** | 1er touch-target arreglado+verificado (botón audio 32→48dp, `SpeakerButton`, test dirigido verde). i18n mejorando (era hardcodeado), RTL parcial, contraste y ~108 `contentDescription=null` aún con huecos. |

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

### Iteración 4 (tras: botones 3D en los CTA hero de Login)
Evidencia dura: build + `testDebugUnitTest` VERDE; robo **Passed**; **screenshot de dispositivo real** (robo ci-17, frame 7/8) que MUESTRA "Probar una lección ahora" y "Empezar gratis" con la base 3D (antes planos — comparado contra el robo anterior). Señal externa que cumple la Regla de oro.
- **UX 45→48** (+3): Login era la última pantalla de ENTRADA con botones planos → ahora 3D coherente con test/paywall/repaso. Primera impresión más pulida, verificada en dispositivo real. NO sube más: faltan FluentaCard (tarjeta firma), mascota en bienvenida/estados vacíos, micro-animaciones, Paywall/GuestLesson y los botones OTP secundarios.
→ **Global ~50/100** (el bump de UX es real pero pequeño; no inflo el global).

### Iteración 5 (tras: paso de META en onboarding)
Evidencia dura: build verde con **test dirigido** `onboarding_asks_goal_after_language` (maneja el `OnboardingScreen` REAL: welcome→idioma→ y hace assert de "¿Para qué quieres aprender?" + opciones) + robo **Passed** (no-crash) en dispositivo real.
- **Onboarding 58→62** (+4): ahora pregunta la meta/"por qué" (6 motivaciones), guardada client-side (`MotivationStore`); cierra el gap del scorecard. Falta: primer "win" hablado, y captura del first-run en dispositivo real (pendiente de instrumentation — ver abajo).
→ **Global ~51/100**.
- ⚠️ **Hallazgo del motor (2ª vez):** el robo libre NO alcanza pantallas con estado específico — la cuenta de Test Lab ya está onboardeada (l2=NL) → no llega al first-run; igual que Home cargó en shimmer. **Confirma que el #1 del motor es instrumentation dirigida** (navegar a propósito a la pantalla en dispositivo real). Mientras tanto, el test dirigido Robolectric (que ejerce el composable real) es la verificación válida por la Regla de oro.

### Iteración 6 (MOTOR: instrumentation dirigida VERDE en dispositivo real + Blaze)
Cerrada la brecha de verificación. Tras habilitar Blaze en `fluenta-testlab-2026` (estaba en Spark → cuota agotada; el billing no estaba enlazado), `DailyMissionsInstrumentedTest` corrió en hardware real:
- ✅ **OUTCOME Passed — 3 test cases passed** en `MediumPhone.arm-34-es-portrait` (Firebase Test Lab, `--type instrumentation`). Assert dirigido de que las 3 misiones renderizan con su progreso real **sobre un teléfono real** (no el crawl genérico, no JVM). Señal externa máxima de la Regla de oro.
- **Retención 46→50** (+4): este era el gate exacto que dije que faltaba en la iter 3 ("el assert dirigido aún corre en JVM, no en instrumentation sobre dispositivo real"). Cruzado con evidencia. Las Misiones diarias quedan verificadas end-to-end: lógica (unit) → render (Robolectric) → **render en dispositivo real (instrumentation)**. NO sube más: liga sigue placeholder, sin notis, recompensa sin XP server-side.
- **Tech 76→79** (+3): el motor ahora tiene capa 4 (instrumentation dirigida en dispositivo real, on-demand vía `testlab-instr.sh`) + pipeline cuota-graceful. Auto-QA real de pantallas concretas, no solo no-crash.
→ **Global ~53/100**.

## 🔒 Regla de oro del score (anti reward-hacking) — el motor honesto
Investigación 2026 (RLVR / verifiers): el mayor fallo de un loop de auto-mejora con IA es **hacer reward-hacking de su propio scorecard** (inflar la nota sin señal externa). Antídoto, ahora ley del loop:
1. **Una nota sube SOLO con señal externa, basada en reglas, difícil de falsear:** test dirigido VERDE + robo `Passed` + (idealmente) screenshot que muestre el cambio. Nunca por "se ve mejor".
2. **El test debe ejercer el camino REAL de producción** (isomorfismo): construir el estado como en la app (p.ej. `DailyMissions.build`), no objetos falsos a mano. Prohibido borrar/relajar asserts para pasar.
3. **La suite de evals CRECE con cada feature** (cada pantalla nueva → su test dirigido), o el techo lo pone la cobertura, no la capacidad real.
4. **Cero-crédito si el render es inválido** (crash, pantalla vacía) aunque el build pase.

### Iteración 7 (CONTENIDO = prioridad #1: motor de contenido en el backend)
Foco reorientado por decisión del CEO: **el contenido es lo crítico, el resto es accesorio.** Backend `/opt/alturya-incubator/apps/fluenta` (rama `admin/content-engine-evaluator`). Evidencia dura (tests + LLM real):
- **`competency-model.ts`** — competencias CEFR **A1–C2** (vocab por familias, gramática, can-do) ancladas en investigación (Milton/Meara, Nation, CEFR CV) + mapeo a exámenes oficiales (HSK/JLPT/TOPIK).
- **`language-standards.ts`** — exámenes oficiales reales por idioma/país (DELE, HSK, JLPT, TOPIK, Goethe, DELF…) = la "vara real".
- **`content-evaluator.ts`** — **gate PhD-pedagogía** (determinista + examinador LLM) cableado al generador: genera→evalúa→regenera con crítica→solo entra lo aprobado.
- **`learning-path.ts`** — report card: sitúa al alumno en su examen real + qué sigue hacia la meta.
- **C2 habilitado end-to-end** (el enum DB ya lo tenía; sin migración) + TTS `tts-1-hd`.
- ✅ **VERIFICADO con LLM real** (`verify-evaluator.ts`): unidad B2 buena `passed, score 82`; basura principiante `rechazada, score 30` (fuga + palabras sueltas). El gate anti-basura funciona end-to-end. **27 tests de contenido verdes, tsc 0 errores.**
- **Contenido 35→42** (+7): C2 alcanzable + meta anclada en exámenes reales + gate de calidad probado. NO sube más: falta generar el currículo a escala (20×6 pasando el gate) y wirear a la app.
→ **Global ~54/100**.

### Iteración 8 (TERMINAL 4 — a11y: 1er touch-target accesible verificado)
Carril dedicado de Accesibilidad/i18n (rama `a11y-i18n`), en paralelo a CONTENIDO. Hallazgo real de a11y: el botón de audio "Escuchar" en `ConversationScreen` tenía `Modifier.size(32.dp)` → área táctil **32dp, por debajo del mínimo WCAG de 48dp** (toque difícil para motricidad reducida). Arreglo + evidencia dura:
- ✅ Nuevo componente `ui/SpeakerButton.kt`: `IconButton` que mantiene el área táctil en **48dp** (vía `minimumInteractiveComponentSize`) aunque el ícono visual sea de 16dp; etiqueta traducible `contentDescription = I18nStore.t("convo.hear","Escuchar")`. Reemplaza el botón de 32dp en `ConversationScreen`.
- ✅ Test dirigido `A11ySpeakerButtonTest.speakerButton_isLabeled_andHasMinTouchTarget` (Robolectric) **VERDE** — `tests=1 skipped=0 failures=0`. Ejerce el composable REAL y hace assert de: etiqueta presente, `assertHasClickAction`, y **área táctil** `assertTouchWidthIsEqualTo(48.dp)` + `assertTouchHeightIsEqualTo(48.dp)` (mide el touch target, no los bounds visuales — el error que destapó la 1ª versión del test). `assembleDebug` compila.
- **a11y 40→42** (+2): un touch-target real arreglado y verificado por el camino de producción. NO sube más porque: ~108 íconos con `contentDescription=null` sin etiquetar, contraste/escala de fuente/RTL real sin auditar, y **falta el gate de instrumentation en dispositivo real** (este assert corre en JVM/Robolectric, no en hardware — mismo techo que tuvo Retención en iter 3 antes de la iter 6). Próximo: barrer touch-targets <48dp restantes + `contentDescription` de íconos interactivos.
→ **Global ~54/100** (el +2 de a11y es real pero pequeño; no mueve el global redondeado).

## Próximo objetivo del loop = CONTENIDO (prioridad #1) + nota más baja
**Contenido (42)** sigue siendo lo más bajo y es la PRIORIDAD. → **Siguiente:**
1. **CONTENIDO:** wirear `learning-path` a un endpoint (la app muestra "estás en HSK 3 → ruta a HSK 6"); planificador adaptativo del siguiente ejercicio; correr generación a escala pasando el gate; considerar modelo LLM más fuerte (hoy gpt-4o-mini). Ver memoria `fluenta-content-engine`.
2. **UX/Retención (accesorios):** Login OTP/GuestLesson 3D, FluentaCard, mascota; liga real, notis.

## Honestidad de método
Cada nota se sube SOLO con evidencia real (Firebase Test Lab / código verificado), nunca con datos falsos. Si una mejora no se puede verificar en dispositivo real, no cuenta para subir la nota.

### Iteración 3 (2026-06-22) — Entrada Claude Design + ESPINA de progresión (verificado en Firebase run 224414)
- **UX 45→62** (+17). Evidencia REAL (capturas dispositivo, galería /download/firebase.html):
  - Bienvenida Claude Design como 1ª pantalla (búho Hoot dibujado, CTA 3D) — shot 1.
  - **NavBar teal**: pestaña activa con pill mint #CDEEE6 + ícono teal #0A6F64 (ya no rojo) — shots 3,5.
  - **ESPINA: Mapa de Lecciones** con kit — "Mi mapa de lecciones", anillo de unidad 0/5, nodo actual ▶ con burbuja "Empezar ▸", bloqueados 🔒, conector — shot 4. (Resuelve "no hay camino fijo".)
  - Progress/Match/Repaso/Perfil/Conversación portados al kit.
  - Onboarding sin bienvenida duplicada (arranca en paso idioma).
- Falta para 100 (sigue): (a) Login es una 2ª bienvenida (sin mock Claude Design); (b) auto-avance tras CADA ejercicio (solo Match→ruta hecho); (c) Home con UNA acción primaria (hoy disperso); (d) esqueleto de carga gris sin marca (sin Hoot); (e) Perfil/Ajustes y Test de Nivel aún con diseño viejo (mocks ya disponibles en el zip).
- Global estimado ≈ 48/100 (UX deja de ser el tapón absoluto; siguiente cuello: Retención/auto-avance + Onboarding entrada).

### Iteración 4 (2026-06-22) — Perfil al kit (verificado en Firebase run 233217)
- **UX 62→66** (+4). Evidencia REAL (shot 9): "Mi perfil" con avatar búho Hoot dibujado, chip de nivel, 3 stats (Racha/XP/Nivel), tarjeta gradiente "Estás aprendiendo · Cambiar", compartir logro, Fluenta Pro, fila test de nivel; NavBar Perfil teal. Cableado intacto (checkout/badges/referral/logout).
- Polish menor pendiente: el "nombre" muestra el id de dispositivo (d:...) en cuentas demo; ocultar ids crudos para cuentas sin teléfono/email.
- Falta para 100: Ajustes (otro lado del mock) aún viejo; auto-avance universal; Login 2ª bienvenida; Test de Nivel viejo; esqueleto gris sin marca.

### Iteración 5 (2026-06-23) — Ajustes al kit (implementado; pendiente confirmación visual)
- Ajustes reescrito al kit (secciones blancas, icon-box, toggles teal, idioma/meta plegables). compile + tests verdes (picker test actualizado). El robo NO entró a Ajustes esta corrida → NO se sube nota hasta captura.
- HALLAZGO (shot 7, run 235847): estado vacío de Repasar usa búho EMOJI MARRÓN, no el Hoot teal. Reemplazar por mascota de marca en TODOS los estados vacíos (Repasar, Match, etc.).
- UX se mantiene en 66 (sin señal visual nueva). Próximo: Hoot de marca en estados vacíos (verificable) + confirmar Ajustes.
