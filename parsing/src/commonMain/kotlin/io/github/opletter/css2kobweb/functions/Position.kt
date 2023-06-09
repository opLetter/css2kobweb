package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.kebabToPascalCase
import io.github.opletter.css2kobweb.splitNotInParens

internal fun Arg.Function.Companion.positionOrNull(value: String): Arg? {
    val position = value.splitNotInParens(' ')

    val xEdges = setOf("left", "right")
    val yEdges = setOf("top", "bottom")

    return when (position.size) {
        1 -> {
            val arg = position.single()
            Arg.UnitNum.ofOrNull(arg)?.let { Arg.Function("CSSPosition", it) }
                ?: Arg.Property("CSSPosition", kebabToPascalCase(arg))
        }

        2 -> {
            val units = position.mapNotNull { Arg.UnitNum.ofOrNull(it) }
            when (units.size) {
                0 -> {
                    val notCenter = position.filter { it != "center" }
                    when (notCenter.size) {
                        0 -> Arg.Property("CSSPosition", "Center")
                        1 -> Arg.Property("CSSPosition", kebabToPascalCase(notCenter.single()))
                        2 -> {
                            val (x, y) = notCenter.partition { it in xEdges }
                            // nulls will be filtered out in validation step
                            val propertyName = kebabToPascalCase("${y.singleOrNull()}-${x.singleOrNull()}")
                            Arg.Property("CSSPosition", propertyName)
                        }

                        else -> error("Unexpected notCenter size: ${notCenter.size}")
                    }
                }

                1 -> {
                    val centerIndex = position.indexOf("center")
                    val xEdge = xEdges.singleOrNull { it in position }
                    if (centerIndex != -1) {
                        val x = if (centerIndex == 0) edge("center-x") else units.single()
                        val y = if (centerIndex == 1) edge("center-y") else units.single()
                        Arg.Function("CSSPosition", listOf(x, y))
                    } else if (xEdge != null) {
                        val yFun = Arg.Function("Edge.Top", units)
                        Arg.Function("CSSPosition", listOf(edge(xEdge), yFun))
                    } else {
                        val yEdge = yEdges.singleOrNull { it in position } ?: return null
                        val xFun = Arg.Function("Edge.Left", units)
                        Arg.Function("CSSPosition", listOf(xFun, edge(yEdge)))
                    }
                }

                2 -> Arg.Function("CSSPosition", units)

                else -> error("Unexpected units size: ${units.size}")
            }
        }

        3 -> { // could also delegate to 4-value logic but this lets us generate nicer code
            val xIndex = position.indexOfFirst { it in xEdges }
                .let { if (it == -1) position.indexOf("center") else it }
            val yIndex = position.indexOfFirst { it in yEdges }
                .let { if (it == -1) position.indexOf("center") else it }

            val unitIndex = ((0..2) - setOf(xIndex, yIndex)).singleOrNull() ?: return null
            val unit = Arg.UnitNum.ofOrNull(position[unitIndex]) ?: return null

            val xArg = if (xIndex + 1 == unitIndex) edge(position[xIndex], unit)
            else edge(position[xIndex].let { if (it == "center") "center-x" else it })
            val yArg = if (yIndex + 1 == unitIndex) edge(position[yIndex], unit)
            else edge(position[yIndex].let { if (it == "center") "center-y" else it })

            Arg.Function("CSSPosition", listOf(xArg, yArg))
        }

        4 -> {
            val xIndex = if (position[0] in xEdges) 0 else 2
            val xUnit = Arg.UnitNum.ofOrNull(position[xIndex + 1]) ?: return null
            val yUnit = Arg.UnitNum.ofOrNull(position[3 - xIndex]) ?: return null

            Arg.Function("CSSPosition", listOf(edge(position[xIndex], xUnit), edge(position[2 - xIndex], yUnit)))
        }

        else -> null
    }?.takeIf {
        val validPositions = setOf(
            "Top", "TopRight", "Right", "BottomRight", "Bottom", "BottomLeft",
            "Left", "TopLeft", "Center"
        )
        !it.toString().startsWith("CSSPosition.")
                || it.toString().substringAfter(".") in validPositions
    }
}

// positionOrNull
internal fun Arg.Function.Companion.position(value: String): Arg =
    positionOrNull(value) ?: error("Invalid position: $value")

private fun edge(name: String) = Arg.Property("Edge", kebabToPascalCase(name))
private fun edge(name: String, unit: Arg.UnitNum) = Arg.Function("Edge.${kebabToPascalCase(name)}", unit)