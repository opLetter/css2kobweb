package io.github.opletter.css2kobweb

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

internal fun parseValue(propertyName: String, value: String): List<Arg> {
    if (propertyName == "transition") {
        val transitions = value.split(",", ", ").filter { it.isNotBlank() }
        return transitions.map { Arg.Function.transition(it) }
    }

    return value.split("\\s*(?![^()]*\\))[, ]\\s*".toRegex())// split on comma or space (or both?), but not in parens
        .map { prop ->
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
