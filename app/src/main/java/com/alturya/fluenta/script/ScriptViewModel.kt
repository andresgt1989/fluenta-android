package com.alturya.fluenta.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.ScriptInfo
import com.alturya.fluenta.network.ScriptLesson
import com.alturya.fluenta.network.ScriptLessonBody
import com.alturya.fluenta.network.ScriptQuiz
import com.alturya.fluenta.network.ScriptQuizGradeBody
import com.alturya.fluenta.network.ScriptQuizResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScriptPhase { LOADING, LEARN, QUIZ, RESULT, ERROR }

data class ScriptUiState(
    val phase: ScriptPhase = ScriptPhase.LOADING,
    val l2: String = "",
    val info: ScriptInfo? = null,
    val lesson: ScriptLesson? = null,
    val quiz: ScriptQuiz? = null,
    // índice de pregunta del quiz → opción elegida
    val answers: Map<Int, String> = emptyMap(),
    val result: ScriptQuizResult? = null,
    val error: String = "",
)

// Aprende a LEER y ESCRIBIR el script (no-latinos) con el método nativo, antes
// de avanzar. La escritura trazo-a-trazo se hace en la pantalla (Hanzi Writer).
class ScriptViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScriptUiState())
    val state = _state.asStateFlow()

    private var scriptKey: String = ""

    fun load(l2: String) {
        _state.value = ScriptUiState(phase = ScriptPhase.LOADING, l2 = l2)
        viewModelScope.launch {
            try {
                val info = ApiClient.api.getScriptInfo(l2)
                // Unidad base = la primera (la que se aprende primero).
                scriptKey = info.scripts.firstOrNull()?.key ?: ""
                val lesson = ApiClient.api.getScriptLesson(ScriptLessonBody(l2 = l2, scriptKey = scriptKey))
                _state.value = _state.value.copy(phase = ScriptPhase.LEARN, info = info, lesson = lesson)
            } catch (e: Exception) {
                _state.value = _state.value.copy(phase = ScriptPhase.ERROR, error = I18nStore.t("script.error.alphabet", "No se pudo cargar el alfabeto. Revisa tu conexión."))
            }
        }
    }

    fun startQuiz() {
        val l2 = _state.value.l2
        if (l2.isBlank() || scriptKey.isBlank()) return
        // Only test glyphs the user actually saw in this lesson — never surprise them with unknowns
        val taughtGlyphs = _state.value.lesson?.items?.map { it.glyph }?.toSet() ?: emptySet()
        _state.value = _state.value.copy(phase = ScriptPhase.LOADING)
        viewModelScope.launch {
            try {
                val rawQuiz = ApiClient.api.getScriptQuiz(l2, scriptKey)
                val filteredItems = if (taughtGlyphs.isEmpty()) rawQuiz.items
                                    else rawQuiz.items.filter { it.glyph in taughtGlyphs }
                val quiz = rawQuiz.copy(items = filteredItems)
                _state.value = _state.value.copy(phase = ScriptPhase.QUIZ, quiz = quiz, answers = emptyMap(), result = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(phase = ScriptPhase.ERROR, error = I18nStore.t("script.error.quiz", "No se pudo cargar el examen de lectura."))
            }
        }
    }

    fun choose(index: Int, option: String) {
        _state.value = _state.value.copy(answers = _state.value.answers + (index to option))
    }

    fun submitQuiz() {
        val quiz = _state.value.quiz ?: return
        val answers = quiz.items.indices.map { _state.value.answers[it] ?: "" }
        _state.value = _state.value.copy(phase = ScriptPhase.LOADING)
        viewModelScope.launch {
            try {
                val result = ApiClient.api.gradeScriptQuiz(ScriptQuizGradeBody(quiz.quizId, answers))
                _state.value = _state.value.copy(phase = ScriptPhase.RESULT, result = result)
            } catch (e: Exception) {
                _state.value = _state.value.copy(phase = ScriptPhase.ERROR, error = I18nStore.t("script.error.grade", "No se pudo calificar el examen."))
            }
        }
    }

    fun backToLearn() {
        _state.value = _state.value.copy(phase = ScriptPhase.LEARN)
    }
}
