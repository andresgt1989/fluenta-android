package com.alturya.fluenta.diagnostic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.DiagnosticAnswerBody
import com.alturya.fluenta.network.DiagnosticProgress
import com.alturya.fluenta.network.DiagnosticQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DiagnosticUiState {
    object Intro : DiagnosticUiState()
    object Loading : DiagnosticUiState()
    data class Quiz(
        val question: DiagnosticQuestion,
        val progress: DiagnosticProgress,
        val sessionId: String,
        val answering: Boolean = false,
        val lastAnsweredIndex: Int? = null,
        val lastCorrect: Boolean? = null
    ) : DiagnosticUiState()
    data class Result(
        val level: String,
        val confidence: Double,
        val correctCount: Int,
        val total: Int
    ) : DiagnosticUiState()
    data class Error(val message: String) : DiagnosticUiState()
}

class DiagnosticViewModel : ViewModel() {

    private val _state = MutableStateFlow<DiagnosticUiState>(DiagnosticUiState.Intro)
    val state = _state.asStateFlow()

    fun start() {
        viewModelScope.launch {
            _state.value = DiagnosticUiState.Loading
            try {
                val res = ApiClient.api.diagnosticStart()
                _state.value = DiagnosticUiState.Quiz(
                    question = res.question,
                    progress = res.progress,
                    sessionId = res.sessionId
                )
            } catch (e: Exception) {
                _state.value = DiagnosticUiState.Error(I18nStore.t("diagnostic.startError", "No se pudo iniciar el test"))
            }
        }
    }

    fun answer(index: Int) {
        val current = _state.value as? DiagnosticUiState.Quiz ?: return
        if (current.answering) return

        _state.value = current.copy(answering = true, lastAnsweredIndex = index)
        viewModelScope.launch {
            try {
                val res = ApiClient.api.diagnosticAnswer(
                    DiagnosticAnswerBody(current.sessionId, index)
                )
                if (res.done) {
                    _state.value = DiagnosticUiState.Result(
                        level = res.level ?: "a1",
                        confidence = res.confidence ?: 0.0,
                        correctCount = res.correctCount ?: 0,
                        total = res.total ?: 6
                    )
                } else if (res.question != null && res.progress != null) {
                    // Briefly show feedback, then advance
                    _state.value = current.copy(
                        answering = false,
                        lastAnsweredIndex = index,
                        lastCorrect = res.lastCorrect
                    )
                    kotlinx.coroutines.delay(800)
                    _state.value = DiagnosticUiState.Quiz(
                        question = res.question,
                        progress = res.progress,
                        sessionId = current.sessionId
                    )
                }
            } catch (e: Exception) {
                _state.value = DiagnosticUiState.Error(I18nStore.t("common.error", "Algo falló — intenta de nuevo"))
            }
        }
    }
}
