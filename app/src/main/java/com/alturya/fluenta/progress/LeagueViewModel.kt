package com.alturya.fluenta.progress

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.LeagueResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeagueState(
    val loading: Boolean = true,
    val league: LeagueResponse? = null,
)

class LeagueViewModel : ViewModel() {
    private val _state = MutableStateFlow(LeagueState())
    val state: StateFlow<LeagueState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getMyLeague()
                _state.update { it.copy(loading = false, league = res) }
            } catch (e: Exception) {
                Log.e("League", "load failed", e)
                _state.update { it.copy(loading = false, league = null) }
            }
        }
    }
}
