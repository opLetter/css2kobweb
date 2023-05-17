package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.constants.colors
import kotlin.math.roundToInt

private fun rgbOrNull(prop: String): Arg.Function? {
    val nums = prop.substringAfter("(").substringBefore(")")
        .split(' ', ',', '/')
        .filter { it.isNotBlank() }

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

internal fun Arg.Companion.asColorOrNull(value: String): Arg? {
    if (value.startsWith("#")) {
        return Arg.Function("Color.rgb", listOf(Arg.Hex(value.drop(1))))
    }
    val color = colors.firstOrNull { it.lowercase() == value }
    if (color != null) {
        return Arg.Property("Colors", color)
    }
    if (value.startsWith("rgb") && value.endsWith(")")) {
        return rgbOrNull(value)
    }
    return null
}