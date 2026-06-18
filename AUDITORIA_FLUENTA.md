# Auditoría real de Fluenta — por qué se siente 1/100 y cómo llegar a 100/100

Fecha: 2026-06-18 · Basado en: lectura del código de navegación real + 21 screenshots renderizados + flujo de primer arranque + benchmarks Duolingo/Babbel/Busuu.

---

## 0. La verdad incómoda primero

Las "auditorías por screenshot" anteriores se veían bien **porque los tests usan datos falsos** (`previewState` con `l2="en"` e inglés bonito hardcodeado en `ScreenshotTest.kt`). La app real carga tu perfil del servidor, y si ese perfil tiene `l2="zh"`, **todo sale en chino**. Por eso yo veía pantallas correctas y tú veías basura. Las capturas NO reproducían tu realidad. Eso ya está identificado y es la raíz del problema de credibilidad.

---

## 1. Los 3 bugs estructurales del arranque (causa raíz)

### BUG 1 — Usuario con cuenta NUNCA vuelve a elegir idioma
`MainActivity.kt:130-134`:
```kotlin
startDest = when {
    tok != null   -> "main"        // tienes token → directo a HOME, sin preguntar nada
    onboardingDone -> "login"
    else          -> "onboarding"  // elegir idioma SOLO si eres 100% nuevo
}
```
Tú ya creaste cuenta en una prueba anterior → tienes token → la app te tira directo a Home con tu perfil viejo (`l2=zh`). Nunca te pregunta el idioma porque cree que ya lo elegiste. **Este es el "empieza en chino por default".**

### BUG 2 — Cambiar idioma NO refrescaba nada (CORREGIDO esta sesión)
`LanguagesViewModel.select()` hacía POST del idioma y `popBackStack()`, pero `HomeViewModel.load()` solo corre una vez (en `init`). Volvías a Home y seguía el chino viejo hasta reiniciar la app.
**Fix aplicado:** señal `Session.requestReload()` que dispara `vm.load()` en Home y en el Mapa cuando cambias de idioma. Ya está compilado y desplegado en `beta.apk`.

### BUG 3 — El default del servidor cuando no hay idioma elegido
Las 4 rutas de registro (`LoginViewModel`) aplican `chosenL2` SOLO si no es null. Si entras por "Empezar gratis" sin pasar por la selección de idioma, `chosenL2=null` y **el servidor asigna un default** (aparentemente el primer currículo sembrado, que es chino). Hay que forzar selección de idioma antes de crear cuenta, o que el server defaultee a un par seguro según el idioma del dispositivo (es→en).

---

## 2. Inventario: qué tiene HOY (pantalla por pantalla, lo que sí está)

| Pantalla | Estado real | Veredicto |
|---|---|---|
| Login | "Empezar gratis" + guest + email/Google/teléfono | ✅ Bien diseñada |
| Onboarding | 3 pasos: bienvenida → hablas (es/en/pt) → aprendes (16 opciones) | ✅ Existe pero se SALTA si tienes token |
| Home | Coach IA, progreso a nivel, stats, repaso SRS, paywall | ⚠️ Depende 100% del perfil del server |
| Mapa currículo | Camino zigzag por unidades con tipos de lección | ✅ Bien, labels ya no se truncan |
| Lección (teach) | Tarjetas de vocab con audio TTS | ✅ |
| Lección (quiz) | Traducir/opción múltiple/ordenar/emparejar/escuchar/hablar | ✅ Variedad buena |
| Conversación | Chat IA con corrección + STT 27 idiomas + micro | ✅ |
| Diagnóstico CAT | Test adaptativo con "No sé" + ejemplo | ✅ |
| Repaso SRS | Tarjetas error→corrección con intervalos | ✅ |
| Progreso | Radar de habilidades, liga, errores por categoría | ✅ |
| Pronunciación | Grabar + score por fonema | ✅ |
| Verbos | 10 verbos/día con variantes | ✅ |
| Script (ja/zh/ar) | Aprender alfabeto + trazos + examen | ✅ quiz ya filtra a lo enseñado |
| Perfil | Plan, logros, referidos, share card | ✅ |
| Idiomas | Catálogo de pares con "con currículo" | ⚠️ Cambio ahora sí refresca (fix) |

**Conclusión del inventario:** el problema NO es falta de features. Hay muchísimo construido. El problema es que **el primer minuto está roto** (idioma equivocado, sin poder corregirlo fácil) y eso destruye toda la credibilidad antes de que veas lo bueno.

---

## 3. Criterios de una app de idiomas nivel unicornio

De Duolingo (churn 47%→28% con gamificación), Babbel (Babbel Speak IA 2025), Busuu (CEFR + feedback nativo):

1. **Valor en 30-60 segundos** — el usuario debe tener éxito en una micro-tarea ANTES de pedirle cuenta. Duolingo: ejercicio de traducción inmediato.
2. **Onboarding que pregunta el "por qué"** — meta personal (trabajo/viaje/familia) → personaliza el camino.
3. **Una sola conducta diaria** — "completa una lección hoy". Todo (rachas, ligas, notis, widgets) empuja a eso.
4. **Rachas + ligas + XP** — el motor de hábito. 55% de DAU vuelven por la racha.
5. **IA adaptativa** — dificultad siempre calibrada (ni aburre ni quema).
6. **SRS real** — repaso espaciado con intervalos por desempeño.
7. **Speaking con reconocimiento** — feedback de pronunciación inmediato.
8. **Test de nivel CEFR** — empezar en el punto correcto.
9. **A/B testing constante** — iterar con datos, no con saltos.
10. **Freemium con upsell** — 80% de ingresos por suscripción.

Fluenta YA tiene 4,5,6,7,8,10. Le falta solidez en 1,2,3,4 y todo el 9.

---

## 4. Gap analysis — qué falta para 100/100

### P0 — Bloqueantes del primer minuto (sin esto, nada importa)
- [ ] **BUG 1**: si el perfil llega con un idioma que el usuario no eligió explícitamente, ofrecer "¿Aprendiendo el idioma correcto?" en grande en Home. Banner dismissable.
- [ ] **BUG 3**: el server NO debe defaultear a chino. Si `chosenL2=null`, defaultear a un par seguro por locale (es→en, pt→en, en→es).
- [ ] Forzar paso de idioma antes de "Empezar gratis" si nunca se eligió.
- [ ] **Reproducir el estado real en screenshots** — añadir tests con `l2="zh"` y progreso 0, para que el QA visual refleje la realidad y no inglés falso.

### P1 — Hábito y retención (el motor unicornio)
- [ ] Onboarding pregunta la META (trabajo/viaje/familia/examen) y la usa en el coach.
- [ ] Recordatorio diario inteligente (notificación a la hora que el usuario suele practicar).
- [ ] Liga semanal REAL con otros usuarios (hoy se ve placeholder "0/1").
- [ ] Pantalla de "racha en riesgo" con freeze/escudo (ya hay banner; falta el escudo Pro).
- [ ] Widget de inicio "lección de hoy".

### P2 — Profundidad de contenido (Babbel/Busuu)
- [ ] Datos culturales / "sabías que" por lección.
- [ ] Pares mínimos en pronunciación.
- [ ] Más tipos de ejercicio por lección.
- [ ] Feedback de comunidad / nativos (diferenciador Busuu) — opcional, requiere backend.

---

## 5. Cobertura 3 → 20 idiomas

**Origen (L1) hoy:** es, en, pt (3) ✅ — coincide con el objetivo.

**Destino (L2) hoy en onboarding:** en, es, pt, fr, de, it, ja, zh, ko, ar, ru, hi, tr, nl, sv, pl = **16**.

**Para llegar a 20 destino**, faltan 4. Candidatos por demanda: **vietnamita (vi), indonesio (id), tailandés (th), griego (el)** o **hebreo (he), polaco ya está, ucraniano (uk)**.

**Requisitos por cada par nuevo (no basta agregar el código):**
1. Currículo sembrado en backend (badge "con currículo").
2. Sistema de nivel correcto (CEFR / JLPT / HSK / TOPIK / según idioma).
3. Mapeo STT BCP-47 (ya hay 27 en `ConversationViewModel`).
4. Para no-latinos (th, he…): pantalla de script + detección de rango Unicode.
5. Banderas + nombres en `flag()` / `langName()`.
6. RTL si aplica (he, ar ya cubiertos por `isRtl()`).

**Acción concreta:** definir los 4 idiomas finales, sembrar currículo en backend, y añadir al `TARGET_LANGS` de `OnboardingScreen.kt`. El cliente ya está casi listo para escalar.

---

## 6. Roadmap priorizado (orden de ataque)

1. **P0 completo** — arreglar el primer minuto (idioma correcto siempre). Sin esto seguimos en 1/100 por más features que haya.
2. **Screenshots con datos reales** — para que cada cambio se valide contra la realidad, no contra inglés falso.
3. **P1 hábito** — meta en onboarding + notificación inteligente + liga real.
4. **3→20 idiomas** — definir los 4 faltantes + sembrar currículo.
5. **P2 contenido** — profundidad cultural y ejercicios.
6. **A/B testing** — instrumentar el embudo activación→D1→D7.

---

## 7. Ya corregido en esta sesión
- ✅ BUG 2 (reload al cambiar idioma) — Home + Mapa se refrescan.
- ✅ Script quiz filtra solo caracteres enseñados.
- ✅ Mapa: labels ya no se truncan a media palabra.
- ✅ Home: "¡Empieza hoy!" en vez de 0/0/0 para usuarios nuevos.
- ✅ Vidas 3→5 (menos punitivo).
- ✅ Micrófono: botón accionable en vez de error muerto.
- ✅ Diagnóstico: ejemplo de pregunta para bajar ansiedad.
- ✅ Script árabe: nota "escritura próximamente".
