package io.github.opletter.css2kobweb

internal fun parseCss(css: String): List<Pair<String, ParsedModifier>> {
    return css.splitIntoCssBlocks().mapNotNull { (selector, properties) ->
        val subBlocks = properties.splitIntoCssBlocks()

        if (subBlocks.isEmpty()) {
            val parsedProperties = getProperties(properties)
            if (parsedProperties.properties.isEmpty()) return@mapNotNull null // may happen if all were css vars

            selector to parsedProperties
        } else null // TODO: handle nested blocks
    }
}

internal fun getProperties(str: String): ParsedModifier {
    val props = str.splitNotInParens(';').map { it.trim() }.filter { it.isNotEmpty() }
    return props.mapNotNull { prop ->
        val (name, value) = prop.split(":", limit = 2).map { it.trim() } + "" // use empty if not present

        if (name.startsWith("--")) return@mapNotNull null // ignore css variables

        val parsedProperty = if (name.startsWith("-")) {
            val propertyArgs = listOf(name, value).map { Arg.Literal.withQuotesIfNecessary(it) }
            Arg.Function("styleModifier", lambdaStatements = listOf(Arg.Function("property", propertyArgs)))
        } else {
            parseValue(
                propertyName = kebabToCamelCase(name),
                value = value
                    .replace("!important", "")
                    .lines()
                    .filterNot { it.startsWith("@import") || it.startsWith("@charset") || it.startsWith("@namespace") }
                    .joinToString(" ") { it.trim() }
                    .replace("  ", " "),
            )
        }

        parsedProperty.name to parsedProperty
    }.postProcessProperties().let { ParsedModifier(it) }
}

/**
 * Returns a list of pairs of the form (selector, block content).
 * Note that this only gets the first level of selectors, so nested selectors will be kept within their parent.
 */
private fun String.splitIntoCssBlocks(): List<Pair<String, String>> {
    return splitNotBetween('{', '}', setOf('{'))
        .filter { it.isNotBlank() }
        .fold(listOf<Pair<String, String>>()) { acc, str ->
            val prev = acc.lastOrNull() ?: ("" to "")
            val properties = str.substringBeforeLast("}").trim()
            val nextSelector = str.substringAfterLast("}").trim()

            acc.dropLast(1) + (prev.first to properties) + (nextSelector to "")
        }.drop(1).dropLast(1)
}