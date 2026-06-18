package com.alturya.fluenta.util

import com.alturya.fluenta.data.I18nStore

// The backend stores `level` as a CEFR value (a1..c1) for every pair, plus a
// `levelSystem` (cefr | hsk | jlpt | topik). For non-CEFR verticals we map the
// CEFR position onto the target framework's ladder so learners see the label
// their language actually uses (e.g. a Japanese learner sees "JLPT N4", not "A2").
//
// Authoritative ladders (from backend prompts/tutor.ts):
//   CEFR : A1 A2 B1 B2 C1
//   JLPT : N5 N4 N3 N2 N1      (N5 = beginner, N1 = advanced)
//   HSK  : HSK 1 .. HSK 6
//   TOPIK: 1 .. 6              (1-2 beginner, 3-4 intermediate, 5-6 advanced)

private val CEFR_ORDER = listOf("a1", "a2", "b1", "b2", "c1")

private val JLPT_LADDER = listOf("N5", "N4", "N3", "N2", "N1")
private val HSK_LADDER = listOf("HSK 1", "HSK 2", "HSK 3", "HSK 4", "HSK 5")
private val TOPIK_LADDER = listOf("1", "2", "3", "4", "5")

fun levelLabel(level: String?, levelSystem: String?): String {
    val idx = CEFR_ORDER.indexOf((level ?: "").lowercase()).let { if (it < 0) 2 else it }
    return when ((levelSystem ?: "cefr").lowercase()) {
        "jlpt" -> "JLPT ${JLPT_LADDER[idx]}"
        "hsk" -> HSK_LADDER[idx]
        "topik" -> "TOPIK ${TOPIK_LADDER[idx]}"
        else -> (level ?: "—").uppercase()
    }
}

fun levelSystemName(levelSystem: String?): String = when ((levelSystem ?: "cefr").lowercase()) {
    "jlpt" -> "JLPT"
    "hsk" -> "HSK"
    "topik" -> "TOPIK"
    else -> "CEFR"
}

// Position of the user's level on the 5-band ladder (0..4); -1 if unknown/null
// (→ Home shows the "discover your level" nudge instead of the journey).
fun levelIndex(level: String?): Int = CEFR_ORDER.indexOf((level ?: "").lowercase())

// Short band labels for the Home level-journey pills (no system prefix — the
// "goal" label carries the framework name).
fun levelLadderShort(levelSystem: String?): List<String> = when ((levelSystem ?: "cefr").lowercase()) {
    "jlpt" -> JLPT_LADDER
    "hsk" -> listOf("1", "2", "3", "4", "5")
    "topik" -> TOPIK_LADDER
    else -> CEFR_ORDER.map { it.uppercase() }
}

// Localized via I18nStore (a Korean user sees "영어" for English). Falls back to the
// Spanish name (or the uppercased code) when the backend strings aren't loaded.
fun langName(code: String?): String {
    val c = (code ?: "").lowercase()
    return I18nStore.t("langName.$c", langNameFallback(c))
}

private fun langNameFallback(c: String): String = when (c) {
    "es" -> "Español"; "en" -> "Inglés"; "pt" -> "Portugués"; "fr" -> "Francés"
    "it" -> "Italiano"; "de" -> "Alemán"; "de-ch" -> "Alemán (Suiza)"; "nl" -> "Neerlandés"
    "ru" -> "Ruso"; "uk" -> "Ucraniano"; "pl" -> "Polaco"; "el" -> "Griego"
    "sv" -> "Sueco"; "da" -> "Danés"; "fi" -> "Finés"; "et" -> "Estonio"; "lt" -> "Lituano"
    "ar" -> "Árabe"; "zh" -> "Chino"; "ja" -> "Japonés"; "ko" -> "Coreano"
    "fa" -> "Persa"; "hi" -> "Hindi"; "id" -> "Indonesio"; "th" -> "Tailandés"
    "tr" -> "Turco"; "vi" -> "Vietnamita"; "he" -> "Hebreo"; "ur" -> "Urdu"
    else -> c.uppercase()
}

// Languages that use a right-to-left script. Used to set text direction in
// conversation bubbles, repaso cards, and anywhere L2 text is displayed.
fun isRtl(code: String?): Boolean = when ((code ?: "").lowercase()) {
    "ar", "he", "fa", "ur" -> true
    else -> false
}

fun flag(code: String?): String = when ((code ?: "").lowercase()) {
    "es" -> "🇪🇸"; "en" -> "🇬🇧"; "pt" -> "🇧🇷"
    "fr" -> "🇫🇷"; "it" -> "🇮🇹"; "de" -> "🇩🇪"
    "de-ch" -> "🇨🇭"; "nl" -> "🇳🇱"; "ru" -> "🇷🇺"
    "uk" -> "🇺🇦"; "pl" -> "🇵🇱"; "el" -> "🇬🇷"
    "sv" -> "🇸🇪"; "da" -> "🇩🇰"; "fi" -> "🇫🇮"
    "et" -> "🇪🇪"; "lt" -> "🇱🇹"; "ar" -> "🇸🇦"
    "zh" -> "🇨🇳"; "ja" -> "🇯🇵"; "ko" -> "🇰🇷"
    "fa" -> "🇮🇷"; "hi" -> "🇮🇳"; "id" -> "🇮🇩"; "th" -> "🇹🇭"
    "tr" -> "🇹🇷"; "vi" -> "🇻🇳"; "he" -> "🇮🇱"; "ur" -> "🇵🇰"
    else -> "🌐"
}
