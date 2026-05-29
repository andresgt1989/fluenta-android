package com.alturya.fluenta.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.LeagueRow

private fun tierEmoji(tier: String): String = when (tier) {
    "bronze" -> "🥉"
    "silver" -> "🥈"
    "gold" -> "🥇"
    "diamond" -> "💎"
    else -> "🏅"
}

private fun tierName(tier: String): String = when (tier) {
    "bronze" -> I18nStore.t("league.tierBronze", "Liga Bronce")
    "silver" -> I18nStore.t("league.tierSilver", "Liga Plata")
    "gold" -> I18nStore.t("league.tierGold", "Liga Oro")
    "diamond" -> I18nStore.t("league.tierDiamond", "Liga Diamante")
    else -> I18nStore.t("league.default", "Liga")
}

@Composable
fun LeagueCard() {
    val vm: LeagueViewModel = viewModel()
    val state by vm.state.collectAsState()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
                return@Column
            }
            val league = state.league
            if (league == null) {
                Text(I18nStore.t("league.weekly", "Liga semanal"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    I18nStore.t("league.empty", "Completa lecciones esta semana para entrar a la tabla y competir por subir de liga."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tierEmoji(league.tier), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(tierName(league.tier), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    league.myRank?.let {
                        Text("${I18nStore.t("league.position", "Tu posición")}: #$it · ${league.myWeeklyXp} XP", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (league.board.isEmpty()) {
                Text(I18nStore.t("league.emptyBoard", "Aún nadie tiene XP esta semana. ¡Sé el primero!"), style = MaterialTheme.typography.bodyMedium)
            } else {
                league.board.take(10).forEach { row ->
                    LeagueRowItem(row, promotionCutoff = league.promotionCutoff)
                }
                if (league.myRank != null && league.myRank > 10) {
                    Divider(Modifier.padding(vertical = 6.dp))
                    league.board.find { it.isMe }?.let { LeagueRowItem(it, promotionCutoff = league.promotionCutoff) }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                I18nStore.t("league.promotion", "Top {n} suben de liga el domingo 🔼").replace("{n}", "${league.promotionCutoff}"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun LeagueRowItem(row: LeagueRow, promotionCutoff: Int) {
    val inPromotion = row.rank <= promotionCutoff
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (row.isMe) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${row.rank}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (inPromotion) FontWeight.Bold else FontWeight.Normal,
            color = if (inPromotion) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(28.dp),
        )
        Text(
            row.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (row.isMe) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text("${row.weeklyXp} XP", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
