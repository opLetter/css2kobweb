package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.kebabToPascalCase
import io.github.opletter.css2kobweb.splitNotInParens

internal fun Arg.Function.Companion.linearGradient(value: String): Arg.Function {
    val parts = value.splitNotInParens(',')
    val argsAsColors = parts.mapNotNull { Arg.asColorOrNull(it) }
    val firstAsUnitNum = Arg.UnitNum.ofOrNull(parts[0], zeroUnit = "deg")

    val (x, y) = parts[0].split(' ').partition { it == "left" || it == "right" }
    val direction = Arg.Property(
        "LinearGradient.Direction",
        (y + x).joinToString("") { kebabToPascalCase(it) },
    )

    if (parts.size == 2 && argsAsColors.size == 2) {
        return linearGradientOf(argsAsColors[0], argsAsColors[1])
    }
    if (parts.size == 3 && argsAsColors.size == 2) {
        if (firstAsUnitNum != null) {
            return linearGradientOf(firstAsUnitNum, argsAsColors[0], argsAsColors[1])
        }
        if (Arg.asColorOrNull(parts[0].substringBefore(' ')) == null) {
            return linearGradientOf(direction, argsAsColors[0], argsAsColors[1])
        }
    }

    val mainArg = if (Arg.asColorOrNull(parts[0].substringBefore(' ')) == null) {
        firstAsUnitNum ?: direction
    } else null

    val lambdaFunctions = gradientColorStopList(parts.drop(if (mainArg == null) 0 else 1))

    return linearGradientOf(
        args = listOfNotNull(mainArg).toTypedArray(),
        lambdaFunctions = lambdaFunctions,
    )
}

private fun linearGradientOf(vararg args: Arg, lambdaFunctions: List<Arg.Function> = emptyList()): Arg.Function =
    Arg.Function("linearGradient", args.toList(), lambdaFunctions)