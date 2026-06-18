package com.alturya.fluenta.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.TokenStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.DeviceAuthBody
import com.alturya.fluenta.network.EmailRequestBody
import com.alturya.fluenta.network.EmailVerifyBody
import com.alturya.fluenta.network.FcmRegisterBody
import com.alturya.fluenta.network.GoogleAuthBody
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
    data class Success(val isNewUser: Boolean) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state = _state.asStateFlow()

    fun reset() { _state.value = LoginState.Idle }

    fun requestOtp(phone: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val res = ApiClient.api.requestOtp(OtpRequestBody(phone))
                // The server can now tell us delivery actually failed (e.g. no open
                // 24h window for a brand-new user). Don't fake "code sent" — guide
                // the user to open the bot first so the next attempt can deliver.
                if (res.delivered == false) {
                    _state.value = LoginState.Error(I18nStore.t(
                        "login.otpNotDelivered",
                        "No pudimos enviarte el código por WhatsApp. Toca \"Abrir WhatsApp\", escríbele al bot y vuelve a tocar \"Enviar\"."
                    ))
                } else {
                    _state.value = LoginState.OtpSent
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(I18nStore.t("login.sendError", "No se pudo enviar el código"))
            }
        }
    }

    fun verifyOtp(context: Context, phone: String, code: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val deviceL1 = java.util.Locale.getDefault().language.takeIf { it.length == 2 } ?: "es"
                val chosenL2 = TokenStore.getChosenL2(context).firstOrNull()
                val res = ApiClient.api.verifyOtp(OtpVerifyBody(phone, code, deviceL1, chosenL2))
                if (res.token != null) {
                    TokenStore.save(context, res.token, phone)
                    ApiClient.setToken(res.token)
                    // Apply the language the user picked during play-first onboarding,
                    // so their fresh account starts on the right pair.
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
                    Analytics.track(context, Analytics.REGISTER_SUCCESS, mapOf("method" to "phone"))
                    _state.value = LoginState.Success(res.isNewUser == true)
                } else {
                    _state.value = LoginState.Error(I18nStore.t("login.codeError", "Código incorrecto"))
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(I18nStore.t("login.codeError", "Código incorrecto"))
            }
        }
    }

    // ── Email login (canal que NO depende de Meta) ──────────────────────────
    fun requestEmailOtp(email: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val res = ApiClient.api.requestEmailOtp(EmailRequestBody(email.trim()))
                if (res.delivered == false) {
                    _state.value = LoginState.Error(I18nStore.t(
                        "login.emailNotDelivered",
                        "No pudimos enviar el código a tu email. Revisa la dirección e intenta de nuevo."
                    ))
                } else {
                    _state.value = LoginState.OtpSent
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(I18nStore.t("login.sendError", "No se pudo enviar el código"))
            }
        }
    }

    fun verifyEmailOtp(context: Context, email: String, code: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val deviceL1 = java.util.Locale.getDefault().language.takeIf { it.length == 2 } ?: "es"
                val chosenL2 = TokenStore.getChosenL2(context).firstOrNull()
                val res = ApiClient.api.verifyEmailOtp(EmailVerifyBody(email.trim(), code.trim(), deviceL1, chosenL2))
                if (res.token != null) {
                    TokenStore.save(context, res.token, email.trim())
                    ApiClient.setToken(res.token)
                    if (!chosenL2.isNullOrBlank()) {
                        try { ApiClient.api.selectLanguage(SelectLanguageBody(chosenL2)) }
                        catch (_: Exception) { /* non-critical */ }
                    }
                    val fcmToken = TokenStore.getFcmToken(context).firstOrNull()
                    if (fcmToken != null) {
                        try { ApiClient.api.registerFcmToken(FcmRegisterBody(fcmToken)) }
                        catch (_: Exception) { /* non-critical */ }
                    }
                    Analytics.track(context, Analytics.REGISTER_SUCCESS, mapOf("method" to "email"))
                    _state.value = LoginState.Success(res.isNewUser == true)
                } else {
                    _state.value = LoginState.Error(I18nStore.t("login.codeError", "Código incorrecto"))
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(I18nStore.t("login.codeError", "Código incorrecto"))
            }
        }
    }

    // ── Cuenta instantánea por dispositivo (sin email/Google/Meta) ──────────
    // Entra de inmediato y guarda progreso. Cero fricción = máxima activación.
    fun signInWithDevice(context: Context) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val deviceId = TokenStore.getOrCreateAnonId(context)
                val deviceL1 = java.util.Locale.getDefault().language.takeIf { it.length == 2 } ?: "es"
                val chosenL2 = TokenStore.getChosenL2(context).firstOrNull()
                val res = ApiClient.apiNoAuth.authDevice(DeviceAuthBody(deviceId, deviceL1, chosenL2))
                if (res.token != null) {
                    TokenStore.save(context, res.token, "device")
                    ApiClient.setToken(res.token)
                    if (!chosenL2.isNullOrBlank()) {
                        try { ApiClient.api.selectLanguage(SelectLanguageBody(chosenL2)) }
                        catch (_: Exception) { /* non-critical */ }
                    }
                    val fcmToken = TokenStore.getFcmToken(context).firstOrNull()
                    if (fcmToken != null) {
                        try { ApiClient.api.registerFcmToken(FcmRegisterBody(fcmToken)) }
                        catch (_: Exception) { /* non-critical */ }
                    }
                    Analytics.track(context, Analytics.REGISTER_SUCCESS, mapOf("method" to "device"))
                    _state.value = LoginState.Success(res.isNewUser == true)
                } else {
                    _state.value = LoginState.Error(I18nStore.t("login.startError", "No se pudo entrar. Revisa tu conexión."))
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(I18nStore.t("login.startError", "No se pudo entrar. Revisa tu conexión."))
            }
        }
    }

    // ── Google Sign-In (un toque, sin Meta, sin OTP) ────────────────────────
    // El idToken lo obtiene la pantalla vía Credential Manager; aquí lo
    // canjeamos por nuestro token de sesión.
    fun signInWithGoogle(context: Context, idToken: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val deviceL1 = java.util.Locale.getDefault().language.takeIf { it.length == 2 } ?: "es"
                val chosenL2 = TokenStore.getChosenL2(context).firstOrNull()
                val res = ApiClient.apiNoAuth.authGoogle(GoogleAuthBody(idToken, deviceL1, chosenL2))
                if (res.token != null) {
                    TokenStore.save(context, res.token, "google")
                    ApiClient.setToken(res.token)
                    if (!chosenL2.isNullOrBlank()) {
                        try { ApiClient.api.selectLanguage(SelectLanguageBody(chosenL2)) }
                        catch (_: Exception) { /* non-critical */ }
                    }
                    val fcmToken = TokenStore.getFcmToken(context).firstOrNull()
                    if (fcmToken != null) {
                        try { ApiClient.api.registerFcmToken(FcmRegisterBody(fcmToken)) }
                        catch (_: Exception) { /* non-critical */ }
                    }
                    Analytics.track(context, Analytics.REGISTER_SUCCESS, mapOf("method" to "google"))
                    _state.value = LoginState.Success(res.isNewUser == true)
                } else {
                    _state.value = LoginState.Error(I18nStore.t("login.googleError", "No se pudo iniciar sesión con Google"))
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(I18nStore.t("login.googleError", "No se pudo iniciar sesión con Google"))
            }
        }
    }
}