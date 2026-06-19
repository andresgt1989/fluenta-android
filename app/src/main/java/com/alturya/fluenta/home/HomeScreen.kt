package com.alturya.fluenta.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.data.GoalStore
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.ui.ShimmerCard
import com.alturya.fluenta.network.CoachAction
import com.alturya.fluenta.progress.LeagueViewModel
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelIndex
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelLadderShort
import com.alturya.fluenta.util.levelSystemName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSeeMap: () -> Unit = {},
    onPronunciation: () -> Unit = {},
    onPlayMatch: () -> Unit = {},
    onStartLesson: (String) -> Unit = {},
    onChangeLanguage: () -> Unit = {},
    onRepaso: () -> Unit = {},
    onLevelTest: () -> Unit = {},
    onConversation: () -> Unit = {},
    onConversationLesson: (String) -> Unit = {},
    onScript: (String) -> Unit = {},
    onUpgrade: () -> Unit = {},
    previewState: HomeState? = null,
) {
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState
    val p = state.profile
    val scope = rememberCoroutineScope()
    // Re-fetch when the user changes their learning language elsewhere, so Home never
    // shows a stale language (the old Chinese-after-switching-to-French bug).
    val reloadSignal by com.alturya.fluenta.data.Session.reloadSignal.collectAsState()
    LaunchedEffect(reloadSignal) {
        if (previewState == null && reloadSignal > 0) vm.load()
    }
    val pullState = rememberPullToRefreshState()
    val localGoalXp by remember { GoalStore.flow(context) }.collectAsState(initial = 50)
    var showGoalDialog by remember { mutableStateOf(false) }
    if (pullState.isRefreshing) {
        LaunchedEffect(Unit) {
            if (previewState == null) vm.load()
            pullState.endRefresh()
        }
    }

    if (state.loading) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ShimmerCard(height = 64.dp)
            ShimmerCard(height = 120.dp)
            ShimmerCard(height = 88.dp)
            ShimmerCard(height = 200.dp)
        }
        return
    }

    Box(Modifier.fillMaxSize().nestedScroll(pullState.nestedScrollConnection)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header: saludo + chip de idioma (tap para cambiar) ─────────────────
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(com.alturya.fluenta.R.drawable.ic_fluenta_hola),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    I18nStore.t("home.greeting", "Hola"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onChangeLanguage,
                shape = MaterialTheme.shapes.large,
                color = if (p?.l2 == null) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (p?.l2 == null) {
                        Text(
                            I18nStore.t("home.chooseLanguage", "Elegir idioma →"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        Text(
                            "${flag(p.l2)} ${langName(p.l2)} · ${levelLabel(p.level, p.levelSystem)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "⇄ ${I18nStore.t("home.changeLanguage", "cambiar")}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // ── Fallo total de carga (perfil + coach nulos): no fallar en silencio ─
        if (p == null && state.coach == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        I18nStore.t("home.loadError", "No pudimos cargar tus datos. Revisa tu conexión."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.load() }) {
                        Text(I18nStore.t("common.retry", "Reintentar"), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── "¿Idioma equivocado?" — escape hatch prominente ───────────────────
        // Si el usuario tiene un idioma asignado pero CERO progreso (lecciones, XP,
        // racha = 0), es muy probable que cayó en el idioma equivocado (p.ej. chino
        // por default). Le damos una salida grande e imposible de no ver.
        val noProgressYet = (state.progress?.completedLessons ?: 0) == 0 &&
                (state.progress?.totalXp ?: 0) == 0 &&
                (state.progress?.streakDays ?: 0) == 0
        if (p?.l2 != null && noProgressYet) {
            Surface(
                onClick = onChangeLanguage,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            I18nStore.t("home.wrongLangTitle", "¿Querías aprender {lang}?")
                                .replace("{lang}", langName(p.l2)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            I18nStore.t("home.wrongLangHint", "Toca aquí para cambiar a otro idioma."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // ── EL TUTOR IA: protagonista del Home. Mensaje proactivo (en tu idioma)
        // + progreso a la meta (C1/HSK/JLPT) + LA acción siguiente. El coach
        // ORQUESTA todo el recorrido (script → test → repaso → lección → conversar),
        // así que reemplaza el desorden de tarjetas sueltas por UN solo foco.
        val runCoachAction: (CoachAction) -> Unit = { a ->
            when (a.type) {
                "script" -> onScript(state.coach?.l2 ?: p?.l2 ?: "")
                "placement" -> onLevelTest()
                "review" -> onRepaso()
                "conversation" -> { Analytics.track(context, Analytics.CONVERSATION_START); onConversation() }
                "lesson" -> {
                    val n = state.nextLesson
                    if (n?.id != null) {
                        Analytics.track(context, Analytics.LESSON_START, mapOf("lessonType" to (n.lessonType ?: "")))
                        if (n.lessonType == "roleplay" || n.lessonType == "free_chat") onConversationLesson(n.id) else onStartLesson(n.id)
                    } else onSeeMap()
                }
                else -> onSeeMap()
            }
        }
        val coach = state.coach
        if (coach != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                onClick = { runCoachAction(coach.action) },
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(I18nStore.t("home.yourTutor", "Tu tutor"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        coach.message,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "${I18nStore.t("home.progressTo", "Progreso a")} ${(coach.goal ?: "c1").uppercase()} · ${coach.progressPct}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (coach.progressPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(color = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.large) {
                        Text(
                            "▶ ${coach.action.label}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        } else {
            // Fallback (sin conexión / coach no disponible): no dejar al usuario sin acción.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                onClick = onSeeMap,
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        I18nStore.t("home.startHereTitle", "Empieza tu lección"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        // ── Viaje de nivel (CEFR/HSK/JLPT) — dónde estás y a dónde vas ─────────
        LevelJourneyCard(level = p?.level, levelSystem = p?.levelSystem, onLevelTest = onLevelTest)

        // ── Streak hero + stats ───────────────────────────────────────────────
        val streak = state.progress?.streakDays ?: 0
        val totalXp = state.progress?.totalXp ?: 0
        val completedLessons = state.progress?.completedLessons ?: 0
        if (streak == 0 && totalXp == 0 && completedLessons == 0) {
            // New user — skip triple-zeros, show first-day call to action instead
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🎯", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            I18nStore.t("home.firstDayTitle", "¡Empieza hoy!"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            I18nStore.t("home.firstDayHint", "Tu primera lección te toma solo 5 minutos."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        } else {
            StatsHero(streak = streak, xp = totalXp, lessons = completedLessons)
        }

        // ── Meta diaria (daily goal ring) ─────────────────────────────────────
        val prog = state.progress
        if (prog != null) {
            val localPct = if (localGoalXp > 0) ((prog.todayXp * 100) / localGoalXp).coerceIn(0, 100) else 0
            DailyGoalBar(
                todayXp = prog.todayXp,
                goalXp = localGoalXp,
                pct = localPct,
                onClick = { showGoalDialog = true },
            )
        }

        // ── Misiones de hoy (daily quests) — motor de retención client-side ────
        // Derivadas de señales REALES del backend (todayXp / cardsDueToday); el
        // backend las resetea cada día, así que se reinician solas.
        val missions = DailyMissions.build(
            progress = state.progress,
            goalXp = localGoalXp,
            titleStreak = I18nStore.t("missions.streak", "Preséntate hoy — protege tu racha"),
            titleGoal = I18nStore.t("missions.goal", "Alcanza tu meta diaria"),
            titleReview = I18nStore.t("missions.review", "Despeja tu repaso (SRS)"),
        )
        DailyMissionsCard(
            missions = missions,
            onMission = { action ->
                when (action) {
                    MissionAction.Review -> { Analytics.track(context, Analytics.LESSON_START, mapOf("from" to "mission_review")); onRepaso() }
                    MissionAction.Lesson, MissionAction.Goal -> {
                        val n = state.nextLesson
                        if (n?.id != null) {
                            Analytics.track(context, Analytics.LESSON_START, mapOf("from" to "mission", "lessonType" to (n.lessonType ?: "")))
                            if (n.lessonType == "roleplay" || n.lessonType == "free_chat") onConversationLesson(n.id) else onStartLesson(n.id)
                        } else onSeeMap()
                    }
                }
            },
        )

        // ── Goal picker dialog ────────────────────────────────────────────────
        if (showGoalDialog) {
            AlertDialog(
                onDismissRequest = { showGoalDialog = false },
                title = {
                    Text(
                        I18nStore.t("goal.dialogTitle", "Tu meta diaria"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column {
                        Text(
                            I18nStore.t("goal.dialogSubtitle", "Elige cuánto quieres estudiar cada día:"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        GoalStore.OPTIONS.forEach { opt ->
                            val selected = opt.xp == localGoalXp
                            Card(
                                onClick = {
                                    scope.launch { GoalStore.set(context, opt.xp) }
                                    showGoalDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(opt.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(I18nStore.t("goal.${opt.xp}.desc", opt.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (selected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGoalDialog = false }) {
                        Text(I18nStore.t("common.close", "Cerrar"))
                    }
                },
            )
        }

        // ── Coach message ─────────────────────────────────────────────────────
        state.coachMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    state.affectiveState?.let { st ->
                        Spacer(Modifier.height(6.dp))
                        AffectiveChip(st)
                    }
                }
            }
        }

        // ── Repaso (SRS) — acción diaria primaria de retención ────────────────
        val dueCount = state.progress?.cardsDueToday ?: 0
        Card(
            onClick = onRepaso,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (dueCount > 0) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (dueCount > 0)
                            I18nStore.t("home.reviewDue", "Repasar ($dueCount vencen hoy)")
                                .replace("\$dueCount", "$dueCount")
                        else I18nStore.t("home.reviewTitle", "Repasar tus errores"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        if (dueCount > 0) I18nStore.t("home.reviewUrgent", "¡No dejes que se olviden! Toca aquí")
                        else I18nStore.t("home.reviewSubtitle", "Refuerza lo que se te olvida (SRS)"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        // ── Liga semanal teaser — visible desde Home para enganchar competencia ─
        LeagueTeaserCard()

        // ── Secundario: COLAPSADO por defecto (coach enfocado, no menú) ────────
        var showMore by remember { mutableStateOf(false) }
        Surface(
            onClick = { showMore = !showMore },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "${I18nStore.t("home.morePractice", "Más práctica")} ${if (showMore) "▴" else "▾"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (showMore) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(Icons.AutoMirrored.Filled.VolumeUp, I18nStore.t("home.pronunciationShort", "Pronunciación"), onClick = onPronunciation)
                ActionCard(Icons.Default.SportsEsports, I18nStore.t("home.gameShort", "Juego"), onClick = onPlayMatch)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(Icons.Default.Map, I18nStore.t("home.mapShort", "Mi mapa"), onClick = onSeeMap)
                // Independencia de WhatsApp: conversar es IN-APP con el tutor de IA.
                ActionCard(
                    Icons.AutoMirrored.Filled.Chat,
                    I18nStore.t("home.conversationShort", "Conversar"),
                    onClick = onConversation,
                )
            }
        }
        // ── Streak at risk banner ─────────────────────────────────────────
        val todayXp = state.progress?.todayXp ?: 0
        if (streak > 0 && todayXp == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                onClick = onRepaso,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            I18nStore.t("home.streakAtRisk", "¡Tu racha de $streak días está en riesgo!"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            I18nStore.t("home.streakAtRiskHint", "Practica hoy para mantenerla."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // ── Upgrade nudge (free users) / Trial banner (trialing) ─────────
        if (p?.plan == null || p.plan == "free") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                onClick = onUpgrade,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Upgrade,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            I18nStore.t("home.upgradeCta", "Prueba Pro gratis 7 días"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            I18nStore.t("home.upgradeHint", "Escudo de racha · Sin anuncios · Offline"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        )
                    }
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else if (p.plan == "trialing") {
            TrialBanner()
        }

        Spacer(Modifier.height(8.dp))
    }  // Column
    PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))
    }  // Box
}

@Composable
private fun StatsHero(streak: Int, xp: Int, lessons: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Animated flame for the streak — gentle pulse.
            val transition = rememberInfiniteTransition(label = "flame")
            val flameScale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "flameScale",
            )
            HeroStat(
                icon = Icons.Default.LocalFireDepartment,
                value = "$streak",
                label = I18nStore.t("home.streak", "Racha"),
                iconScale = if (streak > 0) flameScale else 1f,
            )
            HeroStat(Icons.Default.Star, "$xp", I18nStore.t("home.xp", "XP"))
            HeroStat(Icons.AutoMirrored.Filled.MenuBook, "$lessons", I18nStore.t("home.lessons", "Lecciones"))
        }
    }
}

@Composable
private fun HeroStat(icon: ImageVector, value: String, label: String, iconScale: Float = 1f) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp).scale(iconScale))
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun RowScope.ActionCard(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f).height(92.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyGoalBar(todayXp: Int, goalXp: Int, pct: Int, onClick: () -> Unit = {}) {
    val fraction = (pct / 100f).coerceIn(0f, 1f)
    val done = pct >= 100
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (done) I18nStore.t("home.goalDone", "¡Meta de hoy completada!") else I18nStore.t("home.dailyGoal", "Meta de hoy"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$todayXp / $goalXp XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Edit, contentDescription = I18nStore.t("goal.change", "Cambiar meta"), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun LeagueTeaserCard() {
    val vm: LeagueViewModel = viewModel()
    val state by vm.state.collectAsState()
    val league = state.league ?: return

    val tierColor = when (league.tier) {
        "bronze" -> androidx.compose.ui.graphics.Color(0xFFCD7F32)
        "silver" -> androidx.compose.ui.graphics.Color(0xFF94A3B8)
        "gold" -> androidx.compose.ui.graphics.Color(0xFFEAB308)
        "diamond" -> androidx.compose.ui.graphics.Color(0xFF38BDF8)
        else -> MaterialTheme.colorScheme.primary
    }
    val tierLabel = when (league.tier) {
        "bronze" -> I18nStore.t("league.tierBronze", "Liga Bronce")
        "silver" -> I18nStore.t("league.tierSilver", "Liga Plata")
        "gold" -> I18nStore.t("league.tierGold", "Liga Oro")
        "diamond" -> I18nStore.t("league.tierDiamond", "Liga Diamante")
        else -> I18nStore.t("league.default", "Liga")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = tierColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tierLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                league.myRank?.let {
                    Text(
                        "#$it · ${league.myWeeklyXp} XP ${I18nStore.t("league.thisWeek", "esta semana")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val topN = league.promotionCutoff
            Text(
                I18nStore.t("league.topNPromo", "Top $topN sube").replace("$topN", "$topN"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TrialBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    I18nStore.t("home.trialActive", "Prueba Pro activa"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    I18nStore.t("home.trialHint", "Disfruta conversación IA, escudo de racha y sin anuncios"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Icon(Icons.Default.Star, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFEAB308), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AffectiveChip(affectiveState: String) {
    val (icon, label, chipColor) = when (affectiveState) {
        "motivated" -> Triple(Icons.Default.LocalFireDepartment, I18nStore.t("affective.motivated", "En racha"), MaterialTheme.colorScheme.error)
        "returning" -> Triple(Icons.Default.Refresh, I18nStore.t("affective.returning", "De regreso"), MaterialTheme.colorScheme.secondary)
        "at_risk" -> Triple(Icons.Default.Star, I18nStore.t("affective.atRisk", "Te extrañamos"), MaterialTheme.colorScheme.tertiary)
        "steady" -> Triple(Icons.Default.CheckCircle, I18nStore.t("affective.steady", "Constante"), MaterialTheme.colorScheme.primary)
        else -> Triple(Icons.Default.School, I18nStore.t("affective.new", "Empezando"), MaterialTheme.colorScheme.primary)
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon = { Icon(icon, contentDescription = null, tint = chipColor, modifier = Modifier.size(14.dp)) },
    )
}

// ── Viaje de nivel (ficha §7-BIS): "estás en B1 → meta C1" ────────────────────
// Sin nivel (test omitido) → nudge para hacer el test. Con nivel → escalera del
// marco del par (CEFR/JLPT/HSK/TOPIK) con la banda actual resaltada.
@Composable
private fun LevelJourneyCard(level: String?, levelSystem: String?, onLevelTest: () -> Unit) {
    val idx = levelIndex(level)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (idx < 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(I18nStore.t("home.discoverLevel", "Descubre tu nivel"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    I18nStore.t("home.discoverLevelHint", "Haz un test corto y ajustamos las lecciones a tu nivel real."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                com.alturya.fluenta.ui.FluentaButton(
                    text = I18nStore.t("home.takeLevelTest", "Hacer el test de nivel"),
                    onClick = onLevelTest,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                val ladder = levelLadderShort(levelSystem)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(I18nStore.t("home.yourJourney", "Tu camino"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${I18nStore.t("home.goal", "meta")} ${levelLabel("c1", levelSystem)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ladder.forEachIndexed { i, lbl ->
                        val current = i == idx
                        val done = i < idx
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = when {
                                current -> MaterialTheme.colorScheme.primary
                                done -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                lbl,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                color = when {
                                    current -> MaterialTheme.colorScheme.onPrimary
                                    done -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${I18nStore.t("home.youAreHere", "Estás en")} ${levelLabel(level, levelSystem)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
