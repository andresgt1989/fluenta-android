package com.alturya.fluenta.data

/**
 * In-memory snapshot of the signed-in user's language pair, set when the profile
 * loads (see HomeViewModel). Lets leaf screens — pronunciation, speak-repeat —
 * assess against the user's REAL target language instead of a hardcoded "en".
 * Falls back to "en" when unset (e.g. opened before the profile resolves).
 */
object Session {
    @Volatile var l1: String? = null
    @Volatile var l2: String? = null
}
