package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.constants.intoShorthandLambdaProperty
import io.github.opletter.css2kobweb.functions.position
import io.github.opletter.css2kobweb.functions.transition

internal fun List<Pair<String, ParsedProperty>>.postProcessProperties(): List<ParsedProperty> {
    return map {
        val newProp = it.second.intoShorthandLambdaProperty()
        newProp.name to newProp
    }
        .combineLambdaModifiers()
        .replaceKeysIfEqual(setOf("width", "height"), "size")
        .replaceKeysIfEqual(setOf("minWidth", "minHeight"), "minSize")
        .replaceKeysIfEqual(setOf("maxWidth", "maxHeight"), "maxSize")
        .combineDirectionalModifiers("margin")
        .combineDirectionalModifiers("padding")
        .combineTransitionModifiers()
        .combineBackgroundPosition() // must be before combineBackgroundModifiers
        .combineBackgroundModifiers()
        .combineAnimationModifiers()
        .values.map { property ->
            val matchedFunction = setOf("width", "height", "size").find { it == property.name }
            if (matchedFunction != null && property.args.single() == Arg.UnitNum.of("100%"))
                ParsedProperty("fillMax${matchedFunction.replaceFirstChar(Char::uppercase)}")
            else property
        }
}

private fun List<Pair<String, ParsedProperty>>.combineLambdaModifiers(): Map<String, ParsedProperty> {
    return groupBy({ it.first }, { it.second }).mapValues { (name, properties) ->
        if (properties.all { it.args.isEmpty() }) {
            Arg.Function(name, lambdaStatements = properties.flatMap { it.lambdaStatements })
        } else { // should only be one but if not just take last
            properties.last()
        }
    }
}

private fun Map<String, ParsedProperty>.combineBackgroundPosition(): Map<String, ParsedProperty> {
    val keys = setOf("backgroundPositionX", "backgroundPositionY")
    val positions = keys.mapNotNull {
        val value = this[it] ?: return@mapNotNull null
        // one arg -> Arg.Literal -> see parseCssProperty()
        value.args.single().toString().splitNotInParens(',')
    }.ifEmpty { return this }

    val positionArgs = positions.first().indices.map { index ->
        val cssPosition = Arg.Function.position(positions.joinToString(" ") { it[index] })
        Arg.Function("BackgroundPosition.of", cssPosition)
    }
    val newProperty = ParsedProperty("backgroundPosition", positionArgs)
    return (this - keys) + (newProperty.name to newProperty)
}


private fun Map<String, ParsedProperty>.combineBackgroundModifiers(): Map<String, ParsedProperty> {
    fun String.getArgName() = this.substringAfter("background").substringBefore("Mode").lowercase()

    val existingBackground = this["background"]
    val existingBackgroundArgs = existingBackground?.args
        ?.dropWhile { !it.toString().startsWith("CSS") } // filter color arg
        ?.ifEmpty { null }
        // Un-reverse the initial parsing back to line up with order of other properties,
        // after which we will reverse again to match the order in kobweb.
        // Note that it's important to do parsing in declaration order, as if "background" specifies two images
        // but "backgroundImage" only specifies one, we want to use the latter's value for the first image
        ?.reversed()

    val propertyKeys = setOf(
        "backgroundImage",
        "backgroundRepeat",
        "backgroundSize",
        "backgroundPosition",
        "backgroundBlendMode",
        "backgroundOrigin",
        "backgroundClip",
        "backgroundAttachment",
    )
    val argNames = propertyKeys.map { it.getArgName() }

    val propertyValues = propertyKeys.mapNotNull { prop ->
        this[prop]?.let { prop to it.args }
    }.toMap()

    if (propertyValues.isEmpty() || (existingBackgroundArgs == null && propertyValues.values.all { it.size == 1 }))
        return this

    // According to the CSS spec, the number of layers is determined by the # of background-image values,
    // which can come from either the "backgroundImage" or "background"
    val layerIndices = propertyValues["backgroundImage"]?.indices ?: existingBackgroundArgs?.indices
    val backgroundProperties = layerIndices?.map { index ->
        val args = propertyValues.mapNotNull { (prop, args) ->
            val originalArg = args.getOrNull(index) ?: return@mapNotNull null
            val adjustedArg = if (prop == "backgroundImage" && originalArg is Arg.Function) {
                if (originalArg.name == "url")
                    Arg.Function("BackgroundImage.of", originalArg)
                else Arg.ExtensionCall(originalArg, Arg.Function("toImage"))
            } else originalArg

            Arg.NamedArg(prop.getArgName(), adjustedArg)
        }
        val existingArgs = (existingBackgroundArgs?.get(index) as Arg.Function?)?.args.orEmpty()
        val combinedArgs = (existingArgs + args).sortedBy { argNames.indexOf(it.toString().substringBefore(" ")) }

        Arg.Function("CSSBackground", combinedArgs)
    }.orEmpty().reversed()  // args order reversed as in kobweb

    val color = this["backgroundColor"]?.args
        ?: listOfNotNull(existingBackground?.args?.firstOrNull().takeIf { !it.toString().startsWith("CSS") })

    val newProperty = ParsedProperty("background", color + backgroundProperties)

    return (this - propertyKeys - "backgroundColor") + (newProperty.name to newProperty)
}

private fun Map<String, ParsedProperty>.combineAnimationModifiers(): Map<String, ParsedProperty> {
    fun String.getArgName() = this.substringAfter("animation").replaceFirstChar { it.lowercase() }

    val existingAnimation = this["animation"]
    val existingAnimationArgs = existingAnimation?.args?.ifEmpty { null }

    val propertyKeys = setOf(
        "animationName",
        "animationDuration",
        "animationTimingFunction",
        "animationDelay",
        "animationIterationCount",
        "animationDirection",
        "animationFillMode",
        "animationPlayState",
    )
    val argNames = propertyKeys.map { it.getArgName() }

    val propertyValues = propertyKeys.mapNotNull { prop ->
        this[prop]?.let { prop to it.args }
    }.toMap()

    // kobweb currently only supports specifying the whole animation as a single property, so we have to handle
    // all cases where one of these properties is individually specified in css. If this changes,
    // then this return condition can be expanded to match the one for [background]

    // IMPORTANT: we currently take advantage of the above fact by using combineAnimationModifiers
    // as the sole place where `CSSAnimation(...)` gets transformed into 'Name.toAnimation(...)'
    // If the `animation-...` properties are ever supported individually, this would need to be accounted for
    if (propertyValues.isEmpty() && existingAnimation == null)
        return this

    val animationProperties = (propertyValues.values.firstOrNull()?.indices ?: (0..0)).map { index ->
        val args = propertyValues.map { (prop, args) ->
            Arg.NamedArg(prop.getArgName(), args[index])
        }
        val existingArgs = (existingAnimationArgs?.get(index) as Arg.Function?)?.args.orEmpty()
        val combinedArgs = (existingArgs + args).sortedBy { argNames.indexOf(it.toString().substringBefore(" ")) }

        val (name, otherArgs) = combinedArgs.partition { it is Arg.NamedArg && it.name == "name" }

        name.singleOrNull()?.let { arg ->
            check(arg is Arg.NamedArg)
            Arg.ExtensionCall(
                Arg.Property.fromKebabValue(null, arg.value.toString().removeSurrounding("\"")),
                Arg.Function("toAnimation", listOf(Arg.Property(null, "colorMode")) + otherArgs)
            )
        } ?: Arg.Function("CSSAnimation", combinedArgs)
    }
    val newProperty = ParsedProperty("animation", animationProperties)

    return (this - propertyKeys) + (newProperty.name to newProperty)
}

private fun Map<String, ParsedProperty>.combineTransitionModifiers(): Map<String, ParsedProperty> {
    val transitionProperties = this["transitionProperty"]?.args
    // treat "transition" as "transitionProperty" if all it contains are properties
        ?: this["transition"]?.args?.mapNotNull { (it as? Arg.Function)?.args?.singleOrNull() }
        ?: return this

    val propertyKeys = setOf(
        "transitionProperty",
        "transitionDuration",
        "transitionTimingFunction",
        "transitionDelay",
    )
    val propertyValues = propertyKeys.map { this[it]?.args }

    val transitionGroup = transitionProperties.size > 1 &&
            propertyValues.drop(1).all { it == null || it.size == 1 }

    val combinedProperties = if (transitionGroup) {
        Arg.Function.transition(
            property = Arg.Function("setOf", transitionProperties),
            duration = propertyValues.getOrNull(1)?.getOrNull(0),
            remainingArgs = propertyValues.drop(2).mapNotNull { it?.getOrNull(0) }
        ).let { listOf(it) }
    } else {
        val otherProperties = propertyValues.drop(1).filterNotNull()
        if (otherProperties.isEmpty() || otherProperties.any { it.size != transitionProperties.size })
            return this

        transitionProperties.indices.map { index ->
            Arg.Function.transition(
                property = transitionProperties[index],
                duration = propertyValues[1]?.getOrNull(index),
                remainingArgs = propertyValues.drop(2).mapNotNull { it?.getOrNull(index) }
            )
        }
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

private fun Map<String, ParsedProperty>.combineDirectionalModifiers(property: String): Map<String, ParsedProperty> {
    // consider whether this should be combined with the above replaceKeysIfEqual function
    fun MutableMap<String, Arg.NamedArg>.replaceIfEqual(keysToReplace: Set<String>, newKey: String) {
        val values = keysToReplace.mapNotNull { get(it)?.value }
        if (values.size == keysToReplace.size && values.toSet().size == 1) {
            keysToReplace.forEach { remove(it) }
            put(newKey, Arg.NamedArg(newKey, values.first()))
        }
    }

    val directions = setOf("Top", "Right", "Bottom", "Left") // capitalized for camelCase
    val processedArgs = buildMap {
        directions.forEach { direction ->
            this@combineDirectionalModifiers[property + direction]
                ?.let { put(direction.lowercase(), Arg.NamedArg(direction.lowercase(), it.args.single())) }
        }
        replaceIfEqual(setOf("left", "right"), "leftRight")
        if ("leftRight" in this || ("left" !in this && "right" !in this))
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
        processedArgs["all"]?.value, // ignore name for "all"
    )

    return minus(directions.map { property + it }.toSet()) + (property to ParsedProperty(property, finalArgs))
}