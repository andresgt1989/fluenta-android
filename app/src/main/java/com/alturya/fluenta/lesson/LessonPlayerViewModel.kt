package com.alturya.fluenta.lesson

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.network.ApiClient
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
    val lastSubmittedResult: Boolean? = null,   // visual feedback for current exercise
    val startedAtMs: Long = 0L,
    val submitting: Boolean = false,
    val result: LessonSubmitResponse? = null,
)

class LessonPlayerViewModel(savedState: SavedStateHandle) : ViewModel() {
    private val lessonId: String = checkNotNull(savedState.get<String>("lessonId")) {
        "lessonId arg required"
    }

    private val _state = MutableStateFlow(LessonPlayerState(lessonId = lessonId))
    val state: StateFlow<LessonPlayerState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val res: LessonPlayResponse = ApiClient.api.getLessonPlay(lessonId)
                _state.update {
                    it.copy(
                        loading = false,
                        title = res.lesson.title,
                        introMessage = res.introMessage,
                        exercises = res.exercises,
                        currentIndex = 0,
                        startedAtMs = System.currentTimeMillis(),
                    )
                }
            } catch (e: Exception) {
                Log.e("LessonPlayer", "load failed", e)
                _state.update { it.copy(loading = false, error = "No se pudo cargar la lección. Reintenta.") }
            }
        }
    }

    fun recordAnswer(value: String) {
        val s = _state.value
        val idx = s.currentIndex
        _state.update { it.copy(answers = it.answers + (idx to value)) }
    }

    fun next() {
        val s = _state.value
        if (s.currentIndex >= s.exercises.size - 1) {
            submit()
        } else {
            _state.update { it.copy(currentIndex = it.currentIndex + 1, lastSubmittedResult = null) }
        }
    }

    fun skip() {
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
                _state.update { it.copy(submitting = false, error = "Error enviando respuestas. Reintenta.") }
            }
        }
    }

    fun retry() = load()
}
