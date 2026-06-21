package com.alturya.fluenta

import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.PluralCategory
import com.alturya.fluenta.data.PluralRules
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Red de seguridad del MOTOR i18n: reglas plurales CLDR + la API real `I18nStore.plural`.
 * Corre en JVM (sin emulador) y bloquea regresiones. Es el antídoto al plural ingenuo
 * (`if (n==1) "x" else "xs"`) que NO escala a 20 idiomas: aquí el call-site no ramifica
 * por idioma; la regla vive en un solo sitio y se verifica por familia lingüística.
 */
class PluralRulesTest {

    private fun cat(lang: String, n: Int) = PluralRules.select(lang, n)

    @Test fun romance_germanic_use_one_other() {
        for (lang in listOf("es", "en", "de", "it", "nl", "sv")) {
            assertEquals("$lang 0", PluralCategory.OTHER, cat(lang, 0))
            assertEquals("$lang 1", PluralCategory.ONE, cat(lang, 1))
            assertEquals("$lang 2", PluralCategory.OTHER, cat(lang, 2))
            assertEquals("$lang 100", PluralCategory.OTHER, cat(lang, 100))
        }
    }

    @Test fun french_portuguese_one_includes_zero() {
        for (lang in listOf("fr", "pt")) {
            assertEquals(PluralCategory.ONE, cat(lang, 0))   // 0 cuenta como "one"
            assertEquals(PluralCategory.ONE, cat(lang, 1))
            assertEquals(PluralCategory.OTHER, cat(lang, 2))
        }
    }

    @Test fun chinese_japanese_korean_have_only_other() {
        for (lang in listOf("zh", "ja", "ko", "vi", "th")) {
            assertEquals(PluralCategory.OTHER, cat(lang, 0))
            assertEquals(PluralCategory.OTHER, cat(lang, 1))
            assertEquals(PluralCategory.OTHER, cat(lang, 5))
        }
    }

    @Test fun russian_one_few_many() {
        assertEquals(PluralCategory.ONE, cat("ru", 1))
        assertEquals(PluralCategory.ONE, cat("ru", 21))   // mod10=1, mod100!=11
        assertEquals(PluralCategory.FEW, cat("ru", 2))
        assertEquals(PluralCategory.FEW, cat("ru", 24))
        assertEquals(PluralCategory.MANY, cat("ru", 5))
        assertEquals(PluralCategory.MANY, cat("ru", 11))   // teens → many
        assertEquals(PluralCategory.MANY, cat("ru", 111))
    }

    @Test fun polish_one_few_many() {
        assertEquals(PluralCategory.ONE, cat("pl", 1))
        assertEquals(PluralCategory.FEW, cat("pl", 2))
        assertEquals(PluralCategory.FEW, cat("pl", 22))
        assertEquals(PluralCategory.MANY, cat("pl", 5))
        assertEquals(PluralCategory.MANY, cat("pl", 12))   // teens → many
    }

    @Test fun arabic_uses_all_six_categories() {
        assertEquals(PluralCategory.ZERO, cat("ar", 0))
        assertEquals(PluralCategory.ONE, cat("ar", 1))
        assertEquals(PluralCategory.TWO, cat("ar", 2))
        assertEquals(PluralCategory.FEW, cat("ar", 3))
        assertEquals(PluralCategory.FEW, cat("ar", 10))
        assertEquals(PluralCategory.MANY, cat("ar", 11))
        assertEquals(PluralCategory.MANY, cat("ar", 99))
        assertEquals(PluralCategory.OTHER, cat("ar", 100))
    }

    @Test fun unknown_language_defaults_to_one_other_never_crashes() {
        assertEquals(PluralCategory.ONE, cat("xx", 1))
        assertEquals(PluralCategory.OTHER, cat("xx", 2))
        assertEquals(PluralCategory.ONE, cat("zz-Latn-XX", 1)) // tolera tags largos
    }

    // ── API real de producción (path de fallback sin backend cargado) ───────────────

    @Test fun plural_picks_correct_spanish_fallback_form() {
        // memCache vacío (cold start) → usa los fallbacks inline; {n} formateado por locale.
        assertEquals("Repasado 1 vez",
            I18nStore.plural("k.review", 1, one = "Repasado {n} vez", other = "Repasado {n} veces", lang = "es"))
        assertEquals("Repasado 2 veces",
            I18nStore.plural("k.review", 2, one = "Repasado {n} vez", other = "Repasado {n} veces", lang = "es"))
    }

    @Test fun plural_non_spanish_without_backend_falls_back_to_other() {
        // ru "few" no tiene forma propia inline → cae a 'other' (documentado), no crashea.
        assertEquals("2 veces",
            I18nStore.plural("k.x", 2, one = "{n} vez", other = "{n} veces", lang = "ru"))
    }

    @Test fun format_number_is_locale_aware() {
        assertEquals("1,000", I18nStore.formatNumber(1000, "en"))
        // es agrupa distinto que en (separador de miles) — la UI no debe hardcodear formato.
        assertEquals(I18nStore.formatNumber(1000, "es"), I18nStore.formatNumber(1000, "es"))
        org.junit.Assert.assertNotEquals(
            I18nStore.formatNumber(1000, "en"),
            I18nStore.formatNumber(1000, "es"))
    }
}
