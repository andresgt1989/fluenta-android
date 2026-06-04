package com.alturya.fluenta.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.NextLesson
import com.alturya.fluenta.network.UserProfile
import com.alturya.fluenta.network.UserProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val progress: UserProgress? = null,
    val nextLesson: NextLesson? = null,
    val coachMessage: String? = null,
    val affectiveState: String? = null,
    // Business WhatsApp URL from the server (includes the correct business phone number).
    val practiceWaUrl: String? = null,
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeState(loading = true)
            val profile = try { ApiClient.api.getProfile() } catch (_: Exception) { null }
            profile?.let {
                com.alturya.fluenta.data.Session.l1 = it.l1
                com.alturya.fluenta.data.Session.l2 = it.l2
            }
            val progress = try { ApiClient.api.getProgress() } catch (_: Exception) { null }
            val next = try { ApiClient.api.getNextLesson().next } catch (_: Exception) { null }
            val coach = try { ApiClient.api.getCoachMessage() } catch (_: Exception) { null }
            val waUrl = try { ApiClient.api.getWhatsAppHandoff(intent = "practice").url } catch (_: Exception) { null }
            _state.value = HomeState(
                loading = false,
                profile = profile,
                progress = progress,
                nextLesson = next,
                coachMessage = coach?.message,
                affectiveState = coach?.affectiveState,
                practiceWaUrl = waUrl,
            )
        }
    }
}
