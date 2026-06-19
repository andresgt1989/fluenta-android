package com.alturya.fluenta.verbs

import com.alturya.fluenta.data.I18nStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.DailyVerbCard
import com.alturya.fluenta.network.VerbAnswerBody
import com.alturya.fluenta.network.VerbStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerbsTodayState(
    val loading: Boolean = true,
    val error: String? = null,
    val verbs: List<DailyVerbCard> = emptyList(),
    val stats: VerbStats = VerbStats(0, 0, 0),
    val target: Int = 10,
    val achievedToday: Int = 0,
    /** Per-verb local mastery state for visual feedback before server roundtrip. */
    val markedMastered: Set<String> = emptySet(),
)

class VerbsTodayViewModel : ViewModel() {
    private val _state = MutableStateFlow(VerbsTodayState())
    val state: StateFlow<VerbsTodayState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getVerbsToday()
                _state.update {
                    it.copy(
                        loading = false,
                        verbs = res.verbs,
                        stats = res.stats,
                        target = res.target,
                        achievedToday = res.achievedToday,
                    )
                }
            } catch (e: Exception) {
                Log.e("VerbsToday", "load failed", e)
                _state.update { it.copy(loading = false, error = I18nStore.t("verbs.error.load", "No se pudieron cargar los verbos.")) }
            }
        }
    }

    /** Marks a verb as practiced + correct; server records exposure and may flip mastery. */
    fun markPracticed(verbId: String) {
        _state.update { it.copy(markedMastered = it.markedMastered + verbId) }
        viewModelScope.launch {
            try {
                ApiClient.api.answerVerb(verbId, VerbAnswerBody(correct = true))
            } catch (e: Exception) {
                Log.e("VerbsToday", "answer failed", e)
            }
        }
    }
}
