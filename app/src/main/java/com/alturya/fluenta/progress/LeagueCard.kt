package com.alturya.fluenta.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Star
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
import com.alturya.fluenta.ui.ShimmerCard

private fun tierName(tier: String): String = when (tier) {
    "bronze" -> I18nStore.t("league.tierBronze", "Liga Bronce")
    "silver" -> I18nStore.t("league.tierSilver", "Liga Plata")
    "gold" -> I18nStore.t("league.tierGold", "Liga Oro")
    "diamond" -> I18nStore.t("league.tierDiamond", "Liga Diamante")
    else -> I18nStore.t("league.default", "Liga")
}

private fun tierColor(tier: String): Color = when (tier) {
    "bronze" -> Color(0xFFCD7F32)
    "silver" -> Color(0xFF94A3B8)
    "gold" -> Color(0xFFEAB308)
    "diamond" -> Color(0xFF38BDF8)
    else -> Color(0xFF6366F1)
}

@Composable
fun LeagueCard() {
    val vm: LeagueViewModel = viewModel()
    val state by vm.state.collectAsState()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    I18nStore.t("league.weekly", "Liga semanal"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (state.refreshing && state.league != null) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.height(8.dp))

            if (state.loading) {
                ShimmerCard(height = 56.dp)
                return@Column
            }
            val league = state.league
            if (league == null) {
                Text(
                    I18nStore.t("league.empty", "Completa lecciones esta semana para entrar a la tabla y competir por subir de liga."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }

            // Tier header
            val tc = tierColor(league.tier)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = tc.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = tc, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            tierName(league.tier),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tc,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                league.myRank?.let {
                    Text(
                        "#$it · ${league.myWeeklyXp} XP",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // XP progress bar toward promotion
            val topXp = league.board.firstOrNull()?.weeklyXp?.takeIf { it > 0 } ?: 1
            val myXp = league.myWeeklyXp
            val promotionXp = league.board.getOrNull(league.promotionCutoff - 1)?.weeklyXp ?: topXp
            val progressFraction = (myXp.toFloat() / promotionXp.coerceAtLeast(1)).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(progressFraction, tween(800), label = "xpBar")

            Spacer(Modifier.height(10.dp))
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            I18nStore.t("league.toPromotion", "Para subir de liga"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "$myXp / $promotionXp XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = tc,
                    trackColor = tc.copy(alpha = 0.15f),
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            if (league.board.isEmpty()) {
                Text(I18nStore.t("league.emptyBoard", "Aún nadie tiene XP esta semana. ¡Sé el primero!"), style = MaterialTheme.typography.bodyMedium)
            } else {
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { shown = true }
                league.board.take(10).forEachIndexed { idx, row ->
                    AnimatedVisibility(
                        visible = shown,
                        enter = slideInVertically(tween(durationMillis = 200, delayMillis = idx * 40)) { it / 2 } + fadeIn(tween(150, idx * 40)),
                    ) {
                        LeagueRowItem(row, promotionCutoff = league.promotionCutoff, tierColor = tc)
                    }
                }
                if (league.myRank != null && league.myRank > 10) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    league.board.find { it.isMe }?.let {
                        LeagueRowItem(it, promotionCutoff = league.promotionCutoff, tierColor = tc)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                I18nStore.t("league.promotion", "Top {n} suben de liga el domingo").replace("{n}", "${league.promotionCutoff}"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun LeagueRowItem(row: LeagueRow, promotionCutoff: Int, tierColor: Color) {
    val inPromotion = row.rank <= promotionCutoff
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    row.isMe -> MaterialTheme.colorScheme.primaryContainer
                    inPromotion -> tierColor.copy(alpha = 0.07f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val medalColor = when (row.rank) {
            1 -> Color(0xFFEAB308)
            2 -> Color(0xFF94A3B8)
            3 -> Color(0xFFCD7F32)
            else -> null
        }
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            if (medalColor != null) {
                Icon(
                    imageVector = if (row.rank == 1) Icons.Default.EmojiEvents else Icons.Default.Star,
                    contentDescription = null,
                    tint = medalColor,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    "${row.rank}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (inPromotion) FontWeight.Bold else FontWeight.Normal,
                    color = if (inPromotion) tierColor else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            row.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (row.isMe) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${row.weeklyXp} XP",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (row.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (inPromotion) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = tierColor, modifier = Modifier.size(14.dp))
        }
    }
}
