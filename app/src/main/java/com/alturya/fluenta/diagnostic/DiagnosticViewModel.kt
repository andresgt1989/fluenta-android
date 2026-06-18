package com.alturya.fluenta.diagnostic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.DiagnosticAnswerBody
import com.alturya.fluenta.network.DiagnosticAnswerResponse
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
        val lastCorrect: Boolean? = null,
        val wasSkipped: Boolean = false
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
        _state.value = current.copy(answering = true, lastAnsweredIndex = index, wasSkipped = false)
        viewModelScope.launch {
            try {
                val res = ApiClient.api.diagnosticAnswer(DiagnosticAnswerBody(current.sessionId, index))
                handleResponse(current, res, wasSkipped = false)
            } catch (e: Exception) {
                _state.value = DiagnosticUiState.Error(I18nStore.t("common.error", "Algo falló — intenta de nuevo"))
            }
        }
    }

    // User explicitly said "I don't know". We try sending -1 as a skip signal first
    // (backends that support it treat it as wrong with higher certainty than a guess).
    // If the backend rejects -1, we fall back to index 0. Either way it is marked as
    // wasSkipped = true so the UI shows "Saltada" instead of correct/wrong colours.
    fun skip() {
        val current = _state.value as? DiagnosticUiState.Quiz ?: return
        if (current.answering) return
        _state.value = current.copy(answering = true, wasSkipped = true, lastAnsweredIndex = null)
        viewModelScope.launch {
            for (fallbackIndex in listOf(-1, 0)) {
                try {
                    val res = ApiClient.api.diagnosticAnswer(DiagnosticAnswerBody(current.sessionId, fallbackIndex))
                    handleResponse(current, res, wasSkipped = true)
                    return@launch
                } catch (_: Exception) { /* try next fallback */ }
            }
            _state.value = DiagnosticUiState.Error(I18nStore.t("common.error", "Algo falló — intenta de nuevo"))
        }
    }

    private suspend fun handleResponse(
        current: DiagnosticUiState.Quiz,
        res: DiagnosticAnswerResponse,
        wasSkipped: Boolean
    ) {
        if (res.done) {
            _state.value = DiagnosticUiState.Result(
                level = res.level ?: "a1",
                confidence = res.confidence ?: 0.0,
                correctCount = res.correctCount ?: 0,
                total = res.total ?: 6
            )
        } else if (res.question != null && res.progress != null) {
            _state.value = current.copy(
                answering = false,
                lastAnsweredIndex = if (wasSkipped) null else current.lastAnsweredIndex,
                lastCorrect = if (wasSkipped) false else res.lastCorrect,
                wasSkipped = wasSkipped
            )
            kotlinx.coroutines.delay(800)
            _state.value = DiagnosticUiState.Quiz(
                question = res.question,
                progress = res.progress,
                sessionId = current.sessionId
            )
        }
    }
}
