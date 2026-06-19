package com.alturya.fluenta.exercises

import com.alturya.fluenta.data.I18nStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.MatchPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MatchState(
    val loading: Boolean = true,
    val empty: Boolean = false,
    val error: String? = null,
    val leftItems: List<String> = emptyList(),   // L2 (target language)
    val rightItems: List<String> = emptyList(),   // L1 (native)
    val answerKey: Map<String, String> = emptyMap(), // l2 -> l1
    val selectedLeft: String? = null,
    val matched: Set<String> = emptySet(),         // matched l2 keys
    val wrongFlash: Pair<String, String>? = null,  // (l2, l1) last wrong attempt
    val attempts: Int = 0,
    val won: Boolean = false
)

class MatchViewModel : ViewModel() {

    private val _state = MutableStateFlow(MatchState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = MatchState(loading = true)
            try {
                val res = ApiClient.api.getMatchExercise()
                val pairs = res.pairs
                if (pairs.size < 2) {
                    _state.value = MatchState(loading = false, empty = true)
                    return@launch
                }
                _state.value = MatchState(
                    loading = false,
                    leftItems = pairs.map { it.l2 }.shuffled(),
                    rightItems = pairs.map { it.l1 }.shuffled(),
                    answerKey = pairs.associate { it.l2 to it.l1 }
                )
            } catch (e: Exception) {
                _state.value = MatchState(loading = false, error = I18nStore.t("match.error.load", "No se pudo cargar el ejercicio"))
            }
        }
    }

    fun tapLeft(l2: String) {
        val s = _state.value
        if (s.matched.contains(l2)) return
        _state.value = s.copy(selectedLeft = if (s.selectedLeft == l2) null else l2, wrongFlash = null)
    }

    fun tapRight(l1: String) {
        val s = _state.value
        val left = s.selectedLeft ?: return
        if (s.matched.any { s.answerKey[it] == l1 }) return // already matched

        val correct = s.answerKey[left] == l1
        if (correct) {
            val matched = s.matched + left
            _state.value = s.copy(
                matched = matched,
                selectedLeft = null,
                wrongFlash = null,
                attempts = s.attempts + 1,
                won = matched.size == s.answerKey.size
            )
        } else {
            _state.value = s.copy(
                wrongFlash = left to l1,
                selectedLeft = null,
                attempts = s.attempts + 1
            )
        }
    }
}
