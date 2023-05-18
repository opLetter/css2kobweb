package io.github.opletter.css2kobweb

sealed interface CssParseResult

class ParsedModifier(val properties: List<ParsedProperty>) : CssParseResult {
    override fun toString(): String = "Modifier\n\t." + properties.joinToString("\n\t.")
}

class ParsedComponentStyles(val styles: List<ParsedComponentStyle>) : CssParseResult {
    override fun toString(): String {
        val globalModifiers = styles.flatMap { style ->
            style.modifiers.values.flatMap { it.filterModifiers<StyleModifier.Global>() }
        }.distinctBy { it.value }

        return globalModifiers.joinToString("") { "private val ${it.value} = ${it.modifier}\n" } +
                styles.joinToString("\n")
    }
}

// convenient to reuse the same type for both
typealias ParsedProperty = Arg.Function

class ParsedComponentStyle(val name: String, val modifiers: Map<String, StyleModifier>) {
    private fun toStringBase(): String {
        return """
            |val ${name}Style by ComponentStyle.base {
            |${"\t"}${modifiers["base"].toString().replace("\n", "\n\t")}
            |}
        """.trimMargin()
    }

    private fun toStringMultiple(): String {
        val modifiersStr = modifiers.toList().joinToString("\n|", prefix = "|") { (k, v) ->
            "$k {\n|\t${v.toString().replace("\n", "\n|\t")}\n|}"
        }
        val localModifiers = modifiers.values
            .flatMap { it.filterModifiers<StyleModifier.Local>() }
            .distinctBy { it.value }
            .joinToString("") { "\n\tval ${it.value} = ${it.modifier}" }
            .replace("\t.", "\t\t.")

        return """
            |val ${name}Style by ComponentStyle {$localModifiers
            ${modifiersStr.replace("|", "|\t")}
            |}
        """.trimMargin()
    }

    override fun toString(): String {
        return if (modifiers.keys.all { it == "base" }) toStringBase() else toStringMultiple()
    }
}
