package com.alturya.fluenta.util

/**
 * Autónimos: el nombre de cada idioma EN SU PROPIO idioma (中文, 日本語, Français…).
 *
 * El kit de diseño muestra el patrón bilingüe "autónimo · nombre localizado"
 * (p.ej. «中文 · Chino», «Español / Spanish»). [langName] ya da el nombre
 * localizado al idioma del usuario; este helper aporta la otra mitad sin tocar
 * el backend. Si no hay autónimo conocido, cae al nombre localizado.
 */
fun autonym(code: String?): String = when ((code ?: "").lowercase()) {
    "es" -> "Español"; "en" -> "English"; "pt" -> "Português"
    "fr" -> "Français"; "it" -> "Italiano"; "de" -> "Deutsch"; "de-ch" -> "Schweizerdeutsch"
    "nl" -> "Nederlands"; "ru" -> "Русский"; "uk" -> "Українська"; "pl" -> "Polski"
    "el" -> "Ελληνικά"; "sv" -> "Svenska"; "da" -> "Dansk"; "fi" -> "Suomi"
    "et" -> "Eesti"; "lt" -> "Lietuvių"; "ar" -> "العربية"; "zh" -> "中文"
    "ja" -> "日本語"; "ko" -> "한국어"; "fa" -> "فارسی"; "hi" -> "हिन्दी"
    "id" -> "Bahasa Indonesia"; "th" -> "ไทย"; "tr" -> "Türkçe"; "vi" -> "Tiếng Việt"
    "he" -> "עברית"; "ur" -> "اردو"
    else -> langName(code)
}
