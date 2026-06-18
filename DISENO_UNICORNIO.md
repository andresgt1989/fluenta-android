# Fluenta — Producción del sistema de diseño (nivel unicornio)

Objetivo: subir la estética de 4/100 a nivel Duolingo/Babbel con un **sistema de componentes con firma propia**, aplicado a todas las pantallas. Verificado con Firebase Test Lab (app real). Investigación: paleta vibrante de marca + tipografía redondeada + **botón 3D táctil** + mascota con personalidad + mucho espacio + micro-animaciones.

Base que YA existe (y no se estaba usando): paleta (teal/coral/ámbar/púrpura/sky), 3 poses de mascota (hola/saluda/celebra), shapes redondeados.

## FASE 1 — Componentes firma (se usan en TODAS las pantallas)
1. ✅ `FluentaButton` — botón 3D táctil (cara + base, anim de presión). Variantes primary/success/danger/neutral.
2. ⏳ `FluentaCard` — tarjeta con borde inferior sólido (mismo lenguaje 3D), padding y radio consistentes.
3. ⏳ `FluentaTopBar` — cabecera con mascota + título, coherente en todas las pantallas.
4. ⏳ `MascotMoment` — la mascota reacciona (hola/celebra/anima) en bienvenida, acierto, error, vacío.
5. ⏳ `FluentaProgress` — barra de progreso gruesa, redondeada, con color de marca (lecciones/test/script).
6. ⏳ `OptionChip` / `SelectCard` — opción seleccionable con estado 3D (quiz, idiomas, diagnóstico).
7. ⏳ `SectionHeader` — encabezado de sección consistente (peso, color, espaciado).

## FASE 2 — Mascota con presencia (ya hay 3 poses)
8. Bienvenida: mascota saluda grande (no un icono de micro genérico).
9. Acierto: mascota celebra + confeti.
10. Error: mascota anima ("¡casi!") en vez de solo una X roja.
11. Estado vacío (repaso/errores): mascota en pose simpática, no un trofeo plano.
12. Home: mascota saluda con el nombre del usuario.

## FASE 3 — Refactor de pantallas a los componentes (orden por tráfico)
13. Onboarding (bienvenida + ¿qué hablas? + ¿qué aprender?) — botones 3D + mascota + banderas grandes.
14. Home — cabecera con mascota, tarjetas firma, un solo CTA dominante.
15. Lección (teach + quiz + feedback + resultado) — botones 3D, opciones 3D, celebración.
16. Conversación — burbujas con más carácter, header de escenario, arreglar "Conectando…".
17. Diagnóstico — opciones 3D, mascota guía.
18. Mapa — nodos con más vida, mascota al final de unidad.
19. Repaso / Progreso / Perfil / Ajustes / Idiomas / Script — aplicar sistema.

## FASE 4 — Micro-animaciones y deleite
20. Press 3D en todos los botones (ya en FluentaButton).
21. Entrada escalonada de tarjetas (fade+slide) al abrir pantalla.
22. Confeti + sonido + mascota en acierto/lección completa.
23. Transiciones de pantalla coherentes.
24. Racha: llama animada + celebración al subir.
25. Números que cuentan hacia arriba (XP, progreso).

## FASE 5 — Pulido fino (lo que separa 90 de 100)
26. Espaciado consistente (escala 4/8/12/16/24/32) en cada pantalla.
27. Jerarquía tipográfica clara (un H1 por pantalla, cuerpos legibles).
28. Estados de carga = skeletons de marca, no spinners genéricos.
29. Estados vacíos con personalidad (copy + mascota).
30. Dark mode pulido en todos los componentes firma.
31. Iconografía coherente (un set, mismo peso).
32. Banderas/idiomas con estilo consistente.
33. Accesibilidad: contraste, targets ≥48dp, contentDescription.

## Método de verificación
Cada lote → `gcloud firebase test android run --type robo ...` → bajar capturas → revisar nivel estético real, no datos falsos. (Ver memoria fluenta-admin-testing.)

## Progreso
- ✅ #1 FluentaButton (3D) creado + aplicado en Onboarding "Empezar" (verificado en onboarding.png).
- ⏳ Siguiente: FluentaCard + aplicar botón 3D a Home y Lección.
