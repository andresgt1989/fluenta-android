# Cómo añadir un idioma de interfaz a Fluenta

> **Prueba de fuego de la arquitectura:** añadir el idioma #15 debe ser **copiar un
> archivo y traducirlo + una línea en el registro**. Si exige programar lógica nueva,
> la arquitectura no está lista. Este documento es el contrato.

La interfaz (botones, títulos, errores, mensajes) está **separada del código**: ningún
texto va escrito fijo. Cada string sale de `I18nStore.t("clave", "fallback es")` o, si
lleva cantidad, de `I18nStore.plural(...)`. El idioma se resuelve en tiempo de ejecución
según el idioma de interfaz del usuario.

## Las dos piezas de un idioma

1. **El archivo de traducción** (backend): un mapa `clave → texto` por idioma, servido por
   `GET /api/i18n/ui?lang=xx`. La app lo cachea 24 h (`I18nStore`); el backend 30 d (Redis).
2. **Una entrada en el registro** de idiomas de interfaz (app): código + nombre nativo +
   bandera, para que aparezca en el selector de Ajustes.

## Pasos para añadir el idioma `xx`

1. **Backend** — añade el archivo `i18n/xx.json` copiando `es.json` (la fuente de verdad)
   y tradúcelo. Claves obligatorias mínimas (el resto cae al fallback es sin romperse):
   onboarding, pago/paywall, y todos los `*.error.*`.
2. **App** — añade una línea al registro `InterfaceLanguages.SUPPORTED`:
   ```kotlin
   InterfaceLang("xx", "Nombre nativo", "🏳️"),
   ```
3. **¿Plurales?** Si `xx` usa categorías distintas de `{one, other}` (árabe, ruso, polaco…),
   da las formas en el JSON con sufijo de categoría CLDR:
   `"progress.reviewedCount.one"`, `".few"`, `".many"`, `".other"`, etc. **No toques código:**
   `PluralRules` ya sabe qué categoría aplica a cada número en ese idioma. Si falta una forma,
   cae a `.other` y luego al fallback es.
4. **¿RTL** (árabe, hebreo, farsi, urdu)? Nada que programar: añade el código a `RTL_LANGS`
   en `I18nStore` (ya están ar/he/fa/ur) y la UI se refleja sola.
5. Listo. El selector en **Ajustes → Idioma de la interfaz** ya lo muestra.

## Lo que la arquitectura ya resuelve por ti (no reimplementar por idioma)

| Problema al escalar | Cómo está resuelto |
|---|---|
| **Plurales/género** | `I18nStore.plural(key, n, one, other)` + `PluralRules` (CLDR por familia: romance, eslavo, árabe, CJK…). El call-site nunca ramifica por idioma. |
| **Números/miles** | `I18nStore.formatNumber(n, lang)` (separador local: `1,000` en / `1.000` es). |
| **Falta una traducción** | Cae a `.other` → al fallback `es` inline. La pantalla **nunca** se rompe ni muestra la clave cruda. |
| **RTL** (árabe/hebreo) | `I18nStore.isRtl(lang)` aplica `LayoutDirection.Rtl` en el root. Base lista aunque el idioma no se active. |
| **Detección de idioma** | Al arrancar se lee `Locale.getDefault()`; el usuario lo puede cambiar a mano en Ajustes (persistido). |
| **Transliteración** (pinyin…) | Capa aparte del contenido (`replyTranslit`, `scriptTip`), no pegada al texto de interfaz. |
| **Texto que crece** (de/fi) | La UI usa contenedores flexibles; evitar anchos fijos y `maxLines` que trunquen. |

## Estrategia de llenado (economía a 20 idiomas)

1. **L1 reales del producto** (`es`, `pt`, `en`): 100 % revisados a mano.
2. **Resto en oleadas:** traducción asistida + **revisión nativa SOLO de lo crítico**
   (onboarding, pago, errores). No hace falta los 20 perfectos el día 1; lo no traducido
   cae al fallback sin romper.

## Regla de oro

Si añadir un idioma te obliga a abrir un archivo `.kt` que no sea el registro (un paso 2 de
una línea), **eso es un bug de arquitectura** — repórtalo, no lo parchees idioma por idioma.
