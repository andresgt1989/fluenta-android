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
    val coachMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeState(loading = true)
            val profile = try { ApiClient.api.getProfile() } catch (_: Exception) { null }
            val progress = try { ApiClient.api.getProgress() } catch (_: Exception) { null }
            val next = try { ApiClient.api.getNextLesson().next } catch (_: Exception) { null }
            val coach = try { ApiClient.api.getCoachMessage().message } catch (_: Exception) { null }
            _state.value = HomeState(loading = false, profile = profile, progress = progress, nextLesson = next, coachMessage = coach)
        }
    }
}
