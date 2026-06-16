package com.alturya.fluenta.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onSuccess: (Boolean) -> Unit, onTryGuest: (() -> Unit)? = null) {
    val context = LocalContext.current
    val vm: LoginViewModel = viewModel()
    val state by vm.state.collectAsState()

    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    // Email es el canal primario (no depende de Meta). Teléfono = secundario.
    var mode by remember { mutableStateOf("email") }
    // Google Web client ID — lo trae el backend (fuente única). Vacío = Google off.
    var googleClientId by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { ApiClient.apiNoAuth.getAuthConfig().googleClientId }
            .getOrNull()?.let { googleClientId = it }
    }

    LaunchedEffect(state) {
        if (state is LoginState.Success) onSuccess((state as LoginState.Success).isNewUser)
    }

    // Lanza el selector de cuentas de Google (Credential Manager) y canjea el ID token.
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
                if (cred is CustomCredential &&
                    cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                    vm.signInWithGoogle(context, googleCred.idToken)
                }
            } catch (_: Exception) {
                // Usuario canceló o no hay cuentas/Play Services — no es un error duro.
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Brand hero ────────────────────────────────────────────────────────
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🗣️", style = MaterialTheme.typography.displaySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Fluenta",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            I18nStore.t("login.tagline", "Habla un idioma nuevo con tu tutor de IA"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        // Play-first onboarding: try a lesson before registering
        if (onTryGuest != null) {
            Button(
                onClick = onTryGuest,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(
                    I18nStore.t("login.tryNow", "▶ Probar una lección ahora"),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                I18nStore.t("login.orCreateAccount", "— o crea tu cuenta gratis —"),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }

        // Google Sign-In: un toque, sin OTP, sin Meta. Canal primario cuando está configurado.
        if (googleClientId.isNotBlank() && state !is LoginState.OtpSent) {
            Button(
                onClick = { launchGoogleSignIn() },
                enabled = state !is LoginState.Loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(I18nStore.t("login.continueGoogle", "Continuar con Google")) }
            Spacer(Modifier.height(12.dp))
            Text(
                I18nStore.t("login.orWithCode", "— o con un código —"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        // Selector de canal: Email (primario) / Teléfono. Solo antes de pedir código.
        if (state !is LoginState.OtpSent) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeButton("📧 Email", mode == "email", { mode = "email"; vm.reset() }, Modifier.weight(1f))
                ModeButton("📱 Teléfono", mode == "phone", { mode = "phone"; vm.reset() }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }

        if (mode == "email") {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(I18nStore.t("login.emailLabel", "Tu email")) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is LoginState.OtpSent && state !is LoginState.Loading,
            )
        } else {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(I18nStore.t("login.phoneLabel", "Teléfono (ej: +5491123456789)")) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is LoginState.OtpSent && state !is LoginState.Loading,
            )
        }

        if (state is LoginState.OtpSent || state is LoginState.Error) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(if (mode == "email") I18nStore.t("login.codeLabelEmail", "Código del email") else I18nStore.t("login.codeLabel", "Código de WhatsApp")) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))

        if (state is LoginState.Loading) {
            CircularProgressIndicator()
        } else if (state !is LoginState.OtpSent) {
            Button(
                onClick = {
                    Analytics.track(context, Analytics.REGISTER_START, mapOf("method" to mode))
                    if (mode == "email") vm.requestEmailOtp(email) else vm.requestOtp(phone)
                },
                enabled = if (mode == "email") email.isNotBlank() else phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(
                    if (mode == "email") I18nStore.t("login.sendCodeEmail", "Enviar código al email")
                    else I18nStore.t("login.sendCode", "Enviar código por WhatsApp"),
                )
            }
        } else {
            Button(
                onClick = { if (mode == "email") vm.verifyEmailOtp(context, email, code) else vm.verifyOtp(context, phone, code) },
                enabled = code.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(I18nStore.t("login.verify", "Verificar")) }
        }

        if (state is LoginState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                (state as LoginState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            if (mode == "email") I18nStore.t("login.privacyHintEmail", "Te enviaremos un código a tu email para entrar.")
            else I18nStore.t("login.privacyHint", "Te enviaremos un código por WhatsApp para entrar."),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Solo en modo teléfono: fallback de la ventana de 24h de WhatsApp.
        if (mode == "phone" && (state is LoginState.OtpSent || state is LoginState.Error)) {
            Spacer(Modifier.height(16.dp))
            Text(
                I18nStore.t("login.otpHelp", "¿No te llega el código? Escríbele al bot por WhatsApp y vuelve a tocar \"Enviar\"."),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://wa.me/593982645527?text=Hola"),
                        ),
                    )
                }
            }) { Text(I18nStore.t("login.openWhatsapp", "Abrir WhatsApp")) }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}
