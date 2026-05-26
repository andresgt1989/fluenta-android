package com.alturya.fluenta.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName

@Composable
fun ProfileScreen(
    onChangeLanguage: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDiagnostic: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel()
    val state by vm.state.collectAsState()

    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val p = state.profile
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📱 ", style = MaterialTheme.typography.titleMedium)
                    Text(p?.phone ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text("Aprendiendo: ${flag(p?.l2)} ${langName(p?.l2)}")
                Text("Desde: ${langName(p?.l1)}")
                Text("Nivel: ${levelLabel(p?.level, p?.levelSystem)} (${levelSystemName(p?.levelSystem)})")
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Plan: ${(p?.plan ?: "free").uppercase()}") }
                )
            }
        }

        if ((p?.plan ?: "free") == "free") {
            Text("Mejora tu plan", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { vm.openCheckout("basic") { open(it) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Suscribirme a Basic") }
            OutlinedButton(
                onClick = { vm.openCheckout("pro") { open(it) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Suscribirme a Pro") }
        } else {
            Button(
                onClick = { vm.openPortal { open(it) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Gestionar suscripción") }
        }

        Divider(Modifier.padding(vertical = 8.dp))

        OutlinedButton(
            onClick = onDiagnostic,
            modifier = Modifier.fillMaxWidth()
        ) { Text("🎯  Realiza tu test de nivel") }

        OutlinedButton(
            onClick = onChangeLanguage,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Cambiar idioma") }

        TextButton(
            onClick = { vm.logout(context, onLogout) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Cerrar sesión") }
    }
}
