package io.github.opletter.css2kobweb

/** Represents the different ways a modifier can be used inside a ComponentStyle */
sealed class StyleModifier(val value: String) {
    sealed class Normal(value: String) : StyleModifier(value)

    class Inline(val parsedModifier: ParsedModifier) : Normal(parsedModifier.toString())
    class Global(key: String, val modifier: ParsedModifier) : Normal(key)
    class Local(key: String, val modifier: ParsedModifier) : Normal(key)

    class Composite(val modifiers: List<Normal>) : StyleModifier(run {
        val (inlineModifiers, sharedModifiers) = modifiers.partition { it is Inline }
        val start = sharedModifiers.firstOrNull()?.let { first ->
            first.toString() + sharedModifiers.drop(1).joinToString("") { "\n\t.then($it)" }
        } ?: "Modifier"
        val end = inlineModifiers.joinToString("") { "\n" + it.toString().substringAfter("\n") }

        start + end
    })

    override fun toString(): String = value
    open operator fun plus(other: Normal): Composite {
        return when (this) {
            is Composite -> Composite(modifiers + other)
            is Normal -> Composite(listOf(this, other))
        }
    }
}

inline fun <reified T : StyleModifier.Normal> StyleModifier.filterModifiers(): List<T> {
    return when (this) {
        is StyleModifier.Composite -> modifiers.filterIsInstance<T>()
        is T -> listOf(this)
        else -> emptyList()
    }
}