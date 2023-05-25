package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.functions.transition

internal fun Map<String, ParsedProperty>.postProcessProperties(): List<ParsedProperty> {
    return replaceKeysIfEqual(setOf("width", "height"), "size")
        .replaceKeysIfEqual(setOf("minWidth", "minHeight"), "minSize")
        .replaceKeysIfEqual(setOf("maxWidth", "maxHeight"), "maxSize")
        .combineDirectionalModifiers("margin")
        .combineDirectionalModifiers("padding")
        .combineDirectionalModifiers("borderWidth") { "border${it}Width" }
        .combineTransitionModifiers()
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

private fun Map<String, ParsedProperty>.combineTransitionModifiers(): Map<String, ParsedProperty> {
    val transitionProperties = this["transitionProperty"]?.args ?: return this

    val propertyKeys = setOf(
        "transitionProperty",
        "transitionDuration",
        "transitionTimingFunction",
        "transitionDelay",
    )
    val propertyValues = propertyKeys.map { this[it]?.args }

    if (propertyValues.size < 2 || propertyValues.filterNotNull().any { it.size != transitionProperties.size })
        return this

    val combinedProperties = transitionProperties.indices.map { index ->
        Arg.Function.transition(
            property = transitionProperties[index],
            duration = propertyValues.getOrNull(1)?.getOrNull(index),
            remainingArgs = propertyValues.drop(2).mapNotNull { it?.getOrNull(index) }
        )
    }
    return (this - propertyKeys) + ("transition" to ParsedProperty("transition", combinedProperties))
}


private fun Map<String, ParsedProperty>.replaceKeysIfEqual(
    keysToReplace: Set<String>,
    newKey: String,
): Map<String, ParsedProperty> {
    val values = keysToReplace.mapNotNull { get(it)?.args }
    return if (values.size == keysToReplace.size && values.toSet().size == 1) {
        minus(keysToReplace) + (newKey to ParsedProperty(newKey, values.first()))
    } else this
}

private fun Map<String, ParsedProperty>.combineDirectionalModifiers(
    property: String,
    getPropertyByDirection: (direction: String) -> String = { "$property$it" },
): Map<String, ParsedProperty> {
    // consider whether this should be combined with the above replaceKeysIfEqual function
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
    }.ifEmpty { return this }

    val finalArgs = listOfNotNull(
        processedArgs["top"],
        processedArgs["topBottom"],
        processedArgs["leftRight"],
        processedArgs["right"],
        processedArgs["bottom"],
        processedArgs["left"],
        processedArgs["all"]?.value,
    )

    return minus(directions.map { getPropertyByDirection(it) }.toSet()) +
            (property to ParsedProperty(property, finalArgs))
}