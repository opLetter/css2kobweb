package io.github.opletter.css2kobweb

internal fun kebabToPascalCase(str: String): String {
    return str.split('-').joinToString("") { prop ->
        prop.replaceFirstChar { it.titlecase() }
    }
}

internal fun kebabToCamelCase(str: String): String {
    return kebabToPascalCase(str).replaceFirstChar { it.lowercase() }
}

internal fun parenContents(str: String): String {
    return str.substringAfter('(').substringBeforeLast(')').trim()
}

internal data class ParseState(
    val quotesCount: Int = 0,
    val groupCount: Int = 0,
    val buffer: String = "",
    val result: List<String> = emptyList(),
)

/**
 * Splits a string based on delimiters, except those present between matching [openGroup] and [closeGroup] chars,
 * and except those between quotes.
 */
internal tailrec fun String.splitNotBetween(
    openGroup: Char,
    closeGroup: Char,
    splitOn: Set<Char>,
    state: ParseState = ParseState(),
): List<String> {
    if (isEmpty()) {
        return (state.result + state.buffer).filter { it.isNotBlank() }
    }
    val nextState = when (val ch = first()) {
        openGroup -> {
            if (state.groupCount == 0 && splitOn.contains(ch)) {
                state.copy(buffer = "", groupCount = 1, result = state.result + state.buffer)
            } else {
                state.copy(buffer = state.buffer + ch, groupCount = state.groupCount + 1)
            }
        }

        closeGroup -> state.copy(buffer = state.buffer + ch, groupCount = state.groupCount - 1)
        '"' -> state.copy(buffer = state.buffer + ch, quotesCount = state.quotesCount + 1)
        in splitOn -> {
            if (state.groupCount > 0 || state.quotesCount % 2 == 1) {
                state.copy(buffer = state.buffer + ch)
            } else {
                state.copy(buffer = "", result = state.result + state.buffer.trim())
            }
        }

        else -> state.copy(buffer = state.buffer + ch)
    }
    return drop(1).splitNotBetween(openGroup, closeGroup, splitOn, nextState)
}

internal fun String.splitNotInParens(split: Char): List<String> {
    return splitNotBetween('(', ')', splitOn = setOf(split))
        .map { it.trim() }.filter { it.isNotEmpty() }
}