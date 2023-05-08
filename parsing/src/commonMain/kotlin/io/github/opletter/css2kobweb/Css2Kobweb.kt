package io.github.opletter.css2kobweb

fun css2kobweb(rawCSS: String): CssParseResult {
    val cssBySelector = parseCss(rawCSS)

    if (cssBySelector.isEmpty()) {
        val props = getProperties(rawCSS)
        return ParsedModifier(props)
    }

    val styles = cssBySelector.keys.groupBy { it.substringBefore(":") }
    val parsedStyles = styles.map { (baseName, selectors) ->
        if (selectors.singleOrNull() == baseName) {
            ParsedComponentStyle(
                name = kebabToPascalCase(baseName.substringAfter(".")),
                modifiers = mapOf(
                    "base" to ParsedModifier(cssBySelector[selectors.first()]!!)
                )
            )
        }
        val modifiers = selectors.associate { selector ->
            val properties = cssBySelector[selector]!!
            val cleanUpName = if (selector == baseName) "base" else kebabToCamelCase(selector.substringAfter(":"))
            val modifier = ParsedModifier(properties)
            cleanUpName to modifier
        }
        ParsedComponentStyle(kebabToPascalCase(baseName.substringAfter(".")), modifiers)
    }
    return ParsedComponentStyles(parsedStyles)
}