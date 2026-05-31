package com.alturya.fluenta.lesson

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class LessonPlayerState(
    val loading: Boolean = true,
    val error: String? = null,
    val lessonId: String? = null,
    val title: String? = null,
    val introMessage: String? = null,
    val exercises: List<PlayableExercise> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<Int, String> = emptyMap(),
    val checking: Boolean = false,               // calling /check for instant feedback
    val feedback: ExerciseCheckResponse? = null, // shown after "Comprobar", before "Continuar"
    val hearts: Int = 3,                         // vidas: -1 por fallo; a 0 termina la lección
    val startedAtMs: Long = 0L,
    val submitting: Boolean = false,
    val result: LessonSubmitResponse? = null,
)

class LessonPlayerViewModel(savedState: SavedStateHandle, private val app: Application? = null) : ViewModel() {
    private val lessonId: String = checkNotNull(savedState.get<String>("lessonId")) {
        "lessonId arg required"
    }

    private val _state = MutableStateFlow(LessonPlayerState(lessonId = lessonId))
    val state: StateFlow<LessonPlayerState> = _state.asStateFlow()

    init { load() }

    private fun applyLesson(res: LessonPlayResponse) {
        _state.update {
            it.copy(
                loading = false,
                title = res.lesson.title,
                introMessage = res.introMessage,
                exercises = res.exercises,
                currentIndex = 0,
                answers = emptyMap(),
                feedback = null,
                result = null,
                hearts = 3,
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
                    it.copy(checking = false, feedback = res, hearts = nh)
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
            } catch (e: Exception) {
                Log.e("LessonPlayer", "submit failed", e)
                _state.update { it.copy(submitting = false, error = I18nStore.t("lesson.submitError", "Error enviando respuestas. Reintenta.")) }
            }
        }
    }

    fun retry() = load()
}
