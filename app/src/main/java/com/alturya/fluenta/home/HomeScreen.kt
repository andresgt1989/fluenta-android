package com.alturya.fluenta.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.data.GoalStore
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.CoachAction
import com.alturya.fluenta.ui.HootMascot
import com.alturya.fluenta.ui.ShimmerCard
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelLabel

// ── Tokens exactos del kit Claude Design (Fluenta HOME.dc.html · P1) ───────────
private val Ink = Color(0xFF15201D)
private val Muted = Color(0xFF5C6562)
private val Teal = Color(0xFF0E9D8E)
private val TealDark = Color(0xFF0A6F64)
private val MintTrack = Color(0xFFE7EEEB)
private val CardBorder = Color(0xFFEAF0ED)
private val RowDivider = Color(0xFFF0F4F2)
private val AmberBg = Color(0xFFFFE9C2)
private val Amber = Color(0xFFE08A00)
private val MintBg = Color(0xFFDDF3EE)
private val HeroMintTop = Color(0xFFE4F6F1)
private val HeroMintBottom = Color(0xFFCDEEE6)
private val HeroTitleInk = Color(0xFF0A4039)
private val HeroSegEmpty = Color(0xFFAFD9D1)
private val Coral = Color(0xFFE8554B)

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

    // ── Estado: CARGANDO (skeleton, sin salto de layout) ──────────────────────
    if (state.loading) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ShimmerCard(modifier = Modifier.width(130.dp), height = 38.dp)
                ShimmerCard(modifier = Modifier.width(96.dp), height = 30.dp)
            }
            ShimmerCard(modifier = Modifier.width(150.dp), height = 24.dp)
            ShimmerCard(height = 188.dp)
            ShimmerCard(height = 72.dp)
            ShimmerCard(height = 72.dp)
        }
        return
    }

    // ── Estado: ERROR total de carga → pantalla completa (kit) ────────────────
    if (p == null && state.coach == null && state.progress == null) {
        HomeErrorState(onRetry = { vm.load() }, onOffline = onSeeMap)
        return
    }

    if (showGoalDialog) {
        GoalDialog(
            localGoalXp = localGoalXp,
            onPick = { xp -> scope.launch { GoalStore.set(context, xp) }; showGoalDialog = false },
            onDismiss = { showGoalDialog = false },
        )
    }

    val streak = state.progress?.streakDays ?: 0
    val totalXp = state.progress?.totalXp ?: 0
    val completedLessons = state.progress?.completedLessons ?: 0
    val todayXp = state.progress?.todayXp ?: 0
    val dueCount = state.progress?.cardsDueToday ?: 0
    val showHanzi = state.scriptInfo != null
    val newUser = streak == 0 && totalXp == 0 && completedLessons == 0

    Box(Modifier.fillMaxSize().nestedScroll(pullState.nestedScrollConnection)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 24.dp),
        ) {
            // ── Header: chip de nivel/idioma + pills de racha/XP ──────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onChangeLanguage,
                    shape = RoundedCornerShape(999.dp),
                    color = if (p?.l2 == null) MaterialTheme.colorScheme.primaryContainer else Color.White,
                    border = if (p?.l2 == null) null else BorderStroke(1.dp, Color(0xFFDCE5E2)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("▾", color = Color(0xFF9AA39E), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (p?.l2 != null) {
                    HeaderPill(AmberBg, Icons.Default.LocalFireDepartment, Amber, "$streak", Color(0xFF9A5E00))
                    Spacer(Modifier.width(8.dp))
                    HeaderPill(MintBg, Icons.Default.Star, TealDark, "$totalXp", TealDark)
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Saludo + (meta de hoy) ────────────────────────────────────────
            Text(
                if (newUser) "¡${I18nStore.t("home.welcomeNew", "Bienvenida")}!" else "¡${I18nStore.t("home.greeting", "Hola")}!",
                color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
            )
            val remain = (localGoalXp - todayXp).coerceAtLeast(0)
            Text(
                when {
                    newUser -> I18nStore.t("home.firstStep", "Da tu primer paso. Hoot te acompaña.")
                    remain > 0 -> I18nStore.t("home.xpToGoal", "Te faltan {n} XP para tu meta de hoy").replace("{n}", "$remain")
                    else -> I18nStore.t("home.goalReachedShort", "¡Meta de hoy lograda!")
                },
                color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(if (newUser) 20.dp else 12.dp))
            if (!newUser) {
                GoalBarInline(todayXp = todayXp, goalXp = localGoalXp) { showGoalDialog = true }
                Spacer(Modifier.height(20.dp))
            }

            // ── HERO: la ÚNICA acción primaria ────────────────────────────────
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
                HeroContinueCard(
                    kicker = I18nStore.t("home.continueKicker", "CONTINUAR") +
                        (state.nextLesson?.lessonNumber?.let { " · " + I18nStore.t("home.unit", "UNIDAD").uppercase() + " $it" } ?: ""),
                    title = coach.message,
                    sampleL2 = coach.l2Name,
                    sampleRoman = null,
                    progressPct = coach.progressPct,
                    ctaText = coach.action.label,
                    onClick = { runCoachAction(coach.action) },
                )
            } else {
                HeroContinueCard(
                    kicker = if (newUser) I18nStore.t("home.startHereKicker", "EMPIEZA AQUÍ") else I18nStore.t("home.continueKicker", "CONTINUAR"),
                    title = if (newUser) I18nStore.t("home.firstLessonTitle", "Tu primera lección") else I18nStore.t("home.startHereTitle", "Empieza tu lección"),
                    sampleL2 = null,
                    sampleRoman = null,
                    progressPct = 0,
                    ctaText = if (newUser) I18nStore.t("home.firstLessonCta", "Empezar mi primera lección") else I18nStore.t("home.startHereCta", "Empezar"),
                    onClick = onSeeMap,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── PARA HOY ──────────────────────────────────────────────────────
            Text(
                I18nStore.t("home.today", "PARA HOY"),
                color = Color(0xFF6B746F), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(10.dp))
            if (newUser && dueCount == 0) {
                EmptyTodayCard()
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        TodayRow(
                            AmberBg, Icons.Default.Refresh, Amber,
                            I18nStore.t("home.reviewVocab", "Repasar vocabulario"), null,
                            if (dueCount > 0) I18nStore.t("home.reviewVocabDue", "{n} palabras listas para repasar").replace("{n}", "$dueCount")
                            else I18nStore.t("home.reviewSubtitle", "Refuerza lo que se te olvida (SRS)"),
                            if (dueCount > 0) "$dueCount" else null, true, onRepaso,
                        )
                        TodayRow(
                            MintBg, Icons.AutoMirrored.Filled.Chat, TealDark,
                            I18nStore.t("home.convHoot", "Conversación con Hoot"), null,
                            I18nStore.t("home.convHootSub", "Habla 2 min · corrección al instante"),
                            null, showHanzi, onConversation,
                        )
                        if (showHanzi) {
                            TodayRow(
                                MintBg, Icons.Default.Edit, TealDark,
                                I18nStore.t("home.writeChars", "Escribir caracteres"),
                                if (p?.l2 == "zh") "写汉字" else null,
                                I18nStore.t("home.writeCharsSub", "Trazos nuevos hoy"),
                                null, false, { onScript(p?.l2 ?: "") },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Misiones diarias (colapsables) ────────────────────────────────
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

            Spacer(Modifier.height(16.dp))

            // ── Pro (discreto) / Trial ────────────────────────────────────────
            if (p?.plan == null || p.plan == "free") {
                ProDiscreet(onUpgrade)
            } else if (p.plan == "trialing") {
                TrialBanner()
            }
        }
        PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun HeaderPill(bg: Color, icon: ImageVector, iconTint: Color, value: String, valueColor: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// Barra lineal de meta diaria (kit): track mint + relleno con gradiente teal.
@Composable
private fun GoalBarInline(todayXp: Int, goalXp: Int, onClick: () -> Unit) {
    val frac = if (goalXp > 0) (todayXp.toFloat() / goalXp).coerceIn(0f, 1f) else 0f
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(999.dp)).background(MintTrack)) {
            Box(
                Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(999.dp))
                    .background(Brush.horizontalGradient(listOf(Teal, Color(0xFF13B3A1)))),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("$todayXp/$goalXp XP", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TodayRow(
    iconBg: Color,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    titleL2: String?,
    subtitle: String,
    badge: String?,
    divider: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (!titleL2.isNullOrBlank()) Text(titleL2, color = Teal, fontSize = 14.sp)
                }
                Text(subtitle, color = Muted, fontSize = 13.sp)
            }
            if (badge != null) {
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(Amber).padding(horizontal = 7.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                    Text(badge, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Text("›", color = Color(0xFFB6BFBA), fontSize = 22.sp)
            }
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
    }
}

// Estado vacío de "PARA HOY" para usuario A0 (sin repaso aún).
@Composable
private fun EmptyTodayCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD6E0DC)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFBCC4BF), modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(8.dp))
            Text(I18nStore.t("home.noReviewTitle", "Aún no hay repaso"), color = Color(0xFF374039), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                I18nStore.t("home.noReviewBody", "Completa tu primera lección y tus palabras aparecerán aquí para repasar."),
                color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp,
            )
        }
    }
}

// Upsell Pro discreto (kit): tarjeta crema, sin competir con la acción primaria.
@Composable
private fun ProDiscreet(onUpgrade: () -> Unit) {
    Surface(
        onClick = onUpgrade,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFDF8EE),
        border = BorderStroke(1.dp, Color(0xFFEAE0CE)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Amber, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                I18nStore.t("home.proCta", "Fluenta Pro · lecciones ilimitadas"),
                color = Color(0xFF5A4413), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
            )
            Text(I18nStore.t("home.proTry", "Probar"), color = Teal, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/* ── Hero "Continuar" · Claude Design (P1 HOME) — la ÚNICA acción primaria ──────
 * Gradiente mint E4F6F1→CDEEE6, texto teal oscuro, progreso segmentado, botón 3D
 * primary 0E9D8E con relieve 0A6F64. Mascota Hoot arriba a la derecha.
 */
@Composable
private fun HeroContinueCard(
    kicker: String,
    title: String,
    sampleL2: String?,
    sampleRoman: String?,
    progressPct: Int,
    ctaText: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(HeroMintTop, HeroMintBottom)))
            .padding(18.dp),
    ) {
        Image(
            painter = painterResource(com.alturya.fluenta.R.drawable.ic_fluenta_hola),
            contentDescription = null,
            modifier = Modifier.size(64.dp).align(Alignment.TopEnd),
        )
        Column(Modifier.fillMaxWidth()) {
            Text(kicker, color = TealDark, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, color = HeroTitleInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 27.sp, modifier = Modifier.padding(end = 56.dp))
            if (!sampleL2.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sampleL2, color = Teal, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    if (!sampleRoman.isNullOrBlank()) Text(sampleRoman, color = Color(0xFF5D7A74), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            val total = 6
            val filled = ((progressPct / 100f) * total).roundToInt().coerceIn(0, total)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(total) { i ->
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(if (i < filled) Teal else HeroSegEmpty))
                    }
                }
                Text("$filled/$total", color = TealDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            val depth = 4.dp
            Box(Modifier.fillMaxWidth().height(50.dp + depth)) {
                Box(Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(16.dp)).background(TealDark))
                Box(
                    Modifier.fillMaxWidth().height(50.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(16.dp)).background(Teal).clickable(onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) { Text("▶  $ctaText", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

// Estado de error completo (kit): Hoot triste + badge cloud_off + reintentar/offline.
@Composable
private fun HomeErrorState(onRetry: () -> Unit, onOffline: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 34.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            HootMascot(modifier = Modifier.size(96.dp), sad = true)
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFFFE1DC)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.CloudOff, contentDescription = null, tint = Coral, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            I18nStore.t("home.errorTitle", "No pudimos cargar tu progreso"),
            color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            I18nStore.t("home.errorBody", "Revisa tu conexión a internet e inténtalo de nuevo. Tu racha está a salvo."),
            color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(22.dp))
        val depth = 4.dp
        Box(Modifier.fillMaxWidth().height(50.dp + depth)) {
            Box(Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(16.dp)).background(TealDark))
            Box(
                Modifier.fillMaxWidth().height(50.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(16.dp)).background(Teal).clickable(onClick = onRetry),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(I18nStore.t("common.retry", "Reintentar"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onOffline, modifier = Modifier.fillMaxWidth()) {
            Text(I18nStore.t("home.offline", "Seguir sin conexión"), color = TealDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GoalDialog(localGoalXp: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18nStore.t("goal.dialogTitle", "Tu meta diaria"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    I18nStore.t("goal.dialogSubtitle", "Elige cuánto quieres estudiar cada día:"),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                GoalStore.OPTIONS.forEach { opt ->
                    val selected = opt.xp == localGoalXp
                    Card(
                        onClick = { onPick(opt.xp) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(opt.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(I18nStore.t("goal.${opt.xp}.desc", opt.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(I18nStore.t("common.close", "Cerrar")) } },
    )
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
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    I18nStore.t("home.trialHint", "Disfruta conversación IA, escudo de racha y sin anuncios"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(20.dp))
        }
    }
}
