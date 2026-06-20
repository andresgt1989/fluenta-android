package com.alturya.fluenta.data

/**
 * CLDR plural categories. Not every language uses all of them: Spanish/English use
 * {one, other}; Arabic uses all six; Chinese/Japanese use only {other}.
 * See https://cldr.unicode.org/index/cldr-spec/plural-rules
 */
enum class PluralCategory(val tag: String) {
    ZERO("zero"), ONE("one"), TWO("two"), FEW("few"), MANY("many"), OTHER("other");

    companion object {
        fun fromTag(tag: String): PluralCategory? = entries.firstOrNull { it.tag == tag }
    }
}

/**
 * Selects the CLDR plural category for an integer count in a given interface language.
 *
 * Pure Kotlin (no `android.icu`) so it is unit-testable on the JVM AND works in the
 * cold-start / offline fallback path. Covers the plural FAMILIES of the languages we
 * realistically ship; an unknown language defaults to the English-like {one, other}
 * rule, so adding a language never crashes — at worst it gets a sane default until its
 * rule is added here. This is the "base prepared for 20 languages" the architecture needs:
 * the call-site never branches on the language, the rule lives in ONE place.
 */
object PluralRules {

    // Languages with NO plural distinction — a single "other" form (CJK, SE-Asian).
    private val NO_PLURAL = setOf("zh", "ja", "ko", "vi", "th", "id", "ms", "lo", "km", "my")

    // Slavic one/few/many family (Russian/Ukrainian style: mod-10 / mod-100).
    private val SLAVIC_RU = setOf("ru", "uk", "be")

    // West-Slavic one/few/many (Polish/Czech/Slovak style).
    private val SLAVIC_PL = setOf("pl", "cs", "sk")

    // French/Portuguese: "one" also covers 0 (i = 0..1).
    private val ONE_INCLUDES_ZERO = setOf("fr", "pt")

    /** Returns the CLDR category for [count] (absolute value) in [lang] (2-letter code). */
    fun select(lang: String, count: Int): PluralCategory {
        val l = lang.lowercase().take(2)
        val n = if (count < 0) -count else count
        return when {
            l in NO_PLURAL -> PluralCategory.OTHER

            l == "ar" -> arabic(n)
            l in SLAVIC_RU -> slavicRu(n)
            l in SLAVIC_PL -> slavicPl(n)
            l in ONE_INCLUDES_ZERO -> if (n == 0 || n == 1) PluralCategory.ONE else PluralCategory.OTHER

            // Default (en, es, de, nl, it, sv, da, no, fi, tr, el, hi, …): one iff n == 1.
            else -> if (n == 1) PluralCategory.ONE else PluralCategory.OTHER
        }
    }

    private fun arabic(n: Int): PluralCategory {
        val mod100 = n % 100
        return when {
            n == 0 -> PluralCategory.ZERO
            n == 1 -> PluralCategory.ONE
            n == 2 -> PluralCategory.TWO
            mod100 in 3..10 -> PluralCategory.FEW
            mod100 in 11..99 -> PluralCategory.MANY
            else -> PluralCategory.OTHER
        }
    }

    private fun slavicRu(n: Int): PluralCategory {
        val mod10 = n % 10
        val mod100 = n % 100
        return when {
            mod10 == 1 && mod100 != 11 -> PluralCategory.ONE
            mod10 in 2..4 && mod100 !in 12..14 -> PluralCategory.FEW
            else -> PluralCategory.MANY
        }
    }

    private fun slavicPl(n: Int): PluralCategory {
        val mod10 = n % 10
        val mod100 = n % 100
        return when {
            n == 1 -> PluralCategory.ONE
            mod10 in 2..4 && mod100 !in 12..14 -> PluralCategory.FEW
            else -> PluralCategory.MANY
        }
    }
}
