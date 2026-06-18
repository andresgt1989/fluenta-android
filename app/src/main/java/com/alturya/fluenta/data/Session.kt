package com.alturya.fluenta.data

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory snapshot of the signed-in user's language pair, set when the profile
 * loads (see HomeViewModel). Lets leaf screens — pronunciation, speak-repeat —
 * assess against the user's REAL target language instead of a hardcoded "en".
 * Falls back to "en" when unset (e.g. opened before the profile resolves).
 */
object Session {
    @Volatile var l1: String? = null
    @Volatile var l2: String? = null

    /**
     * Bumped whenever the user changes their learning language (LanguagesViewModel).
     * HomeScreen / CurriculumMap collect this and re-fetch, so switching language
     * refreshes the whole app instead of leaving stale (e.g. Chinese) data on screen
     * until the next cold start.
     */
    val reloadSignal = MutableStateFlow(0)

    fun requestReload() {
        reloadSignal.value = reloadSignal.value + 1
    }
}
