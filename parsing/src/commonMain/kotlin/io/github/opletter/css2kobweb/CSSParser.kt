package io.github.opletter.css2kobweb

internal fun parseCss(css: String): List<CssParseResult> {
    return css.splitIntoCssBlocks().mapNotNull { (selector, properties) ->
        val subBlocks = properties.splitIntoCssBlocks()

        if (subBlocks.isEmpty()) {
            ParsedStyleBlock(getProperties(properties), selector)
        } else if (selector.startsWith("@keyframes")) {
            val modifiers = subBlocks.map { (subSelector, subProperties) ->
                ParsedStyleBlock(getProperties(subProperties), subSelector)
            }
            ParsedKeyframes(selector.substringAfter("@keyframes").trim(), modifiers)
        } else null // TODO: handle @media and maybe some other nested blocks?
    }
}

internal fun getProperties(str: String): List<ParsedProperty> {
    return str.splitNotInParens(';').mapNotNull { prop ->
        val (name, value) = prop.split(':', limit = 2).map { it.trim() } + "" // use empty if not present

        if (name.startsWith("--")) return@mapNotNull null // ignore css variables

        fun argAsStringFallback(): Arg.Function {
            val propertyArgs = listOf(name, value).map { Arg.Literal.withQuotesIfNecessary(it) }
            return Arg.Function("styleModifier", lambdaStatements = listOf(Arg.Function("property", propertyArgs)))
        }

        val parsedProperty = if (name.startsWith('-')) {
            argAsStringFallback()
        } else try {
            parseCssProperty(
                propertyName = kebabToCamelCase(name),
                value = value
                    .replace("!important", "")
                    .lines()
                    .joinToString(" ") { it.trim() }
                    .replace("  ", " "),
            )
        } catch (_: Exception) {
            argAsStringFallback()
        }

        parsedProperty.name to parsedProperty
    }.postProcessProperties()
}

/**
 * Returns a list of pairs of the form (selector, block content).
 * Note that this only gets the first level of selectors, so nested selectors will be kept within their parent.
 */
private fun String.splitIntoCssBlocks(): List<Pair<String, String>> {
    return splitNotBetween(setOf('{' to '}'), setOf('{'))
        .filter { it.isNotBlank() }
        .fold(listOf<Pair<String, String>>()) { acc, str ->
            val prev = acc.lastOrNull() ?: ("" to "")
            val properties = str.substringBeforeLast("}").trim()
            val nextSelector = str.substringAfterLast("}").trim()

            acc.dropLast(1) + (prev.first to properties) + (nextSelector to "")
        }.drop(1).dropLast(1)
}