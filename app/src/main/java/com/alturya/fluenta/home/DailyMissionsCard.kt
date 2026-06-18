package com.alturya.fluenta.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.R
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.ui.theme.FluentaAmber
import com.alturya.fluenta.ui.theme.FluentaSuccess

/**
 * Tarjeta de **Misiones de hoy** — el componente de retención estilo Duolingo.
 * Muestra 3 misiones diarias (racha / meta / repaso) derivadas de datos reales,
 * cada una con progreso y check. La mascota saluda mientras hay pendientes y
 * celebra cuando se completan todas. Tocar una misión lleva a su acción.
 */
@Composable
fun DailyMissionsCard(
    missions: List<DailyMission>,
    onMission: (MissionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (missions.isEmpty()) return
    val done = DailyMissions.completedCount(missions)
    val total = missions.size
    val allDone = DailyMissions.allDone(missions)

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            // ── Cabecera: mascota + título + contador ──────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(
                        if (allDone) R.drawable.ic_fluenta_celebra else R.drawable.ic_fluenta_saluda
                    ),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        I18nStore.t("missions.title", "Misiones de hoy"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        if (allDone) I18nStore.t("missions.allDone", "¡Completaste tus misiones! 🎉")
                        else I18nStore.t("missions.subtitle", "Pequeñas metas que crean el hábito"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Contador en pastilla
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (allDone) FluentaSuccess else FluentaAmber)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        "$done/$total",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            missions.forEach { m ->
                MissionRow(m, onClick = { onMission(m.action) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun MissionRow(m: DailyMission, onClick: () -> Unit) {
    val rowColor =
        if (m.done) FluentaSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowColor)
            .clickable(enabled = !m.done, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(m.emoji, style = MaterialTheme.typography.titleLarge)
        Column(Modifier.weight(1f)) {
            Text(
                m.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { m.pct / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = if (m.done) FluentaSuccess else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
            )
        }
        if (m.done) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = I18nStore.t("missions.done", "Completada"),
                tint = FluentaSuccess,
                modifier = Modifier.size(26.dp),
            )
        } else {
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
