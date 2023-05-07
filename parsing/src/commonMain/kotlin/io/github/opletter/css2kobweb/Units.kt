package io.github.opletter.css2kobweb

internal val units = mapOf(
    "%" to "percent",
    "em" to "em",
    "ex" to "ex",
    "ch" to "ch",
    "ic" to "ic",
    "rem" to "cssRem",
    "lh" to "lh",
    "rlh" to "rlh",
    "vw" to "vw",
    "vh" to "vh",
    "vi" to "vi",
    "vb" to "vb",
    "vmin" to "vmin",
    "vmax" to "vmax",
    "cm" to "cm",
    "mm" to "mm",
    "Q" to "Q",
    "pt" to "pt",
    "pc" to "pc",
    "px" to "px",
    "deg" to "deg",
    "grad" to "grad",
    "rad" to "rad",
    "turn" to "turn",
    "s" to "s",
    "ms" to "ms",
    "Hz" to "Hz",
    "kHz" to "kHz",
    "dpi" to "dpi",
    "dpcm" to "dpcm",
    "dppx" to "dppx",
    "fr" to "fr",
    "number" to "number",
)

internal fun replaceUnits(str: String): String {
    return str.replace(Regex("""(\d+)(\w+)""")) {
        val (num, unit) = it.destructured
        val unitName = units[unit] ?: return@replace it.value
        "$num.$unitName"
    }
}