package com.alturya.fluenta.data

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.EventBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Funnel analytics, fire-and-forget. Never blocks the UI and never throws —
// a dropped event must never break a user flow. ApiClient.api attaches the auth
// token when present, so logged-in events get an identity server-side; guest
// events are stitched later via anonId.
object Analytics {
    // ── Sesión / ciclo de vida ────────────────────────────────────────────
    const val APP_OPEN = "app_open"
    // session_start se dispara en cada foreground (ProcessLifecycleOwner ON_START),
    // no solo en arranque frío. Distinto de APP_OPEN que es one-shot por proceso.
    const val SESSION_START = "session_start"

    // ── Onboarding / activación ───────────────────────────────────────────
    const val ONBOARDING_START = "onboarding_start"
    const val ONBOARDING_DONE = "onboarding_done"
    // onboarding_step registra el paso específico donde el usuario se detiene
    // prop: step = "welcome" | "l1" | "l2" | "motivation"
    const val ONBOARDING_STEP = "onboarding_step"

    // ── Auth ──────────────────────────────────────────────────────────────
    const val REGISTER_START = "register_start"
    const val REGISTER_SUCCESS = "register_success"

    // ── Diagnóstico de nivel ──────────────────────────────────────────────
    const val DIAGNOSTIC_START = "diagnostic_start"
    const val DIAGNOSTIC_COMPLETE = "diagnostic_complete" // prop: level

    // ── Wedge / conversación de activación ───────────────────────────────
    const val WEDGE_START = "wedge_start"
    const val WEDGE_FIRST_TURN = "wedge_first_turn" // ← activación

    // ── Lecciones ─────────────────────────────────────────────────────────
    const val LESSON_START = "lesson_start"
    const val LESSON_COMPLETE = "lesson_complete"
    const val LESSON_ABANDON = "lesson_abandon"   // salió SIN terminar (retención)

    // ── Unidad ────────────────────────────────────────────────────────────
    const val UNIT_COMPLETE = "unit_complete"

    // ── Conversación abierta ──────────────────────────────────────────────
    const val CONVERSATION_START = "conversation_start"

    // ── Gamificación ──────────────────────────────────────────────────────
    // streak_day se dispara cuando se actualiza la racha (retención)
    // prop: days = número de días de racha
    const val STREAK_DAY = "streak_day"

    // ── Viral / referidos ─────────────────────────────────────────────────
    // share se dispara cuando el usuario comparte su código/logro
    // prop: type = "referral" | "achievement"
    const val SHARE = "share"

    // ── Monetización ─────────────────────────────────────────────────────
    // paywall_view se dispara al mostrar la pantalla de pago
    // prop: from = pantalla de origen
    const val PAYWALL_VIEW = "paywall_view"

    // ── Navegación / UX ───────────────────────────────────────────────────
    // screen_view registra qué pantallas visita el usuario (time-on-screen y drop-off)
    // props: screen = ruta de la pantalla; ms = tiempo en pantalla al salir (opcional)
    const val SCREEN_VIEW = "screen_view"

    // button_tap registra interacciones con botones/CTAs (qué se toca y qué se ignora)
    // props: target = identificador del botón; screen = pantalla de origen (opcional)
    const val BUTTON_TAP = "button_tap"

    // drop_off marca el punto exacto donde el usuario abandona un flujo
    // props: screen = pantalla; step = paso/fase opcional
    const val DROP_OFF = "drop_off"

    // ── Cambio de idioma ──────────────────────────────────────────────────
    const val LANGUAGE_CHANGE = "language_change"

    // ── Experimentos A/B ──────────────────────────────────────────────────
    // experiment_exposure registra que el usuario fue expuesto a un experimento
    // props: experiment = clave, variant = "control" | "treatment"
    const val EXPERIMENT_EXPOSURE = "experiment_exposure"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun track(context: Context, event: String, props: Map<String, String>? = null) {
        scope.launch {
            try {
                val anonId = TokenStore.getOrCreateAnonId(context)
                ApiClient.api.postEvent(EventBody(event = event, anonId = anonId, props = props))
            } catch (_: Exception) {
                // intentionally swallowed: analytics must never affect the user
            }
        }
    }

    // Atajo para registrar un toque de botón/CTA (qué se usa y qué se ignora).
    fun tap(context: Context, target: String, screen: String? = null) {
        track(context, BUTTON_TAP, buildMap {
            put("target", target)
            if (screen != null) put("screen", screen)
        })
    }
}

// Composable helper — fires a screen_view event once per composition.
// Usage: TrackScreen(context, "Home") at the top of any screen composable.
@Composable
fun TrackScreen(context: Context, name: String) {
    LaunchedEffect(name) {
        Analytics.track(context, Analytics.SCREEN_VIEW, mapOf("screen" to name))
    }
}

// Tracker CENTRAL de tiempo por pantalla. Móntalo UNA vez por NavHost pasando la
// ruta activa. Cada vez que la ruta cambia (o el host sale de composición) emite
// un screen_view con `ms` = tiempo que el usuario estuvo en esa pantalla. Una
// fila por visita completada → potencia time-on-screen + detección de drop-off
// (la última pantalla vista antes de salir). Cobertura automática de TODA la app
// sin tener que instrumentar cada pantalla a mano.
@Composable
fun TrackNavTime(context: Context, route: String?) {
    val r = route ?: return
    DisposableEffect(r) {
        val startMs = SystemClock.elapsedRealtime()
        onDispose {
            val ms = SystemClock.elapsedRealtime() - startMs
            Analytics.track(
                context,
                Analytics.SCREEN_VIEW,
                mapOf("screen" to r, "ms" to ms.toString()),
            )
        }
    }
}
