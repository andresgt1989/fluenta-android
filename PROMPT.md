# Fluenta Android — Plan Maestro (guía permanente)

> Esta app NO reemplaza WhatsApp. WhatsApp es el canal diario de cero fricción.
> La app es el **complemento visual, analítico y de práctica intensiva** que WhatsApp
> no puede dar. Objetivo: ser líder mundial — competimos con Duolingo (peor pedagogía)
> y Preply (sin IA diaria). Diferenciador: pedagogía correcta (Krashen i+1, Swain output
> forzado, FSRS, marcos de nivel reales) + cobertura de 113 verticales + fonética seria.

## Arquitectura de trabajo
- Backend monorepo: `/opt/alturya-incubator/apps/fluenta` (Hono + Drizzle + Postgres + Redis, PM2 id=17, puerto 3010, prod en https://fluenta.alturya.com).
- App: este repo `fluenta-android` (Kotlin + Jetpack Compose Material3 + Retrofit + DataStore).
- El VPS NO tiene Android SDK → no compila aquí. Flujo: editar en VPS → commit/push → el fundador hace `git pull` + `.\gradlew assembleDebug` + Run en Android Studio (Windows). Pega errores → se arreglan en VPS.

## Reglas de calidad (no negociables)
1. **Nada está "listo" hasta que compile (`assembleDebug` exitoso) y corra.**
2. **Antes de usar cualquier dato, confirmar el endpoint con curl.** Si falta, crearlo en `apps/fluenta` (typecheck + pm2 restart + verificar) antes de tocar la app.
3. **No inventar reglas lingüísticas.** Cada idioma tiene su marco y fonética reales (ver abajo). Investigar, no improvisar.
4. **Datos honestos**: si el backend devuelve null/vacío, mostrar estado vacío claro — nunca números inventados.
5. Trabajar por capas, con commit + push por capa. Cada capa debe compilar antes de seguir.

## Marca
Verde Fluenta: primario `#166534` / `#14532d`, brillante `#22c55e`, claro `#dcfce7`. Material3 con tema propio (claro + oscuro), tipografía con jerarquía, componentes branded. NO el morado por defecto.

## Marcos de nivel por vertical (verificado vía /api/languages, 113 pares)
- **CEFR** (101 pares: es, en, pt, fr, it, de, de-ch, nl, ru, uk, pl, el, sv, da, fi, et, lt, ar): A1 → A2 → B1 → B2 → C1.
- **HSK** (zh / chino): HSK 1 → 6 (HSK1=150 palabras … HSK6=5000).
- **JLPT** (ja / japonés): N5 → N4 → N3 → N2 → N1.
- **TOPIK** (ko / coreano): definido en enum, sin pares activos aún. Niveles 1-6.
Helper en `util/LevelLabels.kt`. Backend guarda `level` como CEFR (a1-c1) para TODOS los pares; mapear a marco destino en la app.

## Investigación lingüística por idioma (hechos establecidos — usar, no inventar)
- **Chino (Mandarín)**: 4 tonos + neutro. T1 alto plano (ˉ), T2 ascendente (ˊ), T3 bajo-descendente-ascendente (ˇ), T4 descendente (ˋ). Pinyin + caracteres simplificados. Tone sandhi: 3+3→2+3; 不 bù y 一 yī cambian según contexto. Orden de trazos del Hanzi (dataset: Make Me a Hanzi / Hanzi Writer). Pronunciación tonal = 2ª lección (fosilización temprana es crítica).
- **Japonés**: **pitch accent** (no tonos): patrones de mora alta/baja — heiban (plano), atamadaka (cabeza alta), nakadaka (media), odaka (cola). 3 escrituras: hiragana, katakana, kanji. Orden de trazos (dataset: KanjiVG). Romaji solo en N5/N4.
- **Coreano**: Hangul (alfabeto featural en bloques silábicos), sin tono léxico (Seúl estándar). Orden de trazos del Hangul.
- **Árabe**: RTL. Consonantes enfáticas/faringalizadas (ص ض ط ظ ق ع ح). Vocales cortas (harakat) normalmente no escritas. Letras solares/lunares para "al-". MSA vs dialectos (egipcio, golfo, levantino, magrebí) — detectar dialecto, enseñar en MSA. CEFR en este sistema.
- **Europeos (CEFR)**: A1-C1. Foco en colocaciones (chunks), no palabras sueltas (Principio 4 del doc).

## Endpoints backend (estado)
Verificados en vivo: `/api/auth/*`, `/api/user/profile|progress|errors|skills`, `/api/curriculum/map`, `/api/languages`, `/api/languages/select`, `/api/lessons/next`, `/api/stripe/portal|checkout`.
Por crear cuando la capa lo requiera: `/api/tts` (audio fonética, envuelve `generateSpeech` de @alturya/ai), `/api/notifications/register` (existe, falta columna FCM en DB), `/api/teachers/apply` (captación de profesores), endpoints de ejercicios interactivos.

## Plan por capas
- **Capa 0 — Fundación + marca** ✅/en curso: tema Material3 verde (claro+oscuro), tipografía, componentes branded, manejo de estados (loading/error/empty), caché offline (DataStore→Room) de profile + curriculum.
- **Capa 1 — Fonética por idioma**: endpoint `/api/tts`; reproductor de audio; pantalla de pronunciación con tonos (zh), pitch (ja), fonemas enfáticos (ar). Grabación + comparación (fase posterior con waveform).
- **Capa 2 — Escritura de caracteres** (zh/ja/ko): trazado con orden correcto sobre Canvas (Hanzi Writer / KanjiVG / Hangul).
- **Capa 3 — Ejercicios interactivos**: opción múltiple, emparejar, completar, ordenar, escucha. Conectados al currículo.
- **Capa 4 — Handoff + retención**: deep links `fluenta://app/{ruta}` bidireccionales WhatsApp↔app, FCM push (racha, lección diaria, milestones), streak/freeze.
- **Capa 5 — Captación de profesores**: formulario de lead + lista de espera (marketplace IA+humano viene después, ver doc línea 897).
- **Capa 6 — Pulido**: Crashlytics, analytics, localización de interfaz por idioma, accesibilidad, animaciones.

## Estado de pantallas (MVP ya construido)
Login OTP ✅, bottom-nav (Inicio/Mapa/Progreso/Perfil) ✅, Home (nivel+stats+siguiente lección+handoff WhatsApp) ✅, Mapa de currículo ✅, Progreso (radar de 4 subhabilidades + tablero SRS) ✅, Perfil (plan+Stripe+cambiar idioma+logout) ✅, Selector de 113 idiomas ✅.
