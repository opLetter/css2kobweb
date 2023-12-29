package io.github.opletter.css2kobweb.constants

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.ParsedProperty

private class ShorthandProperty(val property: String, val subProperties: List<String>)

// TODO: In the future we should have an option for "strict" matching the original CSS (using the scope functions)
//  vs. a mode that uses named args instead. Note that even in strict mode, if all shorthands are specified, we
//  should probably still use named args if available.
private val shorthandProperties = listOf(
    ShorthandProperty("border", listOf("Width", "Style", "Color")),
    ShorthandProperty("borderTop", listOf("Width", "Style", "Color")),
    ShorthandProperty("borderBottom", listOf("Width", "Style", "Color")),
    ShorthandProperty("borderRight", listOf("Width", "Style", "Color")),
    ShorthandProperty("borderLeft", listOf("Width", "Style", "Color")),
    ShorthandProperty("overflow", listOf("X", "Y")),
    ShorthandProperty("paddingInline", listOf("Start", "End")),
    ShorthandProperty("paddingBlock", listOf("Start", "End")),
    // Currently don't use scope for these as it's usually unnecessary, and instead we can provide smart reduced
    // named properties (like turning equal "top" and "bottom" into "topBottom").
    // These should be re-enabled after the to-do above is addressed.
//    ShorthandProperty("padding", listOf("Top", "Right", "Bottom", "Left")),
//    ShorthandProperty("margin", listOf("Top", "Right", "Bottom", "Left")),
    ShorthandProperty("font", listOf("Alternates", "Caps", "EastAsian", "Emoji", "Ligatures", "Numeric", "Settings")),
).flatMap { shortHand ->
    shortHand.subProperties.map { "${shortHand.property}$it" to it }
}.toMap()

fun ParsedProperty.intoShorthandLambdaProperty(): ParsedProperty {
    return shorthandProperties[name]?.let { shorthandFun ->
        ParsedProperty(
            name.substringBefore(shorthandFun),
            lambdaStatements = listOf(
                Arg.Function(shorthandFun.replaceFirstChar { it.lowercase() }, args, lambdaStatements)
            )
        )
    } ?: this
}