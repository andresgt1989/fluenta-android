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
import com.alturya.fluenta.data.I18nStore

@Composable
fun LoginScreen(onSuccess: (Boolean) -> Unit, onTryGuest: (() -> Unit)? = null) {
    val context = LocalContext.current
    val vm: LoginViewModel = viewModel()
    val state by vm.state.collectAsState()

    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is LoginState.Success) onSuccess((state as LoginState.Success).isNewUser)
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
            I18nStore.t("login.tagline", "Aprende idiomas de verdad, con tu coach en WhatsApp"),
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

        if (state is LoginState.OtpSent || state is LoginState.Error) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(I18nStore.t("login.codeLabel", "Código de WhatsApp")) },
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
                onClick = { vm.requestOtp(phone) },
                enabled = phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(I18nStore.t("login.sendCode", "Enviar código por WhatsApp")) }
        } else {
            Button(
                onClick = { vm.verifyOtp(context, phone, code) },
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
            I18nStore.t("login.privacyHint", "Te enviaremos un código por WhatsApp para entrar."),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // OTP delivery fallback: WhatsApp only delivers a plain-text code inside the
        // 24h service window. If the user never messaged the bot, the code won't
        // arrive — so nudge them to open WhatsApp first, then re-request the code.
        if (state is LoginState.OtpSent || state is LoginState.Error) {
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
