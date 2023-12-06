package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.constants.cssRules

fun css2kobweb(rawCSS: String, extractOutCommonModifiers: Boolean = true): CssParseResult {
    val cleanedCss = inlineCssVariables(rawCSS)
        .lines()
        .filterNot { it.startsWith("@import") || it.startsWith("@charset") || it.startsWith("@namespace") }
        .joinToString("\n")
        .replace("/\\*[\\s\\S]*?\\*/".toRegex(), "") // remove comments
        .replace('\'', '"') // to simplify parsing

    val cssBySelector = parseCss(cleanedCss).ifEmpty {
        return if (":" in cleanedCss) {
            ParsedStyleBlock(getProperties(cleanedCss))
        } else {
            val parsedProperty = parseCssProperty("", cleanedCss)
            val singleArg = parsedProperty.args.singleOrNull()
            // Only return non-trivial parsed properties so that we don't show garbage during the initial input.
            // However, to show responsiveness, we do want to show some output ("Modifier") when the user start typing
            if (singleArg != null && (singleArg !is Arg.Property || singleArg.className != "")) {
                parsedProperty
            } else if (cleanedCss.trimEnd().last() == '{') {
                // display an empty ComponentStyle block if it looks like the css will have a selector
                ParsedComponentStyles(listOf(ParsedComponentStyle("", emptyMap())))
            } else {
                ParsedStyleBlock(emptyList())
            }
        }
    }

    val parsedModifiers = cssBySelector.filterIsInstance<ParsedStyleBlock>().run {
        // If there are only empty blocks, it's likely the user is still typing, so we show them
        // However if there are non-empty blocks, then we hide any empty blocks since they're not needed
        // Note that empty blocks may arise if the original block only contained css vars (which are inlined)
        filter { it.properties.isNotEmpty() }.ifEmpty { this }
    }

    val modifiersBySelector = parsedModifiers.flatMapIndexed { index, modifier ->
        val allSelectors = modifier.label.splitNotInParens(',')

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

    val styles = parsedModifiers.flatMap { it.label.splitNotInParens(',') }.groupBy { it.baseName() }
    val parsedStyles = styles.map { (baseName, selectors) ->
        val modifiers = selectors.associate { selector ->
            val cleanedUpName = if (selector == baseName) "base"
            else selector.substringAfter(baseName).let { cssRules[it] ?: "cssRule(\"$it\")" }

            cleanedUpName to modifiersBySelector[selector]!!
        }
        val styleName = kebabToPascalCase(baseName.substringAfter(".").substringAfter("#"))
            .replace("*", "All")
        ParsedComponentStyle(styleName, modifiers)
    }.let { ParsedComponentStyles(it) }

    val keyframes = cssBySelector.filterIsInstance<ParsedKeyframes>()

    return if (keyframes.isEmpty()) parsedStyles else ParsedBlocks(parsedStyles, keyframes)
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