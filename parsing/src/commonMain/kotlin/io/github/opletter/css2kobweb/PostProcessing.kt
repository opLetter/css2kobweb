package io.github.opletter.css2kobweb


internal fun Map<String, ParsedProperty>.postProcessProperties(): List<ParsedProperty> {
    return run {
        val width = this["width"]?.args
        val height = this["height"]?.args
        if (width != null && width == height) {
            filterKeys { it != "width" && it != "height" } + ("size" to ParsedProperty("size", width))
        } else this
    }.combineDirectionalModifiers("margin")
        .combineDirectionalModifiers("padding")
        .combineDirectionalModifiers("borderWidth") { "border${it}Width" }
        .values.map {
            if (it.name == "width" && it.args.single() == Arg.UnitNum.of("100%")) {
                ParsedProperty("fillMaxWidth")
            } else if (it.name == "height" && it.args.single() == Arg.UnitNum.of("100%")) {
                ParsedProperty("fillMaxHeight")
            } else if (it.name == "size" && it.args.single() == Arg.UnitNum.of("100%")) {
                ParsedProperty("fillMaxSize")
            } else it
        }
}

private fun Map<String, ParsedProperty>.combineDirectionalModifiers(
    property: String,
    getPropertyByDirection: (direction: String) -> String = { "$property$it" },
): Map<String, ParsedProperty> {
    fun MutableMap<String, Arg.NamedArg>.replaceIfEqual(keysToReplace: Set<String>, newKey: String) {
        val values = keysToReplace.mapNotNull { get(it)?.value }
        if (values.size == keysToReplace.size && values.toSet().size == 1) {
            keysToReplace.forEach { remove(it) }
            put(newKey, Arg.NamedArg(newKey, values.first()))
        }
    }

    val directions = setOf("Top", "Bottom", "Left", "Right") // capitalized for camelCase
    val processedArgs = buildMap {
        directions.forEach { direction ->
            this@combineDirectionalModifiers[getPropertyByDirection(direction)]
                ?.let { put(direction.lowercase(), Arg.NamedArg(direction.lowercase(), it.args.single())) }
        }
        replaceIfEqual(setOf("left", "right"), "leftRight")
        if ("leftRight" in this)
            replaceIfEqual(setOf("top", "bottom"), "topBottom")
        replaceIfEqual(setOf("leftRight", "topBottom"), "all")
    }

    val finalArgs = listOfNotNull(
        processedArgs["top"],
        processedArgs["topBottom"],
        processedArgs["leftRight"],
        processedArgs["right"],
        processedArgs["bottom"],
        processedArgs["left"],
        processedArgs["all"]?.value,
    ).filter { it.toString() != "0px" }.ifEmpty { return this }

    return filterKeys { key ->
        directions.none { key == getPropertyByDirection(it) }
    } + (property to ParsedProperty(property, finalArgs))
}