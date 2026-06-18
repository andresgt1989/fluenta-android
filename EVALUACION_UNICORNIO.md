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
