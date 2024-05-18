package io.github.opletter.css2kobweb

/** Represents the different ways a modifier can be used inside a CssStyle */
sealed class StyleModifier(val value: String) {
    sealed class Normal(value: String) : StyleModifier(value)

    class Inline(val parsedModifier: ParsedStyleBlock) : Normal(parsedModifier.toString())
    class Global(key: String, val modifier: ParsedStyleBlock) : Normal(key)
    class Local(key: String, val modifier: ParsedStyleBlock) : Normal(key)

    class Composite(val modifiers: List<Normal>) : StyleModifier(run {
        val (inlineModifiers, sharedModifiers) = modifiers.partition { it is Inline }
        val start = sharedModifiers.firstOrNull()?.let { first ->
            first.toString() + sharedModifiers.drop(1).joinToString("") { "\n\t.then($it)" }
        } ?: "Modifier"
        val end = inlineModifiers.joinToString("") { "\n" + it.toString().substringAfter("\n") }

        start + end
    })

    override fun toString(): String = value

    operator fun plus(other: Normal): Composite {
        return when (this) {
            is Composite -> Composite(modifiers + other)
            is Normal -> Composite(listOf(this, other))
        }
    }

    fun asCodeBlocks(indentLevel: Int = 0): List<CodeBlock> {
        val indents = "\t".repeat(indentLevel)
        return when (this) {
            is Global, is Local -> listOf(CodeBlock(indents + value, CodeElement.Plain))
            is Inline -> parsedModifier.asCodeBlocks(indentLevel)
            is Composite -> {
                val (inlineModifiers, sharedModifiers) = modifiers.partition { it is Inline }
                val start = sharedModifiers.firstOrNull()?.let { style ->
                    indents + style.toString() +
                            sharedModifiers.drop(1).joinToString("") { "\n\t$indents.then($it)" }
                } ?: "${indents}Modifier"
                val end = inlineModifiers.flatMap { style ->
                    style.asCodeBlocks(indentLevel).let { if (style is Inline) it.drop(1) else it }
                }
                listOf(CodeBlock(start, CodeElement.Plain)) + end
            }
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