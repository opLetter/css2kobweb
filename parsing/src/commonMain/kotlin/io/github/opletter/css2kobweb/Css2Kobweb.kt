package io.github.opletter.css2kobweb


fun css2kobweb(rawCSS: String): String {
    val cssBySelector = parseCss(rawCSS)

    if (cssBySelector.isEmpty()) {
        val props = getProperties(rawCSS)
        return getModifierFromParsed(props).trimMargin()
    }

    val styles = cssBySelector.keys.groupBy { it.substringBefore(":") }
    return styles.map { (baseName, selectors) ->
        if (selectors.singleOrNull() == baseName) {
            val x = getModifierFromParsed(cssBySelector[selectors.first()]!!)
            return@map """
                |val ${kebabToPascalCase(baseName.substringAfter("."))}Style by ComponentStyle.base {
                ${x.replace("|", "|\t")}
                |}
            """.trimMargin()
        }

        val modifiers = selectors.joinToString("\n|", prefix = "|") { selector ->
            val properties = cssBySelector[selector]!!
            val cleanUpName = if (selector == baseName) "base" else kebabToCamelCase(selector.substringAfter(":"))
            val modifier = getModifierFromParsed(properties).replace("|", "|\t")
            "$cleanUpName {\n$modifier\n|}"
        }
        """
            |val ${kebabToPascalCase(baseName.substringAfter("."))}Style by ComponentStyle {
            ${modifiers.replace("|", "|\t")}
            |}
        """.trimMargin()
    }.joinToString("\n")
}