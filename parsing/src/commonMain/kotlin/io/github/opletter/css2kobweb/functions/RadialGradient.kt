package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.kebabToPascalCase
import io.github.opletter.css2kobweb.splitNotInParens

internal fun Arg.Function.Companion.radialGradient(value: String): Arg.Function {
    val parts = value.splitNotInParens(',')

    val shape = parts[0].substringBefore(" at ")
        // check for color value, keeping in mind that there may be a percentage value in the arg
        .takeIf { Arg.asColorOrNull(it.splitNotInParens(' ')[0]) == null }
        ?.let { shapeStr ->
            val shapeParts = shapeStr.splitNotInParens(' ')

            val shape = if (shapeParts[0] == "circle") "Circle" else "Ellipse"
            val shapeSize = (shapeParts - setOf("circle", "ellipse")).map {
                Arg.UnitNum.ofOrNull(it) ?: Arg.Property("RadialGradient.Extent", kebabToPascalCase(it))
            }

            if (shapeSize.isEmpty()) Arg.Property("RadialGradient.Shape", shape)
            else Arg.Function("RadialGradient.Shape.$shape", shapeSize)
        }

    val position = parts[0].substringAfter("at ", "").takeIf { it.isNotEmpty() }
        ?.let { Arg.Function.position(it) }

    val argsAsColors = parts.mapNotNull { Arg.asColorOrNull(it) }
    if (argsAsColors.size == 2) {
        return radialGradientOf(listOfNotNull(shape, argsAsColors[0], argsAsColors[1], position))
    }

    val mainArgs = listOfNotNull(shape, position)
    val lambdaFunctions = gradientColorStopList(parts.drop(if (mainArgs.isEmpty()) 0 else 1))

    return radialGradientOf(args = mainArgs, lambdaFunctions = lambdaFunctions)
}

private fun radialGradientOf(args: List<Arg>, lambdaFunctions: List<Arg.Function> = emptyList()): Arg.Function =
    Arg.Function("radialGradient", args, lambdaFunctions)