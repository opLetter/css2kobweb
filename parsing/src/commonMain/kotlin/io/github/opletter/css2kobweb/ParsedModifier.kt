package io.github.opletter.css2kobweb

sealed interface CssParseResult

class ParsedModifier(val properties: List<ParsedProperty>) : CssParseResult {
    override fun toString(): String = "Modifier\n\t." + properties.joinToString("\n\t.")
}

class ParsedComponentStyles(val styles: List<ParsedComponentStyle>) : CssParseResult {
    override fun toString(): String = styles.joinToString("\n")
}

class ParsedProperty(val function: String, val args: List<Arg>) {
    override fun toString(): String = "$function(${args.joinToString(", ")})"
}

class ParsedComponentStyle(val name: String, val modifiers: Map<String, ParsedModifier>) {
    private fun toStringBase(): String {
        return """
            |val ${name}Style by ComponentStyle.base {
            |${"\t"}${modifiers["base"].toString().replace("\n", "\n\t")}
            |}
        """.trimMargin()
    }

    private fun toStringMultiple(): String {
        val modifiers = modifiers.toList().joinToString("\n|", prefix = "|") { (k, v) ->
            "$k {\n|\t${v.toString().replace("\n", "\n|\t")}\n|}"
        }
        return """
            |val ${name}Style by ComponentStyle {
            ${modifiers.replace("|", "|\t")}
            |}
        """.trimMargin()
    }

    override fun toString(): String {
        return if (modifiers.keys.all { it == "base" }) toStringBase() else toStringMultiple()
    }
}
