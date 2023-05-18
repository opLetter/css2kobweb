package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.functions.asColorOrNull
import io.github.opletter.css2kobweb.functions.linearGradient
import io.github.opletter.css2kobweb.functions.transition

internal fun kebabToPascalCase(str: String): String {
    return str.split('-').joinToString("") { prop ->
        prop.replaceFirstChar { it.titlecase() }
    }
}

internal fun kebabToCamelCase(str: String): String {
    return kebabToPascalCase(str).replaceFirstChar { it.lowercase() }
}

internal data class ParseState(
    val quotesCount: Int = 0,
    val parensCount: Int = 0,
    val buffer: String = "",
    val result: List<String> = emptyList(),
)

// split on comma or space (or both?), but not in parens or quotes; credit: ChatGPT
internal tailrec fun splitString(input: String, state: ParseState = ParseState()): List<String> {
    if (input.isEmpty()) {
        return if (state.buffer.isNotEmpty()) state.result + state.buffer else state.result
    }
    val nextState = when (val ch = input.first()) {
        '"' -> state.copy(quotesCount = state.quotesCount + 1, buffer = state.buffer + ch)
        '(' -> state.copy(parensCount = state.parensCount + 1, buffer = state.buffer + ch)
        ')' -> state.copy(parensCount = state.parensCount - 1, buffer = state.buffer + ch)
        ' ', ',' -> {
            if (state.quotesCount % 2 == 1 || state.parensCount != 0) {
                state.copy(buffer = state.buffer + ch)
            } else {
                if (state.buffer.isNotEmpty()) {
                    state.copy(buffer = "", result = state.result + state.buffer)
                } else state
            }
        }

        else -> state.copy(buffer = state.buffer + ch)
    }
    return splitString(input.drop(1), nextState)
}

internal fun parseValue(propertyName: String, value: String): ParsedProperty {
    if (propertyName == "transition") {
        val transitions = value.split(",", ", ").filter { it.isNotBlank() }
        return ParsedProperty(propertyName, transitions.map { Arg.Function.transition(it) })
    }
    if (propertyName == "transform") {
        val statements = value.splitNotInParens(' ').map { func ->
            val args = splitString(func.substringAfter('(').substringBeforeLast(')')).map {
                if (it.toDoubleOrNull() == 0.0 && (it.startsWith("matrix") || it.startsWith("scale")))
                    Arg.RawNumber(0)
                else
                    Arg.UnitNum.ofOrNull(it) ?: Arg.RawNumber(it.toIntOrNull() ?: it.toDouble())
            }
            Arg.Function(func.substringBefore('('), args)
        }
        return ParsedProperty("transform", lambdaStatements = statements)
    }

    return splitString(value).map { prop ->
        val unit = Arg.UnitNum.ofOrNull(prop)
        if (unit != null) {
            val takeRawZero = setOf(
                "zIndex", "opacity", "lineHeight", "flexGrow", "flexShrink", "flex", "order", "tabIndex",
                "gridColumnEnd", "gridColumnStart", "gridRowEnd", "gridRowStart",
            )

            return@map if (unit.toString().substringBeforeLast('.') == "0" && propertyName in takeRawZero)
                Arg.RawNumber(0)
            else unit
        }

        val rawNum = prop.toIntOrNull() ?: prop.toDoubleOrNull()
        if (rawNum != null) {
            return@map Arg.RawNumber(rawNum)
        }

        Arg.asColorOrNull(prop)?.let { return@map it }

        if (prop.startsWith("linear-gradient(")) {
            return@map Arg.Function.linearGradient(prop.substringAfter("(").substringBeforeLast(")").trim())
        }

        if (prop.startsWith("calc(")) {
            val expr = prop.substringAfter("(").substringBeforeLast(")").trim()
            val indexOfOperator = expr.indexOfAny(charArrayOf('*', '/')).takeIf { it != -1 }
                ?: (expr.substringAfter(' ').indexOfAny(charArrayOf('+', '-')) + expr.indexOf(' ') + 1)

            val arg1 = expr.take(indexOfOperator).trim()
            val arg2 = expr.drop(indexOfOperator + 1).trim()
            return@map Arg.Calc(
                Arg.UnitNum.ofOrNull(arg1) ?: Arg.RawNumber(arg1.toIntOrNull() ?: arg1.toDouble()),
                Arg.UnitNum.ofOrNull(arg2) ?: Arg.RawNumber(arg2.toIntOrNull() ?: arg2.toDouble()),
                expr[indexOfOperator],
            )
        }

        if (prop.startsWith('"') || prop.startsWith("'")) {
            return@map Arg.Literal(prop.replace("'", "\""))
        }

        val className = when (propertyName) {
            "display" -> "DisplayStyle"

            "border", "borderStyle", "borderTop", "borderBottom", "borderLeft", "borderRight",
            "outline", "outlineStyle",
            -> "LineStyle"

            else -> propertyName.replaceFirstChar { it.uppercase() }
        }

        if (prop.endsWith(")")) {
            return@map Arg.Function(
                "$className.${kebabToCamelCase(prop.substringBefore("("))}",
                parseValue(propertyName, prop.substringAfter("(").substringBeforeLast(")")).args
            )
        }

        Arg.Property(className, kebabToPascalCase(prop))
    }.let { ParsedProperty(propertyName, it) }
}
