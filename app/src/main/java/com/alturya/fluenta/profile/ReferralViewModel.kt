package com.alturya.fluenta.profile

import com.alturya.fluenta.data.I18nStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.ReferralClaimBody
import com.alturya.fluenta.network.ReferralResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReferralState(
    val loading: Boolean = true,
    val referral: ReferralResponse? = null,
    val claimInput: String = "",
    val claiming: Boolean = false,
    val claimMessage: String? = null,
    val claimSuccess: Boolean? = null,
)

class ReferralViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReferralState())
    val state: StateFlow<ReferralState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getReferral()
                _state.update { it.copy(loading = false, referral = res) }
            } catch (e: Exception) {
                Log.e("Referral", "load failed", e)
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun setClaimInput(v: String) { _state.update { it.copy(claimInput = v.uppercase().take(8), claimMessage = null) } }

    fun claim() {
        val code = _state.value.claimInput.trim()
        if (code.length < 4) return
        _state.update { it.copy(claiming = true, claimMessage = null) }
        viewModelScope.launch {
            try {
                val res = ApiClient.api.claimReferral(ReferralClaimBody(code))
                _state.update {
                    it.copy(
                        claiming = false,
                        claimSuccess = res.ok,
                        claimMessage = res.message ?: res.reason ?: if (res.ok) I18nStore.t("referral.applied", "Código aplicado") else I18nStore.t("referral.invalid", "Código no válido"),
                    )
                }
                if (res.ok) load()
            } catch (e: Exception) {
                _state.update { it.copy(claiming = false, claimMessage = I18nStore.t("referral.error.network", "Error de red. Intenta de nuevo.")) }
            }
        }
    }
}
