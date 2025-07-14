package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.constants.colors
import io.github.opletter.css2kobweb.parenContents
import kotlin.math.roundToInt

internal fun Arg.Companion.asColorOrNull(value: String): Arg? {
    if (value == "currentColor") {
        // Use JB's `Color` as Kobweb's `Colors` doesn't support currentColor since it's a "fake" color
        return Arg.Property("Color", "currentColor")
    }
    if (value.startsWith("#") && ' ' !in value.trim()) {
        val hexValue = if (value.length <= 5) {
            value.drop(1).toList().joinToString("") { "$it$it" } // #RGBA = #RRGGBBAA = 0xRRGGBBAA
        } else {
            value.drop(1)
        }
        val function = if (hexValue.length == 8) "rgba" else "rgb"
        return Arg.Function("Color.$function", Arg.Hex(hexValue))
    }
    val color = colors.firstOrNull { it.lowercase() == value }
    if (color != null) {
        return Arg.Property("Colors", color)
    }
    if (value.startsWith("rgb") && value.endsWith(")")) {
        return rgbOrNull(value)
    }
    if (value.startsWith("hsl") && value.endsWith(")")) {
        return hslOrNull(value)
    }
    return null
}

private fun rgbOrNull(prop: String): Arg.Function? {
    val nums = parenContents(prop).split(' ', ',', '/').filter { it.isNotBlank() }

    val params = nums.take(3).map {
        if (it.endsWith("%")) Arg.Float(it.dropLast(1).toFloat() / 100)
        else Arg.RawNumber(it.toDouble().roundToInt())
    }
    if (nums.size == 3) {
        return Arg.Function("Color.rgb", params)
    }
    if (nums.size == 4) {
        val alpha = nums.last().let {
            if (it.endsWith("%"))
                Arg.Float(it.dropLast(1).toFloat() / 100)
            else Arg.Float(it.toFloat())
        }
        return Arg.Function("Color.rgba", params.take(3) + alpha)
    }
    return null
}

private fun hslOrNull(prop: String): Arg.Function? {
    val nums = parenContents(prop).split(' ', ',', '/').filter { it.isNotBlank() }

    val params = nums.take(3).mapIndexed { index, s ->
        if (index == 0) {
            Arg.UnitNum.ofOrNull(s, "deg") ?: Arg.UnitNum.of(s + "deg")
        } else {
            Arg.UnitNum.ofOrNull(s, "percent") ?: Arg.Float(s.toFloat())
        }
    }
    if (nums.size == 3) {
        return Arg.Function("Color.hsl", params)
    }
    if (nums.size == 4) {
        val alpha = nums.last().let {
            Arg.UnitNum.ofOrNull(it, "percent") ?: Arg.Float(it.toFloat())
        }
        return Arg.Function("Color.hsla", params.take(3) + alpha)
    }
    return null
}