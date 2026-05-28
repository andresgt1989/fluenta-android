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
fun LoginScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val vm: LoginViewModel = viewModel()
    val state by vm.state.collectAsState()

    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is LoginState.Success) onSuccess()
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
        Spacer(Modifier.height(36.dp))

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
    }
}
