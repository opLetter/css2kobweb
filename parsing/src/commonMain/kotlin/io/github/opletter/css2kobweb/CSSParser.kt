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
            value = value.replace("\n", "").replace("  ", ""),
        )
        kebabToCamelCase(name) to ParsedProperty(function = kebabToCamelCase(name), args = args)
    }.let { properties ->
        val width = properties["width"]?.args
        val height = properties["height"]?.args
        if (width != null && width.toString() == height.toString()) {
            properties.filterKeys { it != "width" && it != "height" } +
                    ("size" to ParsedProperty(function = "size", args = width))
        } else properties
    }.values.map {
        if (it.function == "width" && it.args.first().toString() == Arg.UnitNum(100, "percent").toString()) {
            ParsedProperty(function = "fillMaxWidth", args = emptyList())
        } else if (it.function == "height" && it.args.first().toString() == Arg.UnitNum(100, "percent").toString()) {
            ParsedProperty(function = "fillMaxHeight", args = emptyList())
        } else if (it.function == "size" && it.args.first().toString() == Arg.UnitNum(100, "percent").toString()) {
            ParsedProperty(function = "fillMaxSize", args = emptyList())
        } else it
    }.let { ParsedModifier(it) }
}