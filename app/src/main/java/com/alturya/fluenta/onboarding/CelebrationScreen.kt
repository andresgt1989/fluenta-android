package com.alturya.fluenta.onboarding

// Onboarding sin fricción · pantalla 05 (mock): celebración + REGISTRO DIFERIDO.
// El usuario ya hizo su 1ª micro-lección de tono en modo invitado (el "ajá"); el
// registro es el ÚLTIMO paso — eso es lo que ataca la fuga #1 (~4% llega a lección).
// "Seguir como invitado" continúa al HOME sin obligar a crear cuenta.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alturya.fluenta.R
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.ui.FluentaButton
import com.alturya.fluenta.ui.FluentaButtonStyle
import com.alturya.fluenta.ui.theme.FluentaTokens

@Composable
fun CelebrationScreen(onRegister: () -> Unit, onGuest: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(FluentaTokens.Surface)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_fluenta_hola),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                I18nStore.t("onboarding.celebrationTitle", "¡Tu primera palabra en chino!"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = FluentaTokens.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            // La palabra aprendida (妈 · mā · mamá) en tarjeta destacada.
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, FluentaTokens.Border),
            ) {
                Column(
                    Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("妈", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = FluentaTokens.Ink)
                    Text("mā · ${I18nStore.t("onboarding.celebrationGloss", "mamá")}", fontSize = 16.sp, color = FluentaTokens.Muted)
                }
            }
            Spacer(Modifier.height(16.dp))
            // Pill de XP ganado.
            Surface(shape = RoundedCornerShape(999.dp), color = FluentaTokens.Container) {
                Text(
                    "⚡ +10 XP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FluentaTokens.BrandText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        // Registro diferido — el último paso.
        Text(
            I18nStore.t("onboarding.saveProgress", "Guarda tu progreso"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = FluentaTokens.Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            I18nStore.t("onboarding.saveProgressSub", "Crea tu cuenta para no perder tu racha."),
            style = MaterialTheme.typography.bodyMedium,
            color = FluentaTokens.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        FluentaButton(
            text = I18nStore.t("onboarding.continueGoogle", "Continuar con Google"),
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        FluentaButton(
            text = I18nStore.t("onboarding.createEmail", "Crear cuenta con email"),
            onClick = onRegister,
            style = FluentaButtonStyle.Neutral,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = onGuest,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
        ) {
            Text(
                I18nStore.t("onboarding.continueGuest", "Seguir como invitado"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = FluentaTokens.BrandText,
            )
        }
    }
}
