package com.alturya.fluenta.curriculum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.CurriculumUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CurriculumState(
    val loading: Boolean = true,
    val units: List<CurriculumUnit> = emptyList(),
    val l1: String? = null,
    val l2: String? = null,
    val error: String? = null
)

class CurriculumViewModel : ViewModel() {

    private val _state = MutableStateFlow(CurriculumState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CurriculumState(loading = true)
            try {
                val res = ApiClient.api.getCurriculumMap()
                _state.value = CurriculumState(
                    loading = false,
                    units = res.map,
                    l1 = res.l1,
                    l2 = res.l2
                )
            } catch (e: Exception) {
                _state.value = CurriculumState(loading = false, error = "No se pudo cargar el mapa")
            }
        }
    }
}
