# Claude Design — paquete de mocks para llegar a 100/100 (Fluenta)

Genera estos en Claude Design (proyecto "Fluenta Language Learning App" o un zip de handoff) con el kit existente: teal #0E9D8E / #0A6F64, mint #CDEEE6 / #E4F6F1, ink #0F2E27, ámbar #F6A623, Plus Jakarta Sans, Noto Sans SC, botón 3D (sombra inferior), mascota Hoot (búho teal), estados WCAG AA + RTL + es/en/pt (+40% texto). T3 los implementa 1:1 y verifica en Firebase.

## YA TENGO MOCK (no regenerar) — solo falta implementarlos
- Test de Nivel (`Fluenta Test de Nivel.dc.html`) — pendiente de portar en T3.
- Onboarding, LanguageSelector, GuestLesson, LessonPlayer (Teach/Quiz/Result), Conversation, Script, HanziReview, Paywall, Match, Progress, HOME, Repaso, Bienvenida, NavBar, Perfil y Ajustes — ya implementados.

## FALTAN MOCKS (genera estos) — prioridad por impacto
1. **Login / Crear cuenta** ⬅️ EL MÁS IMPORTANTE
   - Hoy el Login es una 2ª bienvenida fea que duplica el value-prop. Necesito una pantalla de ENTRADA real:
   - Campos: email (OTP) + botón Google + enlace "Probar sin cuenta".
   - SIN repetir el hero/owl grande de la Bienvenida (ya la vio). Encabezado compacto, foco en los métodos de entrada.
   - Estados: idle, enviando código, código enviado (input de 6 dígitos), error.

2. **Liga / Leaderboard semanal** (retención — eje 4)
   - Tabla top-10 con avatares, tiers (Bronce/Plata/Oro/Diamante), tu fila destacada, "termina en N días", XP para ascender. (Hoy es placeholder.)

3. **Misiones diarias / Daily quests** (retención)
   - 3 misiones del día con progreso y recompensa XP; estado completada.

4. **"¡Lección completada!" → Siguiente** (transición de auto-avance)
   - Celebración breve (Hoot feliz, XP ganada, racha) con CTA grande "Siguiente lección →" que empuja al siguiente nodo de la espina. (Refuerza el camino fijo.)

5. **Estados vacío/cargando/error UNIFICADOS con Hoot** (polish)
   - Plantilla única (Hoot normal/triste + título + texto + CTA 3D) para Home/Repaso/Match/Progreso/Mapa. (Ya reemplacé el emoji marrón por Hoot dibujado; esto lo formaliza.)

6. (Opcional) **Perfil — editar** y **chip de cobertura "Completo/Beta/Pronto"** como componente, para alinear con lo que ya cableé.

## Cómo entregarlo
Exporta un zip de handoff (como el anterior) o súbelo al proyecto. Avísame y lo implemento + verifico en Firebase, uno por uno, en el loop.
