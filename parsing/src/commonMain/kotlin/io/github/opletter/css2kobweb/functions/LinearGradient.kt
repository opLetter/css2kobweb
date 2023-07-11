package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.splitNotInParens

internal fun Arg.Function.Companion.linearGradient(value: String): Arg.Function {
    val parts = value.splitNotInParens(',')
    val argsAsColors = parts.mapNotNull { Arg.asColorOrNull(it) }
    val firstAsUnitNum = Arg.UnitNum.ofOrNull(parts[0], zeroUnit = "deg")
    // check for color value, keeping in mind that there may be a percentage value in the arg
    val firstHasColor = Arg.asColorOrNull(parts[0].splitNotInParens(' ').first()) != null

    val (x, y) = parts[0].split(' ').partition { it == "left" || it == "right" }
    val direction = Arg.Property(
        "LinearGradient.Direction",
        (y + x).joinToString("") { it.replaceFirstChar(Char::uppercase) },
    )

    if (parts.size == 2 && argsAsColors.size == 2) {
        return linearGradientOf(argsAsColors)
    }
    if (parts.size == 3 && argsAsColors.size == 2) {
        if (firstAsUnitNum != null) {
            return linearGradientOf(listOf(firstAsUnitNum) + argsAsColors)
        }
        if (!firstHasColor) {
            return linearGradientOf(listOf(direction) + argsAsColors)
        }
    }

    val mainArg = if (!firstHasColor) firstAsUnitNum ?: direction else null
    val lambdaFunctions = gradientColorStopList(parts.drop(if (mainArg == null) 0 else 1))

    return linearGradientOf(args = listOfNotNull(mainArg), lambdaFunctions = lambdaFunctions)
}

private fun linearGradientOf(args: List<Arg>, lambdaFunctions: List<Arg.Function> = emptyList()): Arg.Function =
    Arg.Function("linearGradient", args, lambdaFunctions)