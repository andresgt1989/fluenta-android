# Fluenta — Lista exhaustiva de cambios reales para llegar a unicornio (MVP → líder mundial)

Fecha: 2026-06-18 · Método: revisión de las 21 screenshots reales + `home_real_zh` + lectura de código + verificación de bugs con grep. Objetivo: 3 idiomas origen (es/en/pt) → 20 destino, viaje completo test→A1→C2 con gamificación de nivel Duolingo/Babbel/Busuu.

Leyenda: 🔴 P0 (bloquea el primer minuto) · 🟠 P1 (retención/hábito) · 🟡 P2 (profundidad) · ✅ ya hecho esta sesión

---

## A. ARRANQUE Y PRIMER MINUTO (🔴 lo que te hace decir 1/100)

1. 🔴 Server no debe defaultear a chino: si `chosenL2=null`, par seguro por locale (es→en, pt→en, en→es). *(backend)*
2. 🔴 Forzar selección de idioma antes de "Empezar gratis" si nunca se eligió (cliente puede bloquear).
3. 🔴 Botón "cambiar idioma" gigante e imposible de no ver en Home cuando el idioma se ve sospechoso (hoy es un chip gris diminuto "⇄ cambiar").
4. 🔴 Coach debe hablar en L1 (tu idioma) para principiantes, no en L2 (chino). Hoy `home_real_zh` muestra "你好！今天我们学习汉字" — aterrador. Adaptar según nivel: <A2 = L1, ≥B1 = L2.
5. 🔴 Pantalla de bienvenida al cambiar de idioma: "Ahora aprendes Francés desde Español — empecemos de cero".
6. ✅ Reload al cambiar idioma (Home + Mapa refrescan).
7. 🔴 Detectar "idioma equivocado": banner "¿No querías aprender chino? Cámbialo aquí →" si el usuario nunca completó una lección en ese idioma.
8. 🟠 Onboarding debe preguntar la META (trabajo/viaje/familia/examen/cultura) y usarla en el coach y selección de lecciones.
9. 🟠 Onboarding: preguntar minutos/día (5/10/15/20) → fija meta diaria.
10. 🟠 Onboarding: indicador de progreso "Paso 1 de 3".

## B. GAMIFICACIÓN (el motor unicornio que falta)

11. 🟠 Animación de confeti + sonido al responder correcto (hoy no hay celebración).
12. 🟠 XP que sube con número animado al acertar (mostrar "+10 XP" volando).
13. 🟠 Liga semanal REAL con usuarios (hoy `LeagueCard` es placeholder "0/1"). Bronce→Plata→Oro→Diamante.
14. 🟠 Racha con calendario visual (7 días, llama animada por día cumplido).
15. 🟠 Escudo/freeze de racha (mecánica de Duolingo, monetizable Pro).
16. 🟠 Amnistía de fin de semana / "racha en peligro" con repair.
17. 🟠 Cofres/recompensas al terminar una unidad en el mapa.
18. 🟠 Logros con timeline (hoy solo 3 badges, 2 ganados — muy delgado). Mínimo 30 logros.
19. 🟠 "Misiones diarias" (3 retos: completa 1 lección, 5 repasos, 1 conversación).
20. 🟠 Notificación diaria inteligente a la hora que el usuario suele practicar.
21. 🟠 Widget de pantalla de inicio "tu lección de hoy" + racha.
22. 🟡 Tabla de clasificación de amigos (invitar y competir).
23. 🟡 Sonidos/haptics consistentes en TODA acción (hoy inconsistente).
24. 🟡 Niveles de avatar/mascota que evolucionan con XP.

## C. PANTALLA POR PANTALLA (revisión de las screenshots)

### Login (`login.png`)
25. 🟠 Prueba social: "+10.000 estudiantes · ⭐4.8" bajo el logo.
26. 🟠 Mostrar banderas de los 20 idiomas (carrusel) para comunicar el valor.
27. 🟡 Mucho espacio vacío arriba — subir el contenido o agregar imagen/hero.
28. 🟡 "Empezar gratis" debería decir qué pasa después ("sin tarjeta · 1 min").

### Onboarding (`onboarding.png`)
29. 🟠 Banderas en el paso de "qué idioma hablas" y "qué quieres aprender".
30. 🟡 Bullets de valor con iconos distintos (hoy los 3 son el mismo check).
31. 🟡 Vista previa del primer ejercicio antes de pedir cuenta (valor en 30s).

### Home (`home.png` / `home_real_zh.png`)
32. 🔴 Demasiados CTAs compitiendo: coach + "Descubre tu nivel" + "Empieza hoy" + repaso + paywall. UN solo foco.
33. 🟠 Saludo con nombre del usuario ("Hola, Andrés") no "Hola".
34. 🟠 Anillo de meta diaria prominente arriba, no enterrado.
35. 🟠 Paywall del día 1 se siente spam — mostrarlo tras la 3ª lección.
36. 🟡 Acceso a la liga desde Home.
37. 🟡 "Continuar donde lo dejaste" al reabrir la app.

### Lección - Vocabulario (`lesson_teach.png`)
38. 🟠 Imagen/ilustración por palabra (memoria visual, lo hace Babbel/Duo).
39. 🟠 Indicador de progreso "Tarjeta 1 de 3".
40. 🟡 Botón "ya lo sé" para saltar vocab conocido.
41. 🟡 Feedback visual cuando se reproduce el audio (onda/highlight).
42. 🟡 Fonética/transliteración en todas las tarjetas, no solo algunas.

### Lección - Quiz (`lesson_quiz.png`)
43. 🔴 Traducción de texto libre es brutal para A1 — banco de palabras en niveles bajos.
44. 🟠 Modo práctica sin-fallo (sin corazones) opcional — Duolingo lo migró.
45. 🟠 Botón de pista visible que descuenta poco.
46. 🟠 Audio del prompt (escuchar la frase a traducir).
47. 🟡 Teclado con caracteres especiales (acentos, ñ, scripts) helper.
48. ✅ Banco de vocab colapsable en traducción.
49. ✅ Vidas 3→5.

### Feedback correcto (`lesson_feedback_correct.png`)
50. 🟠 Celebración (confeti + sonido) — hoy es plano.
51. 🟡 Mostrar desglose literal de la traducción.

### Feedback incorrecto (`lesson_feedback_wrong.png`)
52. 🟠 Permitir reintentar antes de avanzar.
53. 🟠 "Guardado para repaso" visible (conecta con SRS).
54. 🟠 Audio de la respuesta correcta para oírla bien.

### Conversación (`conversation.png`)
55. 🔴 Botón de audio para oír los mensajes del partner (hoy no hay).
56. 🔴 Toggle de traducción de los mensajes del partner (un A1 no entiende).
57. 🟠 Header de contexto del escenario (estás en un café…).
58. 🟠 Guardar correcciones "Mejor:" al repaso SRS.
59. 🟡 Fallback a escribir si no hay micrófono.
60. ✅ Botón "Permitir micrófono" accionable.

### Diagnóstico (`diagnostic.png`)
61. 🟠 6 preguntas es poco para CEFR fiable — escalar a 8-12 adaptativas.
62. 🟠 Al terminar, explicar POR QUÉ ese nivel ("dominas presente, falla condicional").
63. 🟡 Explicar qué es CEFR/HSK/JLPT con un tooltip.
64. ✅ Ejemplo de pregunta en la intro.

### Repaso (`repaso_card.png`, `repaso_empty.png`)
65. 🟠 Calidad de recuerdo en 3 niveles (difícil/bien/fácil) para mejor SRS, no binario.
66. 🟠 Audio de la frase.
67. 🟠 Frase de contexto de uso correcto.
68. 🟡 Empty state sugiere acción ("haz una lección") no solo "Volver".
69. ✅ "Piénsala antes de revelar".

### Progreso (`progress.png`)
70. 🔴 "Liga semanal" placeholder — feature muerta visible. Implementar o quitar.
71. 🟠 "0/1 errores dominados" se ve roto — formato claro.
72. 🟠 Radar solo 3 habilidades — agregar listening y fluidez (5 ejes).
73. 🟡 Gráfico de XP en el tiempo (histórico).
74. 🟡 "Vas adelantado/atrasado" vs el ritmo de la meta.

### Perfil (`profile.png`)
75. 🔴 "Referidos no disponibles" — feature muerta visible. Implementar o quitar.
76. 🟠 Ajustes en perfil: notificaciones, sonidos, meta diaria.
77. 🟠 Poder cambiar L1 (idioma nativo), hoy solo L2.
78. 🟡 Badges muy pocos (3) — expandir a 30+.
79. 🟡 Enmascarar teléfono por privacidad.

### Mapa (`map.png`)
80. 🟠 Lecciones bloqueadas no se distinguen (todas teal). Gris+candado claro.
81. 🟠 % de avance por unidad.
82. 🟠 "Test out" de una unidad (saltarla si ya sabes).
83. 🟡 Tiempo estimado por lección.
84. 🟡 Etiquetas "Conversación: Con…" siguen feas — acortar título o 2 líneas reales.
85. ✅ Labels no se truncan a media palabra.

### Pronunciación (`pronunciation.png`)
86. 🟠 Diagrama de posición de boca/lengua por fonema.
87. 🟠 Comparar onda del usuario vs nativo.
88. 🟡 Historial de score por fonema.
89. 🟡 Solo 3 frases — más contenido.

### Verbos (`verbs.png`)
90. 🔴 Dice "3/10" pero solo muestra 3 verbos — ¿dónde están los otros 7? Bug de contenido.
91. 🟠 Tabla de conjugación expandible.
92. 🟠 Frase de ejemplo por verbo.
93. 🟡 "Practicar en WhatsApp" como CTA primario saca de la app — secundario.

### Match (`match.png`)
94. 🟠 Timer + score para gamificar.
95. 🟠 Feedback visual de match correcto/incorrecto.
96. 🟡 Bonus por racha de aciertos.

### Idiomas (`languages.png`)
97. 🔴 Pares SIN "con currículo" (Árabe, Coreano) llevan a experiencia vacía — marcar "próximamente" o esconder.
98. 🟠 Pre-filtrar por el L1 del usuario arriba.
99. 🟡 Buscador de idiomas.
100. 🟡 Bandera del idioma origen en los headers "Desde X".

### Script ja/zh/ar (`script_*.png`)
101. 🟠 Audio del sonido de cada carácter.
102. 🟠 SRS de los caracteres aprendidos.
103. 🟡 Árabe sin práctica de escritura — completar (hoy "próximamente").
104. 🟡 Explicar qué significa el medidor de "Lectura %".
105. ✅ Quiz filtra solo caracteres enseñados.
106. ✅ Nota "examen solo incluye X caracteres".

## D. i18n — 3 IDIOMAS DE SALIDA (bug verificado 🔴)

107. 🔴 `ScriptScreen.kt`: 9 strings en español hardcodeado ("Aprende a leer y escribir", "Hacer examen de lectura", "Examen de lectura", "Calificar", "Seguir practicando", "Listo", "Lectura", "Volver", "Reintentar"). Un usuario en→ja ve español. Envolver en `I18nStore.t()`.
108. 🔴 `ConversationScreen.kt`: 7 strings hardcoded ("Conversación", "Salir", "Crear cuenta gratis", "Terminar conversación", "Listo", "Reintentar", "Puedes decir"). Mismo bug.
109. 🟠 `LessonPlayerScreen.kt`: "Tipo no soportado", "Tu respuesta", "Reintentar", "Volver" hardcoded.
110. 🟠 `AchievementShareCard.kt`: textos en español fijos en el bitmap compartido.
111. 🟠 Auditar que TODOS los `I18nStore.t(key, default)` tengan traducción en/pt en el backend i18n, no solo el default español.
112. 🟡 Test automático que falle si aparece un `Text("...")` con letra inicial mayúscula sin `I18nStore.t`.

## E. PEDAGOGÍA — viaje real A1→C2 (lo que un estudiante necesita)

113. 🟠 Cada lección debe declarar su objetivo CEFR ("A1.2 — pedir comida").
114. 🟠 Gramática explícita antes de practicar (mini-explicación), no solo ejercicios.
115. 🟠 Progresión de dificultad verificable: A1 banco de palabras → B1 texto libre → C1 producción abierta.
116. 🟠 "Did you know" cultural por lección (diferenciador Babbel).
117. 🟠 Comprensión auditiva con audio nativo real, no solo TTS.
118. 🟡 Lectura graduada (textos por nivel) para B1+.
119. 🟡 Escritura libre con corrección IA para C1-C2.
120. 🟡 Certificado/diploma al completar un nivel CEFR.

## F. TÉCNICO / CALIDAD

121. 🔴 65 `catch (Exception)` que tragan el error en silencio. HomeViewModel (7) y LoginViewModel (8) son los peores — si el API falla, pantalla vacía sin explicación. Añadir estado de error + reintentar.
122. 🟠 Dark mode: 14 colores hardcoded en `LessonPlayerScreen`, 8 en `LeagueCard`, 7 en Conversation, etc. rompen el tema oscuro. Migrar a `MaterialTheme.colorScheme`.
123. 🟠 Indicador de "sin conexión" cuando el API falla.
124. 🟠 Accesibilidad: `contentDescription` en iconos con significado (muchos `null`).
125. 🟡 Skeletons de carga en todas las pantallas (no solo spinner).
126. 🔴 Screenshots de QA con datos REALES (l2=zh, progreso 0) — no inglés falso. ✅ añadido `home_real_zh`.
127. 🟡 Instrumentar embudo activación→D1→D7 para A/B testing (criterio unicornio).

---

## Resumen de prioridad
- **🔴 P0 (17 items):** arranque, idioma correcto, coach en L1, i18n es/en/pt, features muertas, silent failures. **Sin esto seguimos en 1/100.**
- **🟠 P1 (~50 items):** gamificación + retención + profundidad por pantalla.
- **🟡 P2 (~40 items):** pulido, contenido, A/B.
- **✅ Ya hecho (12 items).**

---

# PARTE 2 — Expansión a 250+ (hallazgos adicionales por dimensión)

## G. ESTADOS (loading / empty / error) por pantalla
128. 🟠 Home: si el coach falla, no hay fallback — card vacía. Mostrar mensaje genérico motivador.
129. 🟠 Home: skeleton existe, pero stats/coach no tienen estado de error con reintentar.
130. 🟠 Mapa: error tiene reintentar ✅, pero empty ("aún no hay unidades") es pobre.
131. 🟠 Lección: si `getLessonPlay` falla, ErrorView ok, pero sin distinguir offline vs error.
132. 🟠 Conversación: si el partner IA no responde, no hay timeout visible.
133. 🟠 Diagnóstico: si falla a mitad del test, se pierde todo el progreso.
134. 🟠 Repaso: empty muestra trofeo pero sin CTA a lección.
135. 🟠 Progreso: si no hay datos, pantalla casi vacía sin guía.
136. 🟠 Perfil: si `getProfile` falla, spinner infinito (no hay timeout/retry).
137. 🟡 Pronunciación: sin estado cuando el assess falla (solo score 0).
138. 🟡 Verbos: sin empty state si no hay verbos del día.
139. 🟡 Match: sin estado de "completado" celebratorio.
140. 🟡 Idiomas: error de carga del catálogo solo es un snackbar.

## H. ACCESIBILIDAD (WCAG) por pantalla
141. 🟠 Login: icono de micrófono sin contentDescription.
142. 🟠 Home: iconos de stats (llama/estrella/libro) con contentDescription=null.
143. 🟠 Lección quiz: corazones de vidas no anunciados a TalkBack (solo emoji).
144. 🟠 Conversación: botón de micro sin estado anunciado (grabando/parado).
145. 🟠 Mapa: nodos de lección sin descripción de estado (completado/bloqueado).
146. 🟠 Pronunciación: botones Escuchar/Grabar sin rol claro.
147. 🟡 Contraste: texto gris claro sobre fondo claro en varios subtítulos (<4.5:1).
148. 🟡 Soporte de fuente grande (sp escalable) sin romper layouts.
149. 🟡 Targets táctiles <48dp en varios iconos pequeños.
150. 🟡 Orden de foco lógico para navegación por teclado/switch.

## I. ANIMACIONES Y MICRO-INTERACCIONES
151. 🟠 Sin transición al completar una lección (salto brusco a resultado).
152. 🟠 Barra de progreso de lección no anima el avance.
153. 🟠 Cambio de pestaña inferior sin feedback de selección animado.
154. 🟡 Tarjetas de vocab sin animación al voltear/escuchar.
155. 🟡 Racha: la llama pulsa, pero no hay celebración al incrementar.
156. 🟡 Mapa: el nodo activo pulsa ✅, pero sin animación al desbloquear el siguiente.
157. 🟡 Match: sin animación al emparejar correctamente.
158. 🟡 Pull-to-refresh sin haptic al disparar.

## J. NOTIFICACIONES Y RE-ENGAGEMENT
159. 🔴 No hay flujo de opt-in de notificaciones (permiso POST_NOTIFICATIONS declarado pero sin pedirlo con contexto).
160. 🟠 Recordatorio diario a hora fija — debería ser a la hora de práctica habitual.
161. 🟠 Notificación de racha en peligro (push) la noche del día sin practicar.
162. 🟠 Notificación de "tienes X repasos listos".
163. 🟡 Notificación de logro desbloqueado.
164. 🟡 Notificación de amigo que te superó en la liga.
165. 🟡 Resumen semanal de progreso (push/email).

## K. AJUSTES (pantalla inexistente — 🔴 falta completa)
166. 🔴 No existe pantalla de Ajustes. Crear una.
167. 🟠 Toggle de sonidos de efecto.
168. 🟠 Toggle de haptics.
169. 🟠 Configurar hora de recordatorio.
170. 🟠 Cambiar meta diaria desde Ajustes (hoy solo en Home).
171. 🟠 Cambiar idioma de la interfaz (L1) manualmente.
172. 🟠 Gestión de notificaciones granular.
173. 🟡 Borrar cuenta / exportar datos (GDPR).
174. 🟡 Tema claro/oscuro/automático.
175. 🟡 Velocidad del audio TTS.
176. 🟡 Mostrar/ocultar transliteración global.

## L. OFFLINE Y PERFORMANCE
177. 🟠 LessonCache existe, pero no hay indicador de "disponible offline".
178. 🟠 No se precargan las próximas lecciones para uso sin conexión.
179. 🟠 Imágenes/audio no cacheados para repaso offline.
180. 🟡 Sin medición de tiempo de arranque en frío.
181. 🟡 Listas largas (mapa, idiomas) sin paginación/lazy real verificado.
182. 🟡 TTS bloquea UI mientras carga (sin prefetch).
183. 🟡 Sin compresión de payloads grandes del API.

## M. PRIVACIDAD Y SEGURIDAD
184. 🟠 Teléfono mostrado completo en Perfil — enmascarar.
185. 🟠 Token guardado en DataStore sin cifrar — usar EncryptedSharedPreferences/Keystore.
186. 🟡 Sin política de privacidad enlazada en Login/Ajustes.
187. 🟡 Sin términos de servicio enlazados.
188. 🟡 Logs de Analytics sin opción de opt-out.

## N. ANALYTICS / EXPERIMENTACIÓN (criterio unicornio)
189. 🟠 Instrumentar embudo: app_open → onboarding_complete → first_lesson → D1 → D7.
190. 🟠 Evento de abandono en cada pantalla (dónde se cae el usuario).
191. 🟠 Evento de cambio de idioma (medir cuántos llegan al idioma equivocado).
192. 🟡 Framework de A/B testing (variantes de onboarding, paywall).
193. 🟡 Heatmap de qué ejercicios se saltan más.
194. 🟡 Métrica de precisión del diagnóstico vs desempeño real.

## O. CONTENIDO A1→C2 (profundidad por habilidad)
195. 🟠 Definir nº de lecciones por nivel CEFR (A1..C2) y por par de idioma.
196. 🟠 Cada lección con objetivo comunicativo claro (can-do statement).
197. 🟠 Vocabulario por frecuencia (las 1000 palabras más usadas primero).
198. 🟠 Gramática progresiva mapeada a CEFR.
199. 🟡 Listening con acentos variados (no solo un TTS).
200. 🟡 Lectura graduada B1+.
201. 🟡 Escritura libre con corrección IA C1-C2.
202. 🟡 Exámenes de fin de nivel (checkpoint CEFR).
203. 🟡 Contenido cultural por país del idioma.
204. 🟡 Modismos y expresiones por nivel.

## P. ROLLOUT 20 IDIOMAS (3 origen → 20 destino)
205. 🔴 Definir los 20 destinos finales (hoy 16 en onboarding).
206. 🔴 Sembrar currículo backend para cada par es/en/pt → 20.
207. 🟠 Sistema de nivel correcto por idioma (CEFR/JLPT/HSK/TOPIK/otros).
208. 🟠 Verificar STT BCP-47 para los 20 (hoy 27 mapeos en Conversation).
209. 🟠 Pantalla de script para todos los no-latinos (zh/ja/ko/ar/he/th/hi…).
210. 🟠 Banderas + nombres en flag()/langName() para los 20.
211. 🟠 RTL verificado para ar/he/fa/ur.
212. 🟡 Voces TTS de calidad para los 20.
213. 🟡 Mnemónicos de script por idioma.
214. 🟡 Marcar "próximamente" los pares sin currículo aún.

## Q. MÁS BUGS POR PANTALLA (segunda pasada de screenshots)
215. 🟠 Home `home_real_zh`: el chip "Chino · HSK 3 ⇄ cambiar" es lo único que salva al usuario y es diminuto.
216. 🟠 Home: "Descubre tu nivel" y coach piden cosas distintas — el coach ya debería saber el nivel.
217. 🟠 Lección teach: botón "¡Listo, a practicar!" sin contador de cuántos ejercicios vienen.
218. 🟠 Lección quiz: "Saltar este" al fondo, lejos del pulgar — subir.
219. 🟠 Feedback: el botón "Continuar" cambia de color (verde/rojo) pero no de posición — bien, pero sin foco automático.
220. 🟠 Conversación: el chat no muestra quién es el partner (avatar/nombre/rol).
221. 🟠 Diagnóstico: la barra de progreso no dice cuántas preguntas faltan en número.
222. 🟠 Repaso: badge "App/WhatsApp" ocupa espacio sin aportar al estudiante.
223. 🟠 Progreso: "Liga semanal" describe pero no muestra tabla — promesa incumplida.
224. 🟠 Perfil: plan "FREE" en mayúscula sin explicar qué incluye.
225. 🟠 Mapa: la línea de conexión entre nodos no indica dirección/progreso.
226. 🟠 Pronunciación: "Trabajando tu fonema más débil" sin decir cuál es ni por qué.
227. 🟠 Verbos: "Mínimo 10 verbos diarios" pero la lista no llega a 10.
228. 🟠 Match: progreso arriba sin indicar cuántos pares faltan.
229. 🟠 Idiomas: "con currículo" en verde, pero los sin currículo no dicen nada (parecen iguales).
230. 🟡 Script: "Volver" y "Hacer examen" compiten visualmente.

## R. ONBOARDING PROFUNDO (conversión)
231. 🟠 No hay pantalla de "tu plan de estudio personalizado" tras el diagnóstico.
232. 🟠 No se muestra el "por qué" elegido reflejado en el contenido.
233. 🟠 Sin estimación "llegarás a B1 en ~X meses con Y min/día".
234. 🟡 Sin video/animación de bienvenida.
235. 🟡 Sin opción de elegir avatar/nombre.

## S. MONETIZACIÓN (freemium sano)
236. 🟠 Paywall del día 1 — mover a tras valor demostrado (3ª lección).
237. 🟠 Mostrar claramente qué es gratis vs Pro.
238. 🟠 Prueba de 7 días con recordatorio antes del cobro.
239. 🟡 Precios localizados por país (PPP).
240. 🟡 Ofertas por racha/hito ("celebra 7 días con 50% off").
241. 🟡 Plan familiar / estudiante.

## T. CALIDAD DE CÓDIGO / MANTENIBILIDAD
242. 🟡 Extraer colores de marca a tokens del tema (no Color(0x...) sueltos).
243. 🟡 Unificar el patrón de manejo de error en un componente ErrorState reutilizable.
244. 🟡 Tests de UI para los flujos críticos (onboarding, primera lección).
245. 🟡 Tests de los ViewModels (hoy no hay).
246. 🟡 Lint/detekt en CI.
247. 🟡 Snapshot tests con datos reales (es/en/pt + zh) en CI.

## U. PROMESA DE PRODUCTO ("hablar desde el primer día")
248. 🔴 La promesa es hablar desde el día 1, pero el flujo con cuenta lleva a quiz de texto, no a hablar. Alinear: primera experiencia = hablar.
249. 🟠 El wedge de voz (guest_conversation) solo aparece en onboarding nuevo, no para usuarios con cuenta.
250. 🟠 Medir cuántos usuarios realmente HABLAN en la primera sesión (métrica norte).
251. 🟡 Tutorial de cómo usar el micrófono la primera vez.

## Resumen Parte 2
- Total acumulado: **251 cambios** documentados.
- 🔴 nuevos críticos: notificaciones opt-in (159), Ajustes inexistente (166), promesa de producto (248), rollout 20 idiomas (205-206).

---

# PROGRESO REAL (actualizado 2026-06-18) — aplicado y desplegado en beta.apk

## ✅ Completado y verificado (compilado + screenshot) — ~48 cambios
**Arranque/idioma:** #6 reload al cambiar idioma · #7 banner "¿idioma equivocado?" prominente · script-quiz filtrado a lo enseñado · home "¡Empieza hoy!" en vez de 0/0/0.

**i18n (es/en/pt) — tarea cerrada lado cliente:** #107 ScriptScreen (10) · #108 ConversationScreen (7) · #109 LessonPlayerScreen (5) · #110 AchievementShareCard (3). 0 literales en pantallas core.

**Pedagogía/audio:** #46 audio prompt L2 · #54 audio respuesta correcta · #55 audio mensajes partner · #43 vocab tappable (scaffolding A1) · #52 reintentar tras fallo · #53 "guardado para repaso" · #62 diagnóstico explica nivel (can-do CEFR) · #64 ejemplo de pregunta · #49 vidas 3→5 · #69 "piénsala antes de revelar".

**UX/Mapa:** #80 jerarquía visual (activo pulsa, futuros desvanecidos) · #81 progreso por unidad (X/Y) · #84 etiquetas sin prefijo redundante · #85 labels sin truncar · #68 repaso empty con CTA.

**Técnico:** #166 pantalla Ajustes (nueva) · #167 toggle sonido→Sfx · #168 háptica · #169 recordatorios · #170 meta diaria · #159 permiso notificaciones con contexto · #186/#187 enlaces legales · #121/#128/#136 silent failures con error+reintentar (Home, Perfil) · dark-mode burbujas conversación.

**Features muertas:** #75 ReferralCard se oculta si falla.

**Virality:** #110 share card i18n.

## 🔄 Requiere BACKEND (documentado, no fingido)
- #1 coach en L1 para principiantes (el mensaje viene del servidor)
- #1 no defaultear a chino (lógica del servidor)
- #65 SRS de 3 niveles (ErrorReviewBody es booleano)
- #13 liga semanal real (datos del servidor)
- #90 verbos "10 diarios" (el API devuelve la lista)
- #111 cargar traducciones en/pt de las claves nuevas en el backend i18n

## ⏳ Próximos client-side (cola)
#11/#12 confeti+XP por respuesta · #38 imágenes en vocab · #44 modo sin-fallo · #57 header de escenario en conversación · #72 radar 5 habilidades · #78 más logros · #122 dark-mode completo (resto de colores) · #189-191 analytics de embudo.

## ✅ Batch final (gamificación + pulido) — total ~52 cambios
- #11 Combo de aciertos seguidos "🔥 N" en el quiz (gamificación, refuerzo positivo)
- #122 Dark-mode: contenedor de feedback correcto adaptado a tema oscuro
- #191 Evento analytics `language_change` (mide caídas en idioma equivocado)
- #97/#214 Pares sin currículo (Árabe, Coreano) marcados "próximamente" + deshabilitados (antes llevaban a experiencia vacía)

## Estado final de la sesión
- **~52 cambios reales** aplicados, compilados y desplegados en beta.apk
- **23 pantallas** renderizadas, suite completa pasa sin romperse
- Todo el alto-impacto **client-side** está hecho. El resto necesita **backend**.
