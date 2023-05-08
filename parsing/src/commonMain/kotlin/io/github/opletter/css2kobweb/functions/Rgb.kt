package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg

internal fun Arg.Function.Companion.rgbOrNull(prop: String): Arg.Function? {
    if (prop.startsWith("#")) {
        return Arg.Function("Color.rgb", listOf(Arg.Hex(prop.drop(1))))
    }

    val nums = prop.substringAfter("(").substringBefore(")")
        .split(' ', ',', '/')
        .filter { it.isNotBlank() }

    val params = nums.take(3).map {
        Arg.UnitNum.ofOrNull(it) ?: Arg.RawNumber(it.toFloat())
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