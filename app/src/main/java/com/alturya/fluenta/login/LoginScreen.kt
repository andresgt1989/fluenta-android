package com.alturya.fluenta.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.ui.HootMascot
import kotlinx.coroutines.launch

// Tokens exactos del handoff de Claude Design (Fluenta Login.dc.html).
private object Lc {
    val Teal = Color(0xFF0E9D8E)
    val TealDark = Color(0xFF0A6F64)
    val Ink = Color(0xFF15201D)
    val Sub = Color(0xFF5C6562)
    val Surface = Color(0xFFFBFCFB)
    val Border = Color(0xFFDCE5E2)
    val Legal = Color(0xFF9AA39E)
    val ErrBg = Color(0xFFFFE1DC)
    val ErrInk = Color(0xFF8A2C22)
    val ErrIcon = Color(0xFFE8554B)
    val GoogleBlue = Color(0xFF4285F4)
}

@Composable
fun LoginScreen(onSuccess: (Boolean) -> Unit, onTryGuest: (() -> Unit)? = null) {
    val context = LocalContext.current
    val vm: LoginViewModel = viewModel()
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var showEmail by remember { mutableStateOf(false) } // el botón email revela el flujo de email
    var googleClientId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        runCatching { ApiClient.apiNoAuth.getAuthConfig() }.getOrNull()?.let { cfg ->
            cfg.googleClientId?.let { googleClientId = it }
        }
    }
    LaunchedEffect(state) {
        if (state is LoginState.Success) onSuccess((state as LoginState.Success).isNewUser)
    }

    fun launchGoogleSignIn() {
        if (googleClientId.isBlank()) return
        Analytics.track(context, Analytics.REGISTER_START, mapOf("method" to "google"))
        scope.launch {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setServerClientId(googleClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = CredentialManager.create(context).getCredential(context, request)
                val cred = result.credential
                if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    vm.signInWithGoogle(context, GoogleIdTokenCredential.createFrom(cred.data).idToken)
                }
            } catch (_: Exception) { /* cancelado / sin cuentas — no es error duro */ }
        }
    }
    fun guest() {
        Analytics.track(context, Analytics.REGISTER_START, mapOf("method" to "device"))
        onTryGuest?.invoke() ?: vm.signInWithDevice(context)
    }

    val loading = state is LoginState.Loading

    Column(
        modifier = Modifier.fillMaxSize().background(Lc.Surface).padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Hero: mascota + marca + tagline ──────────────────────────────────
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HootMascot(Modifier.size(108.dp), sad = state is LoginState.Error)
            Spacer(Modifier.height(26.dp))
            Text("Fluenta", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Lc.Ink)
            Spacer(Modifier.height(8.dp))
            Text(
                I18nStore.t("login.tagline", "Habla un idioma nuevo desde el primer día."),
                fontSize = 15.sp, color = Lc.Sub, textAlign = TextAlign.Center, lineHeight = 22.sp,
                modifier = Modifier.widthIn(max = 250.dp),
            )
        }

        // ── Zona de acciones ─────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state is LoginState.Error) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Lc.ErrBg).padding(11.dp, 11.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("⚠", fontSize = 18.sp, color = Lc.ErrIcon)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        (state as LoginState.Error).message.ifBlank {
                            I18nStore.t("login.errConn", "No pudimos conectar. Revisa tu internet e inténtalo de nuevo.")
                        },
                        fontSize = 13.sp, color = Lc.ErrInk, lineHeight = 19.sp,
                    )
                }
            }

            if (showEmail && state !is LoginState.Loading) {
                // Flujo de email (revelado por el botón primario)
                OutlinedTextField(
                    value = email, onValueChange = { email = it }, singleLine = true,
                    label = { Text(I18nStore.t("login.emailLabel", "Tu email")) },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state !is LoginState.OtpSent,
                )
                if (state is LoginState.OtpSent) {
                    OutlinedTextField(
                        value = code, onValueChange = { code = it }, singleLine = true,
                        label = { Text(I18nStore.t("login.codeLabelEmail", "Código del email")) },
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Hard3d("✓ " + I18nStore.t("login.verify", "Verificar"), onClick = { vm.verifyEmailOtp(context, email, code) }, enabled = code.isNotBlank())
                } else {
                    Hard3d(I18nStore.t("login.sendCodeEmail", "Enviar código al email"), icon = Icons.Default.MailOutline, onClick = {
                        Analytics.track(context, Analytics.REGISTER_START, mapOf("method" to "email"))
                        vm.requestEmailOtp(email)
                    }, enabled = email.isNotBlank())
                }
                GuestText(I18nStore.t("login.back", "← Volver")) { showEmail = false; vm.reset() }
            } else {
                // Estado por defecto / cargando: 3 botones en jerarquía.
                Hard3d(
                    text = if (loading) I18nStore.t("login.connecting", "Conectando…") else I18nStore.t("login.email", "Continuar con email"),
                    icon = if (loading) null else Icons.Default.MailOutline,
                    showSpinner = loading,
                    onClick = { showEmail = true },
                    enabled = !loading,
                )
                if (state is LoginState.Error) {
                    Hard3d(I18nStore.t("common.retry", "Reintentar"), icon = Icons.Default.Refresh, onClick = { vm.reset() })
                }
                if (googleClientId.isNotBlank()) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color.White).border(1.dp, Lc.Border, RoundedCornerShape(16.dp))
                            .clickable(enabled = !loading) { launchGoogleSignIn() }.padding(14.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("G", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (loading) Lc.Legal else Lc.GoogleBlue)
                        Spacer(Modifier.width(10.dp))
                        Text(I18nStore.t("login.continueGoogle", "Continuar con Google"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (loading) Lc.Legal else Lc.Ink)
                    }
                }
                GuestText(I18nStore.t("login.guest", "Entrar como invitado"), enabled = !loading) { guest() }
            }

            // Legal
            if (state !is LoginState.OtpSent) {
                Text(
                    I18nStore.t("login.legal", "Al continuar aceptas los Términos y la Privacidad."),
                    fontSize = 11.sp, color = Lc.Legal, textAlign = TextAlign.Center, lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

// Botón primario con relieve 3D (box-shadow 0 4px 0 TealDark del diseño).
@Composable
private fun Hard3d(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    showSpinner: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(54.dp)) {
        Box(Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(16.dp)).background(Lc.TealDark))
        Box(
            Modifier.fillMaxWidth().height(50.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(16.dp))
                .background(if (enabled) Lc.Teal else Lc.Teal.copy(alpha = 0.7f)).clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (showSpinner) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(10.dp))
                } else if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(9.dp))
                }
                Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun GuestText(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        color = if (enabled) Lc.TealDark else Lc.Legal, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick).wrapContentHeight(),
    )
}
