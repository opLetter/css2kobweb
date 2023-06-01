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
    return props.map { prop ->
        val (name, value) = prop.split(":", limit = 2).map { it.trim() } + "" // use empty if not present

        val parsedProperty = if (name.startsWith("-")) {
            val propertyArgs = listOf(name, value).map { Arg.Literal.withQuotesIfNecessary(it) }
            Arg.Function("styleModifier", lambdaStatements = listOf(Arg.Function("property", propertyArgs)))
        } else {
            parseValue(
                propertyName = kebabToCamelCase(name),
                value = value.lines().joinToString(" ") { it.trim() }.replace("  ", " "),
            )
        }

        parsedProperty.name to parsedProperty
    }.postProcessProperties().let { ParsedModifier(it) }
}