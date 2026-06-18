package com.alturya.fluenta.data

import android.content.Context
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
    // Funnel event names — must match the backend whitelist in /api/events.
    const val APP_OPEN = "app_open"
    const val ONBOARDING_START = "onboarding_start"
    const val ONBOARDING_DONE = "onboarding_done"
    const val WEDGE_START = "wedge_start"
    const val WEDGE_FIRST_TURN = "wedge_first_turn" // ← activación
    const val REGISTER_START = "register_start"
    const val REGISTER_SUCCESS = "register_success"
    const val LESSON_START = "lesson_start"
    const val LESSON_COMPLETE = "lesson_complete"
    const val CONVERSATION_START = "conversation_start"
    const val LANGUAGE_CHANGE = "language_change"   // mide caídas en idioma equivocado

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
}
