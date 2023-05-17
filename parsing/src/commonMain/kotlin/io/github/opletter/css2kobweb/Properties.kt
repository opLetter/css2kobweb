package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.constants.colors
import io.github.opletter.css2kobweb.functions.rgbOrNull
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
    val inQuotes: Boolean = false,
    val inParentheses: Boolean = false,
    val buffer: String = "",
    val result: List<String> = emptyList(),
)

// split on comma or space (or both?), but not in parens or quotes; credit: ChatGPT
internal tailrec fun splitString(input: String, state: ParseState = ParseState()): List<String> {
    if (input.isEmpty()) {
        return if (state.buffer.isNotEmpty()) state.result + state.buffer else state.result
    }
    val nextState = when (val ch = input.first()) {
        '"' -> state.copy(inQuotes = !state.inQuotes, buffer = state.buffer + ch)
        '(' -> state.copy(inParentheses = true, buffer = state.buffer + ch)
        ')' -> state.copy(inParentheses = false, buffer = state.buffer + ch)
        ' ', ',' -> {
            if (state.inQuotes || state.inParentheses) {
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

internal fun parseValue(propertyName: String, value: String): List<Arg> {
    if (propertyName == "transition") {
        val transitions = value.split(",", ", ").filter { it.isNotBlank() }
        return transitions.map { Arg.Function.transition(it) }
    }

    return splitString(value).map { prop ->
        if (prop == "0") {
            return@map Arg.UnitNum(0, "px")
        }

        val rawNum = prop.toIntOrNull() ?: prop.toDoubleOrNull()
        if (rawNum != null) {
            return@map Arg.RawNumber(rawNum)
        }

        if (prop.startsWith('#') || prop.startsWith("rgb")) {
            Arg.Function.rgbOrNull(prop)?.let { return@map it }
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

        val unit = Arg.UnitNum.ofOrNull(prop)
        if (unit != null) {
            return@map unit
        }

        if (prop.startsWith('"') || prop.startsWith("'")) {
            return@map Arg.Literal(prop.replace("'", "\""))
        }

        val color = colors.firstOrNull { it.lowercase() == prop }
        if (color != null) {
            return@map Arg.Property("Colors", color)
        }

        val className = when (propertyName) {
            "display" -> "DisplayStyle"
            else -> propertyName.replaceFirstChar { it.uppercase() }
        }

        if (prop.endsWith(")")) {
            return@map Arg.Function(
                "$className.${kebabToCamelCase(prop.substringBefore("("))}",
                parseValue(propertyName, prop.substringAfter("(").substringBeforeLast(")"))
            )
        }

        Arg.Property(className, kebabToPascalCase(prop))
    }
}
