# AUTOPROMPT — Loop de mejora UX de Fluenta (clase mundial / unicornio)

> Me lo aplico a mí mismo cada iteración. KPI único: que un usuario real recorra la app y sienta una experiencia **coherente, guiada y deleitable** nivel Duolingo+. No "cantidad de cambios".

## Reglas duras (no romper)
1. **Solo Claude Design.** Cada pantalla/botón/elemento sale de un mock `.dc.html`. Si no hay mock, NO lo invento: lo pido. Logic/wiring/bugs sí los hago sin mock.
2. **Nunca "hecho" sin Firebase.** Toda mejora UI se confirma con capturas en dispositivo real (robo) antes de reportar. Galería base64 en `/download/firebase.html` (las .png no se sirven directo por nginx).
3. **Build de prueba con desbloqueos.** Acceso demo (cuenta de dispositivo) + traversal de TODO: todos los niveles, todos los tests, cada ejercicio. Si algo está gated y bloquea el QA, lo desbloqueo en debug.

## El problema central a resolver (prioridad #1)
**No hay camino fijo.** El usuario pasa un ejercicio y no es redirigido al siguiente; la app dispersa el flujo. Fix:
- **Espina = Mapa de Lecciones** (CurriculumMap con kit Claude Design): nodos completado/actual/bloqueado, un único "siguiente" claro y pulsante.
- **Auto-avance universal:** al terminar CUALQUIER ejercicio (lesson, match, repaso, conversación, script) → volver a la espina con el siguiente nodo, o "Siguiente →" directo. Nunca un `popBackStack` a un callejón.
- **Home con UNA acción primaria** ("Continuar" → nodo actual), no 111 acciones dispersas.

## Loop por iteración
a. Recorrer la app REAL (Firebase robo sobre build demo) → galería.
b. Auditar con agentes-tester de UX (visual/consistencia vs Claude Design · flujo/pedagogía · a11y/i18n · retención) → backlog priorizado con evidencia (nº de captura).
c. Atacar el ítem de mayor ROI (empezando por la espina/auto-avance).
d. Implementar 1:1 desde el mock → `assembleDebug` verde + `testDebugUnitTest`.
e. Re-correr Firebase → confirmar en captura. Re-calificar `EVALUACION_UNICORNIO.md` con señal real.
f. Actualizar `ESTADO_ACTUAL.md`. Volver a (a).

## Áreas a llevar a 100 (revisar cada esquina)
Entrada (Welcome✓ · login dup ✗) · Espina/Path ✗ · Auto-avance ✗ · Home foco ✗ · Lecciones (Teach/Quiz/Result) · Conversación · Repaso/SRS · Match · Progreso/Liga · Perfil/Ajustes (mock listo) · Test de Nivel (mock listo) · NavBar (teal, verificar en real) · vacíos/cargando/error con Hoot · a11y/contraste/i18n es-en-pt · RTL.
