package io.github.opletter.css2kobweb.constants

class ShorthandProperty(val property: String, val subProperties: List<String>)

val shorthandProperties = listOf(
    ShorthandProperty("border", listOf("Width", "Style", "Color")),
    ShorthandProperty("borderTop", listOf("Width", "Style", "Color")),
    ShorthandProperty("borderBottom", listOf("Width", "Style", "Color")),
    ShorthandProperty("borderRight", listOf("Width", "Style", "Color")),
    ShorthandProperty("borderLeft", listOf("Width", "Style", "Color")),
    ShorthandProperty("overflow", listOf("X", "Y")),
    ShorthandProperty("paddingInline", listOf("Start", "End")),
    ShorthandProperty("paddingBlock", listOf("Start", "End")),
    ShorthandProperty("padding", listOf("Top", "Right", "Bottom", "Left")),
    ShorthandProperty("margin", listOf("Top", "Right", "Bottom", "Left")),
    ShorthandProperty("font", listOf("Alternates", "Caps", "EastAsian", "Emoji", "Ligatures", "Numeric", "Settings")),
).flatMap { shortHand ->
    shortHand.subProperties.map { "${shortHand.property}$it" to it }
}.toMap()