package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.constants.cssRules

fun css2kobweb(rawCSS: String, extractOutCommonModifiers: Boolean = true): CssParseResult {
    val cleanedCss = inlineCssVariables(rawCSS)
        .replace("/\\*.*?\\*/".toRegex(), "")
        .replace('\'', '"')
    val cssBySelector = parseCss(cleanedCss).ifEmpty { return getProperties(cleanedCss) }

    val modifiersBySelector = cssBySelector.flatMapIndexed { index, (selectors, modifier) ->
        val allSelectors = selectors.splitNotInParens(',')

        allSelectors.associateWith { _ ->
            if (extractOutCommonModifiers && allSelectors.distinctBy { it.baseName() }.size != 1) {
                StyleModifier.Global("sharedModifier$index", modifier)
            } else if (extractOutCommonModifiers && allSelectors.size != 1) {
                StyleModifier.Local("sharedModifier$index", modifier)
            } else {
                StyleModifier.Inline(modifier)
            }
        }.toList()
    }.sortedBy { it.first }.fold(emptyList<Pair<String, StyleModifier>>()) { acc, (selector, modifier) ->
        val prev = acc.lastOrNull()
        if (prev?.first == selector) {
            acc.dropLast(1) + (selector to (prev.second + modifier))
        } else {
            acc + (selector to modifier)
        }
    }.toMap()

    val styles = cssBySelector.flatMap { it.first.splitNotInParens(',') }.groupBy { it.baseName() }
    val parsedStyles = styles.map { (baseName, selectors) ->
        val modifiers = selectors.associate { selector ->
            val cleanedUpName = if (selector == baseName) "base"
            else selector.substringAfter(baseName).let { cssRules[it] ?: "cssRule(\"$it\")" }

            cleanedUpName to modifiersBySelector[selector]!!
        }
        val styleName = kebabToPascalCase(baseName.substringAfter(".").substringAfter("#"))
            .replace("*", "All")
        ParsedComponentStyle(styleName, modifiers)
    }
    return ParsedComponentStyles(parsedStyles)
}

private fun inlineCssVariables(css: String): String {
    val cssVarPattern = Regex("--([\\w-]+):\\s*([^;]+);")
    var newCss = css

    // Extract and replace CSS variables in reverse order so that nested variables are replaced first
    cssVarPattern.findAll(css).toList().reversed().forEach { matchResult ->
        val varName = matchResult.groupValues[1].trim()
        val varValue = matchResult.groupValues[2].trim()
        newCss = newCss.replace("var(--$varName)", varValue)
    }
    return newCss
}

private fun String.baseName() = substringBefore(":").substringBefore(" ")

// ignores commas in parentheses
fun String.splitNotInParens(char: Char): List<String> {
    val result = fold(mutableListOf(StringBuilder()) to 0) { (list, depth), c ->
        val newDepth = when (c) {
            '(' -> depth + 1
            ')' -> depth - 1
            else -> depth
        }
        if (c == char && depth == 0) {
            list.add(StringBuilder())
        } else {
            list.last().append(c)
        }
        list to newDepth
    }
    return result.first.map { it.toString().trim() }.filter { it.isNotEmpty() }
}