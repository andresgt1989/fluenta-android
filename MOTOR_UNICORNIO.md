# 🛠️ MOTOR UNICORNIO — el loop autónomo robusto (cómo me auto-mejoro sin inflar)

> Especificación del motor que el agente en **modo admin Fluenta** ejecuta. Destila
> las mejores prácticas 2026 de loops agénticos autónomos y las adapta a esta app.
> Objetivo: que cada iteración **componga** sobre la anterior con evidencia real,
> sin reward-hacking del propio scorecard.

## Por qué este doc existe (la lección)
Un loop de IA que se auto-califica tiende a **inflar su nota** (reward-hacking del
verificador): subir el score sin señal externa dura, o "pasar tests" sin hacer el
trabajo. Pasó aquí (subí Retención antes de un assert dirigido). El motor robusto
ancla TODO a señales externas difíciles de falsear.

## El ciclo (TDD agéntico, red-green-refactor a velocidad máquina)
Por cada mejora, en orden, sin saltarse pasos:

1. **PLAN** — elijo la nota más baja accionable (client-side) en `EVALUACION_UNICORNIO.md`. Si no sé hacerlo de clase mundial, investigo ANTES de codear.
2. **TEST PRIMERO (red)** — escribo el **test dirigido** que fallará hasta que el cambio exista:
   - Lógica pura → unit test (JVM).
   - Pantalla concreta → **test de UI dirigido** (Robolectric `createAndroidComposeRule` + `onNodeWithText().assertIsDisplayed()`), construyendo el estado por el **camino real de producción** (isomorfismo), nunca objetos falsos a mano.
3. **CÓDIGO (green)** — implemento hasta que el test pasa + `./gradlew assembleDebug` compila.
4. **VERIFICACIÓN EN CAPAS (grounding multi-señal):**
   - Capa 1 — unit + UI dirigido en JVM (rápido, bloquea CI).
   - Capa 2 — `build-apk.yml`: compila + `testDebugUnitTest` (bloquea merge).
   - Capa 3 — `testlab.yml`: robo en **dispositivo real** → `OUTCOME Passed` (no-crash) + video + screenshots.
   - Capa 4 (gate fuerte, en construcción) — **instrumentation en Test Lab** que navega a propósito a la pantalla y hace assert ahí, en dispositivo real. Reemplaza el crawl genérico para lo que cambié.
5. **RE-SCORE HONESTO** — actualizo la nota SOLO con la evidencia de arriba (ver Regla de oro). Bump conservador, con la lista de evidencia citada.
6. **MERGE** — rama `admin/*` → PR → CI verde (build + robo) → merge a `master`. No dejo trabajo colgando.
7. **MEMORIA** — guardo lo no obvio. Vuelvo a (1).

## 🔒 Regla de oro del score (anti reward-hacking)
Una nota sube SOLO si hay **señal externa, basada en reglas, difícil de falsear**:
- ✅ Test dirigido VERDE que ejerce el camino real (no objetos falsos, no asserts borrados).
- ✅ Robo `Passed` (no-crash) en dispositivo real.
- ✅ Idealmente: screenshot/instrumentation que MUESTRA el cambio renderizado.
- ❌ Nunca por "se ve mejor", ni por # de cambios, ni por screenshots con datos inventados (Roborazzi de preview NO cuenta).
- ❌ Cero-crédito si el render es inválido (crash / pantalla vacía) aunque el build pase.
- 📈 La suite de evals **crece con cada feature**, o el techo lo pone la cobertura, no la capacidad.

## Estado del motor (capacidades / brechas)
**Tengo:** rama+PR+CI que bloquea · robo real auto en PR (reparado; **cuota-graceful**: si Test Lab se queda sin cuota diaria NO bloquea el merge, solo avisa) · unit tests · tests de UI dirigidos (`DailyMissionsUiTest`, `OnboardingFlowTest`) · **instrumentation lista** (`DailyMissionsInstrumentedTest` + `testlab-instr.sh`, on-demand) · scoring honesto en loop · deploy beta en 1 comando.
**Nota de cuota:** Firebase Test Lab (Spark) tiene **cuota diaria** de runs; con iteración rápida se agota. Por eso: el **gate duro de cada PR = build + tests dirigidos** (Robolectric, sin cuota); robo/instrumentation en dispositivo real son **best-effort/on-demand** y se corren cuando hay cuota (resetea ~diario). Una nota de dispositivo real solo sube cuando ese run corre VERDE — no antes.
**Brechas a cerrar (siguiente trabajo del motor):**
1. Correr `testlab-instr.sh` en VERDE cuando la cuota resetee → recién ahí sube Retención por evidencia de dispositivo real. (Infra ya compila; bloqueado hoy por `TEST_QUOTA_EXCEEDED`.)
2. Auto-captura+lectura de screenshot de la pantalla concreta (critic visual).
3. Telemetría real de retención (D1/D7) para cerrar el loop con datos de uso, no solo render.

## Fuera del alcance del motor (acción del usuario / backend)
- Auth limpia de `gh` (hoy depende del token incrustado en el remote).
- Contenido A1→C2 en 20 idiomas, mercado, infra de backend.
Estos se marcan y se escalan; el motor avanza en lo que controla (client-side + pipeline).

_Refs: TDD agéntico + eval suite que crece (Karpathy/AutoResearch loop); reward-hacking de verifiers (RLVR, ICLR 2026); grounding multimodal con critic visual y cero-reward a renders inválidos._
