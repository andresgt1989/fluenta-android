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

    /** Set HSK2 — sílabas únicas reales, 2 por tono. */
    val HSK2: List<ToneWord> = listOf(
        ToneWord("杯", "bei", 1, "vaso"),
        ToneWord("鸡", "ji", 1, "pollo"),
        ToneWord("鱼", "yu", 2, "pez"),
        ToneWord("牛", "niu", 2, "vaca"),
        ToneWord("走", "zou", 3, "caminar"),
        ToneWord("早", "zao", 3, "temprano"),
        ToneWord("快", "kuai", 4, "rápido"),
        ToneWord("累", "lei", 4, "cansado"),
    )

    /** Set HSK3 — sílabas únicas reales, 2 por tono. */
    val HSK3: List<ToneWord> = listOf(
        ToneWord("关", "guan", 1, "cerrar"),
        ToneWord("包", "bao", 1, "bolsa"),
        ToneWord("甜", "tian", 2, "dulce"),
        ToneWord("难", "nan", 2, "difícil"),
        ToneWord("短", "duan", 3, "corto"),
        ToneWord("北", "bei", 3, "norte"),
        ToneWord("饿", "e", 4, "hambriento"),
        ToneWord("旧", "jiu", 4, "viejo"),
    )

    /**
     * Set HSK6 «god-level» — caracteres únicos de registro ELEVADO (书面语) que distinguen
     * al hablante avanzado, balanceados por tono. Lleva el diferenciador del tono al léxico
     * culto que el alumno C1/C2 encuentra en el contenido es→zh (coordinación con T3).
     */
    val HSK6: List<ToneWord> = listOf(
        ToneWord("兼", "jian", 1, "simultanear"),
        ToneWord("枢", "shu", 1, "eje, pivote"),
        ToneWord("巍", "wei", 1, "imponente"),
        ToneWord("凝", "ning", 2, "concentrar, cuajar"),
        ToneWord("谋", "mou", 2, "idear, estrategia"),
        ToneWord("魂", "hun", 2, "alma"),
        ToneWord("谨", "jin", 3, "prudente"),
        ToneWord("朴", "pu", 3, "sencillo, sobrio"),
        ToneWord("舔", "tian", 3, "lamer"),
        ToneWord("弊", "bi", 4, "mal, abuso"),
        ToneWord("赋", "fu", 4, "dotar, conferir"),
        ToneWord("誉", "yu", 4, "honor, prestigio"),
    )

    /** Vocab por nivel HSK (clave = 1,2,3,6). El 6 = god-level. */
    val byLevel: Map<Int, List<ToneWord>> = mapOf(1 to HSK1, 2 to HSK2, 3 to HSK3, 6 to HSK6)

    /** Todo el vocab disponible, de-duplicado por (hanzi+pinyin+tono). */
    val all: List<ToneWord> = byLevel.values.flatten().distinctBy { "${it.hanzi}|${it.pinyin}|${it.tone}" }

    /**
     * Selección equilibrada para una sesión: [perTone] sílabas de cada tono, barajadas.
     * Garantiza que cada sesión cubra los 4 tonos sin sesgo. [level] = null → todos los
     * niveles disponibles; o un nivel HSK concreto (1..3).
     */
    fun session(perTone: Int = 2, shuffle: Boolean = true, level: Int? = null): List<ToneWord> {
        val pool0 = if (level != null) byLevel[level].orEmpty() else all
        val byTone = pool0.groupBy { it.tone }
        val picked = (1..4).flatMap { t ->
            val pool = byTone[t].orEmpty()
            (if (shuffle) pool.shuffled() else pool).take(perTone)
        }
        return if (shuffle) picked.shuffled() else picked
    }
}
