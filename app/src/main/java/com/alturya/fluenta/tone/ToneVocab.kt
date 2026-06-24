package com.alturya.fluenta.tone

/**
 * Vocab REAL es→zh para el entrenador de tono (tarea T2: "no solo el set starter").
 *
 * Sílabas HSK1 auténticas (carácter · pinyin · tono · significado en español),
 * balanceadas entre los 4 tonos para que el alumno discrimine bien. Es vocabulario
 * canónico (datos de referencia, no contenido generado), sílaba única por ítem
 * porque el objetivo es entrenar la PERCEPCIÓN del tono, no leer frases.
 */
object ToneVocab {

    /** Set HSK1 por tono — al menos 6 de cada uno para práctica equilibrada. */
    val HSK1: List<ToneWord> = listOf(
        // ── 1.er tono (alto plano) ──
        ToneWord("他", "ta", 1, "él"),
        ToneWord("她", "ta", 1, "ella"),
        ToneWord("天", "tian", 1, "día / cielo"),
        ToneWord("书", "shu", 1, "libro"),
        ToneWord("家", "jia", 1, "casa"),
        ToneWord("吃", "chi", 1, "comer"),
        ToneWord("听", "ting", 1, "escuchar"),
        ToneWord("三", "san", 1, "tres"),
        // ── 2.º tono (ascendente) ──
        ToneWord("人", "ren", 2, "persona"),
        ToneWord("茶", "cha", 2, "té"),
        ToneWord("钱", "qian", 2, "dinero"),
        ToneWord("来", "lai", 2, "venir"),
        ToneWord("名", "ming", 2, "nombre"),
        ToneWord("十", "shi", 2, "diez"),
        // ── 3.er tono (cae y sube) ──
        ToneWord("我", "wo", 3, "yo"),
        ToneWord("你", "ni", 3, "tú"),
        ToneWord("好", "hao", 3, "bueno"),
        ToneWord("水", "shui", 3, "agua"),
        ToneWord("小", "xiao", 3, "pequeño"),
        ToneWord("买", "mai", 3, "comprar"),
        ToneWord("五", "wu", 3, "cinco"),
        ToneWord("九", "jiu", 3, "nueve"),
        // ── 4.º tono (descendente) ──
        ToneWord("是", "shi", 4, "ser"),
        ToneWord("不", "bu", 4, "no"),
        ToneWord("大", "da", 4, "grande"),
        ToneWord("看", "kan", 4, "mirar"),
        ToneWord("去", "qu", 4, "ir"),
        ToneWord("爱", "ai", 4, "amar"),
        ToneWord("四", "si", 4, "cuatro"),
        ToneWord("二", "er", 4, "dos"),
    )

    /**
     * Selección equilibrada para una sesión: [perTone] sílabas de cada tono,
     * barajadas. Garantiza que cada sesión cubra los 4 tonos sin sesgo.
     */
    fun session(perTone: Int = 2, shuffle: Boolean = true): List<ToneWord> {
        val byTone = HSK1.groupBy { it.tone }
        val picked = (1..4).flatMap { t ->
            val pool = byTone[t].orEmpty()
            (if (shuffle) pool.shuffled() else pool).take(perTone)
        }
        return if (shuffle) picked.shuffled() else picked
    }
}
