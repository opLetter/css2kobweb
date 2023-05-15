package io.github.opletter.css2kobweb

fun css2kobweb(rawCSS: String, extractOutCommonModifiers: Boolean = true): CssParseResult {
    val cssBySelector = parseCss(rawCSS).ifEmpty { return getProperties(rawCSS) }

    val modifiersBySelector = cssBySelector.flatMapIndexed { index, (selectors, modifier) ->
        val allSelectors = selectors.splitByComma()

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

    val styles = cssBySelector.flatMap { it.first.splitByComma() }.groupBy { it.baseName() }
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

// ignores commas in parentheses
private fun String.splitByComma() = split(",(?![^()]*\\))".toRegex()).map { it.trim() }.filter { it.isNotBlank() }
private fun String.baseName() = substringBefore(":").substringBefore(" ")