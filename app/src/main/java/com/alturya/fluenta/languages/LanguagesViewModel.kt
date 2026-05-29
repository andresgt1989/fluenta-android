package com.alturya.fluenta.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.LanguagePair
import com.alturya.fluenta.network.SelectLanguageBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LanguagesState(
    val loading: Boolean = true,
    val pairs: List<LanguagePair> = emptyList(),
    val selecting: String? = null,
    val message: String? = null
)

class LanguagesViewModel : ViewModel() {

    private val _state = MutableStateFlow(LanguagesState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = LanguagesState(loading = true)
            try {
                val res = ApiClient.api.getLanguages()
                _state.value = LanguagesState(loading = false, pairs = res.pairs)
            } catch (e: Exception) {
                _state.value = LanguagesState(loading = false, message = I18nStore.t("languages.loadError", "No se pudo cargar el catálogo"))
            }
        }
    }

    fun select(l2: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(selecting = l2)
            try {
                ApiClient.api.selectLanguage(SelectLanguageBody(l2))
                _state.value = _state.value.copy(selecting = null, message = I18nStore.t("languages.changed", "Idioma cambiado a {lang}").replace("{lang}", l2.uppercase()))
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(selecting = null, message = I18nStore.t("languages.changeError", "No se pudo cambiar el idioma"))
            }
        }
    }
}
