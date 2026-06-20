package com.alturya.fluenta.data

/** Un idioma de interfaz: código ISO-639 (2 letras), nombre EN SU PROPIO idioma, bandera. */
data class InterfaceLang(val code: String, val nativeName: String, val flag: String)

/**
 * Registro ÚNICO de idiomas de interfaz disponibles (el que alimenta el selector de Ajustes).
 *
 * Añadir un idioma = añadir UNA línea aquí + su `xx.json` en el backend. Sin tocar más código.
 * Ver `ADDING_A_LANGUAGE.md`. Hoy listamos los L1 reales del producto (revisados a mano);
 * el resto entra en oleadas. El nombre va en el idioma propio para que el usuario lo reconozca
 * aunque la app esté en un idioma que no entiende.
 */
object InterfaceLanguages {
    val SUPPORTED: List<InterfaceLang> = listOf(
        InterfaceLang("es", "Español", "🇪🇸"),
        InterfaceLang("en", "English", "🇬🇧"),
        InterfaceLang("pt", "Português", "🇧🇷"),
    )

    fun byCode(code: String): InterfaceLang? {
        val c = code.lowercase().take(2)
        return SUPPORTED.firstOrNull { it.code == c }
    }
}
