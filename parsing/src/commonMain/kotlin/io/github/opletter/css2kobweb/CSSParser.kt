package io.github.opletter.css2kobweb

internal fun parseCss(css: String): List<Pair<String, ParsedModifier>> {
    @Suppress("RegExpRedundantEscape") // redundancy needed for JS
    val regex = "([^{}]+)\\s*\\{\\s*([^{}]+)\\s*\\}".toRegex()
    val matches = regex.findAll(css)
    return matches.toList().map { matchResult ->
        val selector = matchResult.groupValues[1].trim()
        val properties = getProperties(matchResult.groupValues[2])
        selector to properties
    }
}

internal fun getProperties(str: String): ParsedModifier {
    val props = str.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    return props.associate { prop ->
        val (name, value) = prop.split(":").map { it.trim() } + "" // use empty if not present
        parseValue(
            propertyName = kebabToCamelCase(name),
            value = value.lines().joinToString("") { it.trim() },
        ).let { it.name to it }
    }.postProcessProperties().let { ParsedModifier(it) }
}