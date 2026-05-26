package com.alturya.fluenta.pronunciation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.audio.TtsPlayer
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.PhonemeDrill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PronState(
    val loading: Boolean = true,
    val drill: PhonemeDrill? = null,
    val source: String? = null,
    val playing: String? = null,
    val error: String? = null
)

class PronunciationViewModel : ViewModel() {

    private val _state = MutableStateFlow(PronState())
    val state = _state.asStateFlow()

    init { loadDrill(null) }

    fun loadDrill(phoneme: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val res = ApiClient.api.getDrill(phoneme)
                _state.value = PronState(loading = false, drill = res.drill, source = res.source)
            } catch (e: Exception) {
                _state.value = PronState(loading = false, error = "No se pudo cargar el ejercicio")
            }
        }
    }

    fun listen(context: Context, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(playing = text)
            val res = TtsPlayer.play(context, text)
            _state.value = if (res.isSuccess) _state.value.copy(playing = null)
            else _state.value.copy(playing = null, error = "No se pudo reproducir el audio")
        }
    }

    override fun onCleared() {
        TtsPlayer.stop()
    }
}
