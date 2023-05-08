package io.github.opletter.css2kobweb

// this function was partially created by ChatGPT
internal fun parseCss(css: String): Map<String, List<ParsedProperty>> {
    @Suppress("RegExpRedundantEscape") // redundancy needed for JS
    val regex = "([^{}]+)\\s*\\{\\s*([^{}]+)\\s*\\}".toRegex()
    val matches = regex.findAll(css)
    return matches.flatMap { matchResult ->
        val selector = matchResult.groupValues[1].trim()
        val properties = getProperties(matchResult.groupValues[2])

        selector.split(", ").map { it to properties }
    }.toMap()
}

internal fun getProperties(str: String): List<ParsedProperty> {
    val props = str.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    return props.map { prop ->
        val (name, value) = prop.split(":").map { it.trim() } + "" // use empty if not present
        val args = parseValue(propertyName = kebabToCamelCase(name), value = value)
        ParsedProperty(function = kebabToCamelCase(name), args = args)
    }.map {
        if (it.function == "width" && it.args.first().toString() == Arg.UnitNum(100, "percent").toString()) {
            ParsedProperty(function = "fillMaxWidth", args = emptyList())
        } else if (it.function == "height" && it.args.first().toString() == Arg.UnitNum(100, "percent").toString()) {
            ParsedProperty(function = "fillMaxHeight", args = emptyList())
        } else it
    }
}