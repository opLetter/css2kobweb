package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.splitNotInParens

internal fun Arg.Function.Companion.conicGradient(value: String): Arg.Function {
    val parts = value.splitNotInParens(',')
    val argsAsColors = parts.mapNotNull { Arg.asColorOrNull(it) }

    val angle = Arg.UnitNum.ofOrNull(parts[0].substringBefore(" at ").substringAfter("from "), zeroUnit = "deg")
    val position = parts[0].substringAfter("at ", "")
        .takeIf { it.isNotEmpty() }
        ?.let { Arg.Function.position(it) }

    if (argsAsColors.size == 2) {
        return conicGradientOf(listOfNotNull(argsAsColors[0], argsAsColors[1], angle, position))
    }

    val mainArgs = listOfNotNull(angle, position)
    val lambdaFunctions = gradientColorStopList(parts.drop(if (mainArgs.isEmpty()) 0 else 1))

    return conicGradientOf(args = mainArgs, lambdaFunctions = lambdaFunctions)
}

private fun conicGradientOf(args: List<Arg>, lambdaFunctions: List<Arg.Function> = emptyList()): Arg.Function =
    Arg.Function("conicGradient", args, lambdaFunctions)