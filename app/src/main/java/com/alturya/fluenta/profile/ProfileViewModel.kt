package com.alturya.fluenta.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.data.TokenStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.Badge
import com.alturya.fluenta.network.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val badges: List<Badge> = emptyList(),
    val badgesEarned: Int = 0,
    val badgesTotal: Int = 0,
    val error: Boolean = false,
)

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = ProfileState(loading = true)
            val profile = try { ApiClient.api.getProfile() } catch (_: Exception) { null }
            val badgesRes = try { ApiClient.api.getBadges() } catch (_: Exception) { null }
            _state.value = ProfileState(
                loading = false,
                profile = profile,
                // Si ni siquiera el perfil cargó, es un error real (no solo datos vacíos).
                error = profile == null,
                badges = badgesRes?.badges ?: emptyList(),
                badgesEarned = badgesRes?.earned ?: 0,
                badgesTotal = badgesRes?.total ?: 0,
            )
        }
    }

    fun openPortal(onUrl: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getPortalUrl()
                res.url?.let(onUrl)
            } catch (_: Exception) {}
        }
    }

    fun openCheckout(plan: String, onUrl: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getCheckoutUrl(plan)
                res.url?.let(onUrl)
            } catch (_: Exception) {}
        }
    }

    fun logout(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            TokenStore.clear(context)
            ApiClient.setToken(null)
            onDone()
        }
    }
}
