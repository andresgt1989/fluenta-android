package com.alturya.fluenta.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.TokenStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.FcmRegisterBody
import com.alturya.fluenta.network.OtpRequestBody
import com.alturya.fluenta.network.OtpVerifyBody
import com.alturya.fluenta.network.SelectLanguageBody
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object OtpSent : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state = _state.asStateFlow()

    fun requestOtp(phone: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                ApiClient.api.requestOtp(OtpRequestBody(phone))
                _state.value = LoginState.OtpSent
            } catch (e: Exception) {
                _state.value = LoginState.Error(I18nStore.t("login.sendError", "No se pudo enviar el código"))
            }
        }
    }

    fun verifyOtp(context: Context, phone: String, code: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val res = ApiClient.api.verifyOtp(OtpVerifyBody(phone, code))
                if (res.token != null) {
                    TokenStore.save(context, res.token, phone)
                    ApiClient.setToken(res.token)
                    // Apply the language the user picked during play-first onboarding,
                    // so their fresh account starts on the right pair.
                    val chosenL2 = TokenStore.getChosenL2(context).firstOrNull()
                    if (!chosenL2.isNullOrBlank()) {
                        try { ApiClient.api.selectLanguage(SelectLanguageBody(chosenL2)) }
                        catch (_: Exception) { /* non-critical: user can change in-app */ }
                    }
                    // Register any FCM token that was generated before login.
                    val fcmToken = TokenStore.getFcmToken(context).firstOrNull()
                    if (fcmToken != null) {
                        try {
                            ApiClient.api.registerFcmToken(FcmRegisterBody(fcmToken))
                        } catch (_: Exception) { /* non-critical */ }
                    }
                    _state.value = LoginState.Success
                } else {
                    _state.value = LoginState.Error(I18nStore.t("login.codeError", "Código incorrecto"))
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(I18nStore.t("login.codeError", "Código incorrecto"))
            }
        }
    }
}