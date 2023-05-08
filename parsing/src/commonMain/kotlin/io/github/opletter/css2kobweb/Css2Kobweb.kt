package io.github.opletter.css2kobweb

fun css2kobweb(rawCSS: String): CssParseResult {
    val cssBySelector = parseCss(rawCSS).ifEmpty { return getProperties(rawCSS) }

    val styles = cssBySelector.keys.groupBy { it.substringBefore(":") }
    val parsedStyles = styles.map { (baseName, selectors) ->
        if (selectors.singleOrNull() == baseName) {
            ParsedComponentStyle(
                name = kebabToPascalCase(baseName.substringAfter(".")),
                modifiers = mapOf("base" to cssBySelector[selectors.first()]!!)
            )
        }
        val modifiers = selectors.associate { selector ->
            val properties = cssBySelector[selector]!!
            val cleanUpName = if (selector == baseName) "base" else kebabToCamelCase(selector.substringAfter(":"))
            cleanUpName to properties
        }
        ParsedComponentStyle(kebabToPascalCase(baseName.substringAfter(".")), modifiers)
    }
    return ParsedComponentStyles(parsedStyles)
}