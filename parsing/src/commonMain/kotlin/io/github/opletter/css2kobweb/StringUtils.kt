package io.github.opletter.css2kobweb

internal fun kebabToPascalCase(str: String): String {
    return str.split('-').joinToString("") { part ->
        part.replaceFirstChar { it.titlecase() }
    }
}

internal fun kebabToCamelCase(str: String): String = kebabToPascalCase(str).replaceFirstChar { it.lowercase() }

internal fun parenContents(str: String): String = str.substringAfter('(').substringBeforeLast(')').trim()

/**
 * Splits a string based on delimiters, except those present between sets of [groups] chars,
 * and except those between quotes.
 *
 * @param groups a set of char pairs, where the string will not be split between the first and second char of each pair
 */
internal fun String.splitNotBetween(
    groups: Set<Pair<Char, Char>>,
    splitOn: Set<Char>,
): List<String> = splitNotBetween(groups, splitOn, ParseState())

private data class ParseState(
    val quotesCount: Int = 0,
    val groupCounts: Map<Char, Int> = emptyMap(),
    val buffer: String = "",
    val result: List<String> = emptyList(),
)

/**
 * Splits a string based on delimiters, except those present between sets of [groups] chars,
 * and except those between quotes.
 *
 * @param groups a set of char pairs, where the string will not be split between the first and second char of each pair
 */
private tailrec fun String.splitNotBetween(
    groups: Set<Pair<Char, Char>>,
    splitOn: Set<Char>,
    state: ParseState,
): List<String> {
    if (isEmpty()) {
        return (state.result + state.buffer).filter { it.isNotBlank() }
    }
    val nextState = when (val ch = first()) {
        in groups.flatMap { listOf(it.first, it.second) } -> {
            val openChar = groups.first { ch == it.first || ch == it.second }.first
            val isGroupStart = openChar == ch
            val newGroupCount = (state.groupCounts[openChar] ?: 0) + if (isGroupStart) 1 else -1
            val restartBuffer = isGroupStart && newGroupCount == 1 && splitOn.contains(ch)

            state.copy(
                buffer = if (restartBuffer) "" else state.buffer + ch,
                groupCounts = state.groupCounts + (openChar to newGroupCount),
                result = if (restartBuffer) state.result + state.buffer else state.result
            )
        }

        '"' -> state.copy(buffer = state.buffer + ch, quotesCount = state.quotesCount + 1)
        in splitOn -> {
            if (state.groupCounts.values.any { it > 0 } || state.quotesCount % 2 == 1) {
                state.copy(buffer = state.buffer + ch)
            } else {
                state.copy(buffer = "", result = state.result + state.buffer)
            }
        }

        else -> state.copy(buffer = state.buffer + ch)
    }
    return drop(1).splitNotBetween(groups, splitOn, nextState)
}

internal fun String.splitNotInParens(split: Char): List<String> {
    return splitNotBetween(setOf('(' to ')'), splitOn = setOf(split))
        .map { it.trim() }.filter { it.isNotEmpty() }
}