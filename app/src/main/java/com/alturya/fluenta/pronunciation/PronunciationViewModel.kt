package com.alturya.fluenta.pronunciation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.audio.TtsPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PronState(
    val playing: String? = null,
    val error: String? = null
)

class PronunciationViewModel : ViewModel() {

    private val _state = MutableStateFlow(PronState())
    val state = _state.asStateFlow()

    fun listen(context: Context, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = PronState(playing = text)
            val res = TtsPlayer.play(context, text)
            _state.value = if (res.isSuccess) PronState(playing = null)
            else PronState(playing = null, error = "No se pudo reproducir el audio")
        }
    }

    override fun onCleared() {
        TtsPlayer.stop()
    }
}
