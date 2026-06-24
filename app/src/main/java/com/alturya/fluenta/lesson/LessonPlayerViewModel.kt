package com.alturya.fluenta.lesson

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.ExerciseCheckBody
import com.alturya.fluenta.network.ExerciseCheckResponse
import com.alturya.fluenta.network.LessonPlayResponse
import com.alturya.fluenta.network.LessonSubmitBody
import com.alturya.fluenta.network.LessonSubmitResponse
import com.alturya.fluenta.network.PlayableExercise
import com.alturya.fluenta.network.SubmissionAnswerBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * unidad_completada (función pura, testeable en JVM): dada la respuesta del mapa de
 * currículo y la lección recién enviada, devuelve la unidad que contiene esa lección
 * SI y solo si, con esta lección, TODAS las lecciones de la unidad quedan completas;
 * `null` en caso contrario. Reglas: la lección debe pertenecer a alguna unidad; una
 * unidad sin lecciones nunca cuenta como completada.
 */
internal fun unitCompletedBy(
    map: com.alturya.fluenta.network.CurriculumMapResponse,
    lessonId: String,
): com.alturya.fluenta.network.CurriculumUnit? {
    val unit = map.map.firstOrNull { u -> u.lessons.any { it.id == lessonId } } ?: return null
    return unit.takeIf { it.lessons.isNotEmpty() && it.lessons.all { l -> l.completed } }
}

/**
 * Siguiente lección (función pura, testeable en JVM): aplana el currículo en su
 * orden real (unidad.number, luego lección.number) y devuelve la PRIMERA lección
 * aún no completada que viene DESPUÉS de la recién terminada. Es el destino de
 * "Siguiente lección →" en la pantalla de resultados: mantiene el momentum en vez
 * de soltar al usuario al menú. `null` si la actual no está en el mapa o si ya no
 * quedan lecciones pendientes por delante (terminó todo lo disponible).
 */
internal fun nextLessonAfter(
    map: com.alturya.fluenta.network.CurriculumMapResponse,
    currentLessonId: String,
): String? {
    val flat = map.map
        .sortedBy { it.number }
        .flatMap { u -> u.lessons.sortedBy { it.number } }
    val idx = flat.indexOfFirst { it.id == currentLessonId }
    if (idx < 0) return null
    return flat.drop(idx + 1).firstOrNull { !it.completed }?.id
}

/**
 * leccion_abandonada (predicado puro, testeable en JVM): dado el estado de la
 * lección y si ya se emitió un evento de salida, decide si el abandono debe
 * dispararse. Solo cuenta como abandono si la lección estaba EN CURSO (cargada,
 * con ejercicios, sin resultado) y aún no se emitió ningún evento de salida —
 * garantiza exclusión mutua con leccion_completada y un único disparo.
 */
internal fun shouldFireAbandon(state: LessonPlayerState, alreadyTracked: Boolean): Boolean =
    !alreadyTracked &&
        state.result == null &&
        !state.loading &&
        state.exercises.isNotEmpty()

data class LessonPlayerState(
    val loading: Boolean = true,
    val error: String? = null,
    val lessonId: String? = null,
    val title: String? = null,
    val introMessage: String? = null,
    val exercises: List<PlayableExercise> = emptyList(),
    val teach: List<com.alturya.fluenta.network.TeachItem> = emptyList(),
    val teachDone: Boolean = false,   // las tarjetas de vocabulario se vieron
    val currentIndex: Int = 0,
    val answers: Map<Int, String> = emptyMap(),
    val checking: Boolean = false,               // calling /check for instant feedback
    val feedback: ExerciseCheckResponse? = null, // shown after "Comprobar", before "Continuar"
    val hearts: Int = 5,                         // vidas: -1 por fallo; a 0 termina la lección
    val combo: Int = 0,                          // aciertos seguidos (gamificación): se reinicia al fallar
    val startedAtMs: Long = 0L,
    val submitting: Boolean = false,
    val result: LessonSubmitResponse? = null,
    val nextLessonId: String? = null,   // destino de "Siguiente lección →" tras completar (retención)
)

class LessonPlayerViewModel(savedState: SavedStateHandle, private val app: Application? = null) : ViewModel() {
    private val lessonId: String = checkNotNull(savedState.get<String>("lessonId")) {
        "lessonId arg required"
    }

    private val _state = MutableStateFlow(LessonPlayerState(lessonId = lessonId))
    val state: StateFlow<LessonPlayerState> = _state.asStateFlow()

    // Retención: cada lección emite EXACTAMENTE un evento de salida —
    // leccion_completada (submit OK) o leccion_abandonada (salió antes). Este flag
    // garantiza que no se dispare ninguno dos veces ni ambos para la misma lección.
    private var outcomeTracked = false

    init { load() }

    fun finishTeach() { _state.update { it.copy(teachDone = true) } }

    private fun applyLesson(res: LessonPlayResponse) {
        _state.update {
            it.copy(
                loading = false,
                title = res.lesson.title,
                introMessage = res.introMessage,
                exercises = res.exercises,
                teach = res.teach ?: emptyList(),
                teachDone = (res.teach ?: emptyList()).isEmpty(),
                currentIndex = 0,
                answers = emptyMap(),
                feedback = null,
                result = null,
                hearts = 5,
                startedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val res: LessonPlayResponse = ApiClient.api.getLessonPlay(lessonId)
                // Cache for offline use
                app?.let { LessonCache.save(it, lessonId, res) }
                applyLesson(res)
            } catch (e: Exception) {
                Log.e("LessonPlayer", "load failed", e)
                // Try offline cache before showing error
                val cached = app?.let { LessonCache.load(it, lessonId) }
                if (cached != null) {
                    applyLesson(cached)
                    // Subtle banner: lesson loaded from cache
                    _state.update { it.copy(introMessage = (it.introMessage ?: "") + " [offline]") }
                } else {
                    _state.update { it.copy(loading = false, error = I18nStore.t("lesson.loadError", "No se pudo cargar la lección. Reintenta.")) }
                }
            }
        }
    }

    fun recordAnswer(value: String) {
        val s = _state.value
        val idx = s.currentIndex
        _state.update { it.copy(answers = it.answers + (idx to value)) }
    }

    /** "Comprobar": record the answer and ask the server if it's right, showing
     *  instant green/red feedback. On network error, fail open and just advance. */
    fun checkAnswer(value: String) {
        val s = _state.value
        if (s.checking || s.feedback != null) return
        val idx = s.currentIndex
        _state.update { it.copy(answers = it.answers + (idx to value), checking = true) }
        viewModelScope.launch {
            try {
                val res = ApiClient.api.checkExercise(lessonId, ExerciseCheckBody(idx, value))
                _state.update {
                    val nh = if (!res.correct) (it.hearts - 1).coerceAtLeast(0) else it.hearts
                    val nc = if (res.correct) it.combo + 1 else 0
                    it.copy(checking = false, feedback = res, hearts = nh, combo = nc)
                }
            } catch (e: Exception) {
                Log.e("LessonPlayer", "check failed", e)
                _state.update { it.copy(checking = false) }
                next()
            }
        }
    }

    /** "Continuar" after feedback is shown. Out of hearts → end the lesson. */
    fun continueAfterFeedback() {
        val outOfHearts = _state.value.hearts <= 0
        _state.update { it.copy(feedback = null) }
        if (outOfHearts) submit() else next()
    }

    /** "Reintentar" tras un fallo: limpia el feedback y deja reintentar el MISMO
     *  ejercicio sin avanzar (el corazón ya se descontó, no se descuenta otra vez
     *  hasta que vuelva a Comprobar). Solo tiene sentido si aún quedan vidas. */
    fun retryAfterFeedback() {
        if (_state.value.hearts <= 0) { continueAfterFeedback(); return }
        _state.update { it.copy(feedback = null) }
    }

    fun next() {
        val s = _state.value
        if (s.currentIndex >= s.exercises.size - 1) {
            submit()
        } else {
            _state.update { it.copy(currentIndex = it.currentIndex + 1, feedback = null) }
        }
    }

    fun skip() {
        _state.update { it.copy(feedback = null) }
        recordAnswer("")
        next()
    }

    private fun submit() {
        val s = _state.value
        if (s.submitting) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                val answers = s.exercises.mapIndexed { idx, _ ->
                    SubmissionAnswerBody(exerciseIndex = idx, value = s.answers[idx] ?: "")
                }
                val timeSpent = ((System.currentTimeMillis() - s.startedAtMs) / 1000L).toInt()
                val res = ApiClient.api.submitLesson(
                    lessonId,
                    LessonSubmitBody(answers = answers, timeSpentSeconds = timeSpent),
                )
                _state.update { it.copy(submitting = false, result = res) }
                trackCompletion(res, timeSpent)
            } catch (e: Exception) {
                Log.e("LessonPlayer", "submit failed", e)
                _state.update { it.copy(submitting = false, error = I18nStore.t("lesson.submitError", "Error enviando respuestas. Reintenta.")) }
            }
        }
    }

    /** leccion_completada (+ unidad_completada si ésta era la última de su unidad).
     *  Solo se llama tras un submit con éxito; nunca tras un abandono. */
    private fun trackCompletion(res: LessonSubmitResponse, timeSpentSeconds: Int) {
        val ctx = app ?: return
        if (outcomeTracked) return
        outcomeTracked = true
        Analytics.track(ctx, Analytics.LESSON_COMPLETE, mapOf(
            "lessonId" to lessonId,
            "scorePct" to res.scorePct.toString(),
            "passed" to res.passed.toString(),
            "timeSpentSeconds" to timeSpentSeconds.toString(),
        ))
        // streak_day: disparar cuando el servidor confirma una racha activa
        val newStreak = res.newStreakDays ?: 0
        if (newStreak > 0) {
            Analytics.track(ctx, Analytics.STREAK_DAY, mapOf("days" to newStreak.toString()))
        }
        // unidad_completada: derivada del mapa de currículo (fuente de verdad del
        // backend). Si todas las lecciones de la unidad que contiene a ésta quedan
        // `completed`, la unidad se completó con esta lección. Fire-and-forget: si
        // la red falla, perdemos el evento de unidad pero nunca el de lección.
        if (!res.passed) return
        viewModelScope.launch {
            try {
                // Una sola lectura del mapa sirve para dos cosas: el evento de
                // unidad completada y el destino de "Siguiente lección →".
                val map = ApiClient.api.getCurriculumMap()
                _state.update { it.copy(nextLessonId = nextLessonAfter(map, lessonId)) }
                val unit = unitCompletedBy(map, lessonId) ?: return@launch
                Analytics.track(ctx, Analytics.UNIT_COMPLETE, mapOf(
                    "unitId" to unit.id,
                    "unitNumber" to unit.number.toString(),
                    "lessonId" to lessonId,
                ))
            } catch (e: Exception) {
                Log.e("LessonPlayer", "unit-complete check failed", e)
            }
        }
    }

    /** leccion_abandonada: el usuario salió de la lección SIN terminarla.
     *  Idempotente y mutuamente excluyente con leccion_completada. Solo cuenta si la
     *  lección ya estaba en curso (se cargó), para no marcar como abandono una
     *  pantalla que nunca llegó a mostrar ejercicios. */
    fun abandon() {
        val ctx = app ?: return
        val s = _state.value
        if (!shouldFireAbandon(s, outcomeTracked)) return
        outcomeTracked = true
        Analytics.track(ctx, Analytics.LESSON_ABANDON, mapOf(
            "lessonId" to lessonId,
            "exerciseIndex" to s.currentIndex.toString(),
            "total" to s.exercises.size.toString(),
            "teachDone" to s.teachDone.toString(),
        ))
    }

    fun retry() = load()
}
