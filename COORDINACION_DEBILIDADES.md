# Fluenta — Cubrir debilidades hacia 100/100 (FODA · CAME · Pareto) — 2026-06-23

Basado en evidencia real: probe del backend (api/curriculum/map por idioma) + capturas Firebase. NO opinión.

## Dato duro (probe 2026-06-23)
- Currículo profundo: **en** (75 unid/~450 lecc), **zh** (60/~360).
- Stub 5 unid/~30 lecc: pt, fr, de, it, ja, ko, ar, ru, hi, tr, nl, pl, el, id, vi, uk, es.
- Casi vacío: sv (1/6).
- → 18/20 idiomas terminan en callejón tras 5 unidades.

## FODA
**Fortalezas:** en+zh con currículo profundo; UX ya coherente y verificada en Firebase (Bienvenida Hoot, espina/Mapa, NavBar teal, Perfil); wedge de voz; 20 idiomas expuestos en UI.
**Oportunidades:** motor de contenido "research-grounded + gate PhD" ya existe (CLAUDE.md) → escalar generación; demanda alta en es/fr/de/pt/ja/ko.
**Debilidades:** 18/20 sin profundidad; sv casi vacío; estados vacíos con emoji marrón (no Hoot); auto-avance solo parcial; Login = 2ª bienvenida; Ajustes sin confirmar en captura; "nombre" muestra id de dispositivo.
**Amenazas:** churn/desconfianza al chocar callejón; un inversor/reviewer detecta "20 idiomas" hueco; retención D1 cae.

## CAME
- **Corregir (debilidades):** generar currículo profundo para los idiomas de mayor demanda; Hoot en estados vacíos; auto-avance universal; confirmar Ajustes en Firebase.
- **Afrontar (amenazas):** mientras se genera contenido, **etiquetar honestamente** los idiomas: "Completo" vs "Beta · pronto más" en onboarding/selector, para que nadie sienta engaño ni choque mudo. (Barato, alto impacto en confianza — lo hace T3.)
- **Mantener (fortalezas):** no romper en+zh ni la UX/espina ya verificada; cada cambio re-verificado en Firebase.
- **Explotar (oportunidades):** usar el motor de contenido para generar en lote; liderar con los idiomas profundos.

## Pareto (el 20% que da el 80%)
1. **Etiqueta honesta de cobertura** (T3, 1 iter) — elimina la amenaza de callejón mudo para los 18, sin esperar contenido. ALTÍSIMO ROI.
2. **Generar profundidad para top-6 de demanda** (es, fr, de, pt, ja, ko) — cubre la mayoría de usuarios reales. (T3b, motor de contenido.)
3. **Auto-avance universal + Hoot en vacíos** (T3/T3c) — cierra la sensación de "no hay rumbo".

## Reparto de tareas (otras terminales)
### T3b — CONTENIDO (lo más crítico)
- Usar el motor de contenido (gate PhD) para generar currículo **profundo (≥40 unidades A1→B1)** para, en orden: **es, fr, de, pt, ja, ko**.
- Verificar por API: `POST /api/languages/select {l2}` + `GET /api/curriculum/map` debe devolver ≥40 unidades por idioma. Reportar conteo real por idioma.
- Subir sv de 1→≥5 mínimo o marcarlo "Próximamente".

### T3c — FLUJO/PANTALLAS
- **Auto-avance universal**: al terminar lección/Repaso/Conversación → siguiente nodo o volver a la espina (Mapa), nunca popBackStack mudo.
- Implementar **"Fluenta Test de Nivel.dc.html"** (mock en /opt/fluenta-claude-design/project/) en DiagnosticScreen.
- Verificar cada uno en Firebase (robo) + captura.

### T3 (yo) — UX + honestidad de cobertura
- Etiqueta "Completo/Beta" por idioma en onboarding + selector (usa el conteo del probe).
- **Hoot de marca en estados vacíos** (hoy emoji marrón).
- Confirmar **Ajustes** en captura Firebase.
- Pulir: ocultar id de dispositivo como "nombre".

## Regla transversal
Nada se sube a "hecho" / nota EVALUACION_UNICORNIO sin **captura Firebase real** (o, para contenido, conteo real por API). 100/100 = verificado, con los 20 idiomas usables (profundos o honestamente etiquetados).

## Tareas de CLAUDE DESIGN (mocks a generar por el usuario)
Prioridad por impacto:
1. **Login / Crear cuenta** (NO existe mock; hoy el Login es una 2ª bienvenida fea que duplica el value-prop). Necesito: pantalla de entrada con email + Google + "Probar sin cuenta", SIN repetir el hero de la Bienvenida. Kit teal/3D/Hoot.
2. **Cobertura de idioma — chip "Completo / Beta · pronto"** para el selector y onboarding (un badge sobre cada idioma según profundidad de currículo).
3. **Transición "¡Lección completada! → Siguiente"** (celebración breve que empuje al siguiente nodo de la espina) — refuerza el auto-avance. (Si LessonPlayer Result ya cubre, indícalo.)
4. **Estados vacíos/cargando/error con Hoot** unificados (Repaso/Match/Home/Progreso) — hoy Repaso usa emoji marrón.
5. (Opcional) **Liga/Leaderboard** real para Progreso (hoy placeholder).

Cuando estén en el proyecto "Fluenta Language Learning App" o en un zip de handoff, T3 los implementa 1:1 y verifica en Firebase.

## es→zh — Cobertura HSK (medido en DB 2026-06-23)
Vocab distinto por nivel CEFR (≈chunks, no palabras sueltas): a1=235, a2=250, b1=250, b2=250, c1=250, c2=249. Total ≈1484.
Acumulado vs HSK: HSK1(150)✅ HSK2(300)✅ HSK3(600)✅ HSK4(1200)⚠️ HSK5(2500)❌ HSK6(5000)❌.
TAREA terminal chino:
1. Generar vocabulario B2/C1/C2 hasta 2500 (HSK5) y 5000 (HSK6) palabras únicas.
2. Validador real: tokenizar target_vocabulary a PALABRAS y cruzar con la wordlist HSK oficial; reportar % cobertura por nivel (no contar chunks).
