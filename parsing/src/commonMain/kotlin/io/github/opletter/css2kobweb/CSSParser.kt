package io.github.opletter.css2kobweb

// this function was partially created by ChatGPT
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
        val args = parseValue(
            propertyName = kebabToCamelCase(name),
            value = value.lines().joinToString("") { it.trim() },
        )
        val function = kebabToCamelCase(name)
        function to ParsedProperty(function, args)
    }.postProcessProperties().let { ParsedModifier(it) }
}