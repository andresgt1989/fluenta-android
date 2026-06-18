package com.alturya.fluenta.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore

@Composable
fun ReferralCard() {
    val vm: ReferralViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Don't render a broken-looking "no disponible" card if referrals failed to load —
    // hide it entirely until there's real data.
    if (!state.loading && state.referral == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    I18nStore.t("referral.title", "Invita y gana días Pro"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(4.dp))

            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                return@Column
            }

            val ref = state.referral
            if (ref == null) {
                Text(
                    I18nStore.t("referral.unavailable", "Referidos no disponibles"),
                    style = MaterialTheme.typography.bodySmall,
                )
                return@Column
            }

            Text(
                I18nStore.t("referral.subtitle", "Por cada amigo que complete su primera lección, ambos ganan {n} días Pro gratis.")
                    .replace("{n}", "${ref.rewardDays}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(14.dp))

            // Referral code + copy tap
            var copied by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("referral_code", ref.code))
                        copied = true
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    ref.code,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = if (copied) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (copied) I18nStore.t("referral.copied", "Copiado") else I18nStore.t("referral.copy", "Copiar"),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (copied) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RefStat(Icons.Default.Group, "${ref.referred}", I18nStore.t("referral.invited", "Invitados"), Modifier.weight(1f))
                RefStat(Icons.Default.CheckCircle, "${ref.activated}", I18nStore.t("referral.activated", "Completaron lección"), Modifier.weight(1f))
                RefStat(Icons.Default.CardGiftcard, "${ref.activated * ref.rewardDays}d", I18nStore.t("referral.earned", "Días ganados"), Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))

            // Share buttons — system chooser primary (reaches all apps), WhatsApp shortcut secondary
            Button(
                onClick = {
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, ref.shareText)
                        },
                        I18nStore.t("referral.share", "Compartir código"),
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(I18nStore.t("referral.shareBtn", "Compartir mi código"), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, ref.shareText)
                        setPackage("com.whatsapp")
                    }
                    runCatching { context.startActivity(intent) }.onFailure {
                        context.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, ref.shareText) },
                            I18nStore.t("referral.share", "Compartir código"),
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(I18nStore.t("referral.shareWa", "WhatsApp"), fontWeight = FontWeight.Bold, color = Color(0xFF25D366))
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(Modifier.height(14.dp))

            // Claim a friend's code
            Text(
                I18nStore.t("referral.claimTitle", "¿Tienes un código de un amigo?"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.claimInput,
                    onValueChange = vm::setClaimInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(I18nStore.t("referral.codePlaceholder", "Código de amigo"), style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); vm.claim() }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Button(
                    onClick = { keyboard?.hide(); vm.claim() },
                    enabled = state.claimInput.trim().length >= 4 && !state.claiming,
                ) {
                    if (state.claiming) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(I18nStore.t("referral.claimBtn", "Aplicar"))
                }
            }
            state.claimMessage?.let { msg ->
                Spacer(Modifier.height(6.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.claimSuccess == true) Color(0xFF15803D) else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RefStat(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
