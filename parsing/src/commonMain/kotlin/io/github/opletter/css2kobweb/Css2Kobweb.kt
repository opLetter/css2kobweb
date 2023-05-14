package io.github.opletter.css2kobweb

fun css2kobweb(rawCSS: String): CssParseResult {
    val cssBySelector = parseCss(rawCSS).ifEmpty { return getProperties(rawCSS) }.toMap()

    val styles = cssBySelector.keys.groupBy { it.substringBefore(":") }
    val parsedStyles = styles.map { (baseName, selectors) ->
        if (selectors.singleOrNull() == baseName) {
            ParsedComponentStyle(
                name = kebabToPascalCase(baseName.substringAfter(".")),
                modifiers = mapOf("base" to cssBySelector[selectors.first()]!!)
            )
        }
        val modifiers = selectors.associate { selector ->
            val cleanedUpName = if (selector == baseName) "base"
            else selector.substringAfter(baseName).let { cssRules[it] ?: "cssRule(\"$it\")" }

            cleanedUpName to cssBySelector[selector]!!
        }
        val styleName = kebabToPascalCase(baseName.substringAfter(".").substringAfter("#"))
            .replace("*", "All")
        ParsedComponentStyle(styleName, modifiers)
    }
    return ParsedComponentStyles(parsedStyles)
}