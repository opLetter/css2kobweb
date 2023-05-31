package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.splitNotInParens

internal fun gradientColorStopList(values: List<String>): List<Arg.Function> {
    return values.map { colorStopList ->
        val subParts = colorStopList.splitNotInParens(' ')
        val unitParts = subParts.mapNotNull { Arg.UnitNum.ofOrNull(it, zeroUnit = "percent") }

        if (subParts.size == 1 && unitParts.size == 1) Arg.Function("setMidpoint", unitParts)
        else Arg.Function("add", listOf(Arg.asColorOrNull(subParts[0])!!) + unitParts)
    }
}