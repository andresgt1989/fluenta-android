package com.alturya.fluenta.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.ErrorItem
import com.alturya.fluenta.network.Skill
import com.alturya.fluenta.network.UserProfile
import com.alturya.fluenta.network.UserProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val progress: UserProgress? = null,
    val skills: List<Skill> = emptyList(),
    val wpm: Int = 0,
    val taskSuccessRate: Int = 0,
    val errors: List<ErrorItem> = emptyList()
)

class ProgressViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProgressState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = ProgressState(loading = true)
            val profile = try { ApiClient.api.getProfile() } catch (_: Exception) { null }
            val progress = try { ApiClient.api.getProgress() } catch (_: Exception) { null }
            val skills = try { ApiClient.api.getSkills() } catch (_: Exception) { null }
            val errors = try { ApiClient.api.getErrors().errors } catch (_: Exception) { emptyList() }
            _state.value = ProgressState(
                loading = false,
                profile = profile,
                progress = progress,
                skills = skills?.skills ?: emptyList(),
                wpm = skills?.wpm ?: 0,
                taskSuccessRate = skills?.taskSuccessRate ?: 0,
                errors = errors
            )
        }
    }
}
