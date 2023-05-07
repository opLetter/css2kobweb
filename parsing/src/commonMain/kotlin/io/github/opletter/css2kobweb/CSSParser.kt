package io.github.opletter.css2kobweb

// this function was created by ChatGPT
internal fun parseCss(css: String): Map<String, Map<String, String>> {
    @Suppress("RegExpRedundantEscape") // redundancy needed for JS
    val regex = "([^{}]+)\\s*\\{\\s*([^{}]+)\\s*\\}".toRegex()
    val matches = regex.findAll(css)
    return matches.flatMap { matchResult ->
        val selector = matchResult.groupValues[1].trim()
        val properties = getProperties(matchResult.groupValues[2])

        selector.split(", ").map { it to properties }
    }.toMap()
}

internal fun getProperties(str: String): Map<String, String> {
    val props = str.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    return props.associate { prop ->
        val (name, value) = prop.split(":").map { it.trim() } + "" // use empty if not present
        kebabToCamelCase(name) to value
    }.mapValues { (prop, v) ->
        parseValue(propertyName = prop, value = v)
    }.map { (k, v) ->
        if (k == "width" && v == "100.percent") {
            "fillMaxWidth" to ""
        } else if (k == "height" && v == "100.percent") {
            "fillMaxHeight" to ""
        } else k to v
    }.toMap()
}

internal fun getModifierFromParsed(properties: Map<String, String>): String {
    return "|Modifier\n|\t" + properties.map {
        (prop, value) -> ".$prop($value)"
    }.joinToString("\n|\t")
}