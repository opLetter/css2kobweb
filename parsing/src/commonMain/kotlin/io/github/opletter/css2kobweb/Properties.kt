package io.github.opletter.css2kobweb

internal fun kebabToPascalCase(str: String): String {
    return str.split('-').joinToString("") { prop ->
        prop.replaceFirstChar { it.titlecase() }
    }
}

internal fun kebabToCamelCase(str: String): String {
    return kebabToPascalCase(str).replaceFirstChar { it.lowercase() }
}

internal fun parseValue(propertyName: String, value: String): String {
    if (propertyName == "transition") {
        val transitions = value.split(",", ", ").filter { it.isNotBlank() }

        return transitions.joinToString(", ") { transition ->
            val params = replaceUnits(transition).split(" ").filter { it.isNotBlank() }
            val start = "CSSTransition(\"${params[0]}\", ${params[1]}"
            when (params.size) {
                2 -> "$start)"
                4 -> "$start, TransitionTimingFunction.${kebabToPascalCase(params[2])}, ${params[3]})"
                3 -> {
                    if ("." !in params[2]) {
                        "$start, TransitionTimingFunction.${kebabToPascalCase(params[2])})"
                    } else {
                        "$start, delay = ${params[2]})"
                    }
                }

                else -> error("Invalid transition: $transition")
            }
        }
    }

    return value.split("\\s*(?![^()]*\\))[, ]\\s*".toRegex())// split on comma or space (or both?), but not in parens
        .map { prop ->
            if (prop == "0") {
                return@map "0.px"
            }

            val rawNum = prop.toIntOrNull() ?: prop.toDoubleOrNull()
            if (rawNum != null) {
                return@map rawNum.toString()
            }

            if (prop.startsWith('#')) {
                return@map "Color.rgb(0x${prop.drop(1)})"
            }

            if (prop.startsWith("rgb")) {
                val nums = prop.substringAfter("(").substringBefore(")")
                    .split(' ', ',', '/')
                    .filter { it.isNotBlank() }
                if (nums.size == 3) {
                    val params = nums.joinToString(", ") { it.replace("%", ".percent") }
                    return@map "Color.rgb($params)"
                }
                if (nums.size == 4) {
                    val params = nums.take(3).joinToString(", ") { it.replace("%", ".percent") }
                    val alpha = nums.last().let {
                        if (it.endsWith("%")) it.dropLast(1).toFloat() / 100 else it
                    }
                    return@map "Color.rgba($params, ${alpha}f)"
                }
            }

            val potentialUnit = prop.dropWhile { it.isDigit() || it == '.' }
            val unit = units[potentialUnit]
            if (unit != null) {
                val num = prop.dropLast(potentialUnit.length)
                return@map "$num.$unit"
            }

            if (prop.startsWith('"') || prop.startsWith("'")) {
                return@map prop.replace("'", "\"")
            }

            val color = colors.firstOrNull { it.lowercase() == prop }
            if (color != null) {
                return@map "Colors.$color"
            }

            val className = when (propertyName) {
                "display" -> "DisplayStyle"
                else -> propertyName.replaceFirstChar { it.uppercase() }
            }

            className + "." + kebabToPascalCase(prop)
        }.joinToString(", ")
}
