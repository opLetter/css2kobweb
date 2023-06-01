package io.github.opletter.css2kobweb

enum class CodeElement {
    Plain, Keyword, Property, ExtensionFun, String, Number, NamedArg
}

class CodeBlock(val text: String, val type: CodeElement) {
    override fun toString(): String = text
}

fun css2kobwebAsCode(rawCSS: String, extractOutCommonModifiers: Boolean = true): List<CodeBlock> {
    val result = css2kobweb(rawCSS, extractOutCommonModifiers)

    if (result is ParsedModifier) {
        return result.asCodeBlocks()
    }
    check(result is ParsedComponentStyles)

    val globalModifierCode = result.styles.flatMap { style ->
        style.modifiers.values.flatMap { it.filterModifiers<StyleModifier.Global>() }
    }.distinctBy { it.value }.flatMap { modifier ->
        listOf(
            CodeBlock("private val ", CodeElement.Keyword),
            CodeBlock("${modifier.value} = ", CodeElement.Plain),
        ) + modifier.modifier.asCodeBlocks() + CodeBlock("\n", CodeElement.Plain)
    }
    // we use a mutable list as otherwise this can become a performance bottleneck
    val stylesCode = result.styles.flatMap { it.asCodeBlocks() }.toMutableList().apply {
        var i = 0
        while (i < size - 1) {
            if (this[i].type == this[i + 1].type) {
                this[i] = CodeBlock(this[i].text + this[i + 1].text, CodeElement.Plain)
                removeAt(i + 1)
            } else {
                i++
            }
        }
    }
    return globalModifierCode + stylesCode
}

internal fun ParsedComponentStyle.asCodeBlocks(): List<CodeBlock> {
    val onlyBaseStyle = modifiers.size == 1 && modifiers.keys.first() == "base"
    val extra = if (onlyBaseStyle) ".base" else ""

    val styleText = listOf(
        CodeBlock("val ", CodeElement.Keyword),
        CodeBlock("${name}Style", CodeElement.Property),
        CodeBlock(" by ", CodeElement.Keyword),
        CodeBlock("ComponentStyle$extra {\n", CodeElement.Plain)
    )

    val localModifierCode = modifiers.values
        .flatMap { it.filterModifiers<StyleModifier.Local>() }
        .distinctBy { it.value }
        .flatMap { modifier ->
            val modifierText = modifier.modifier.asCodeBlocks(indentLevel = 1)
                .let { listOf(CodeBlock("Modifier", CodeElement.Plain)) + it.drop(1) }
            val selectorText = listOf(
                CodeBlock("\tval ", CodeElement.Keyword),
                CodeBlock("${modifier.value} = ", CodeElement.Plain),
            )
            selectorText + modifierText + CodeBlock("\n", CodeElement.Plain)
        }

    val modifierText = if (onlyBaseStyle) {
        modifiers["base"]!!.asCodeBlocks(indentLevel = 1) + CodeBlock("\n", CodeElement.Plain)
    } else {
        modifiers.flatMap { (selectorName, modifier) ->
            val selector = if (selectorName.startsWith("cssRule(")) {
                listOf(
                    CodeBlock("\tcssRule(", CodeElement.Plain),
                    CodeBlock(parenContents(selectorName), CodeElement.String),
                    CodeBlock(") {\n", CodeElement.Plain),
                )
            } else listOf(CodeBlock("\t$selectorName {\n", CodeElement.Plain))

            selector + modifier.asCodeBlocks(indentLevel = 2) + CodeBlock("\n\t}\n", CodeElement.Plain)
        }
    }
    return styleText + localModifierCode + modifierText + CodeBlock("}\n", CodeElement.Plain)
}

internal fun StyleModifier.asCodeBlocks(indentLevel: Int = 0): List<CodeBlock> {
    val indents = "\t".repeat(indentLevel)
    return when (this) {
        is StyleModifier.Global, is StyleModifier.Local -> listOf(CodeBlock(indents + value, CodeElement.Plain))
        is StyleModifier.Inline -> parsedModifier.asCodeBlocks(indentLevel)
        is StyleModifier.Composite -> {
            val (inlineModifiers, sharedModifiers) = modifiers.partition { it is StyleModifier.Inline }
            val start = sharedModifiers.firstOrNull()?.let { style ->
                indents + style.toString() + sharedModifiers.drop(1).joinToString("") { "\n\t$indents.then($it)" }
            } ?: "${indents}Modifier"
            val end = inlineModifiers.flatMap { style ->
                style.asCodeBlocks(indentLevel).let { if (style is StyleModifier.Inline) it.drop(1) else it }
            }
            listOf(CodeBlock(start, CodeElement.Plain)) + end
        }
    }
}

internal fun ParsedModifier.asCodeBlocks(indentLevel: Int = 0): List<CodeBlock> {
    val indents = "\t".repeat(indentLevel)
    val coloredModifiers = properties.flatMap {
        listOf(CodeBlock("\n\t$indents.", CodeElement.Plain)) +
                it.asCodeBlocks(indentLevel, functionType = CodeElement.ExtensionFun)
    }
    return listOf(CodeBlock("${indents}Modifier", CodeElement.Plain)) + coloredModifiers
}

internal fun Arg.asCodeBlocks(
    indentLevel: Int,
    functionType: CodeElement = CodeElement.Plain,
): List<CodeBlock> {
    return when (this) {
        is Arg.Literal -> listOf(CodeBlock(value, CodeElement.String))
        is Arg.Number -> listOf(CodeBlock(value, CodeElement.Number))
        is Arg.Property -> listOf(
            CodeBlock("$className.", CodeElement.Plain),
            CodeBlock(value, CodeElement.Property),
        )

        is Arg.UnitNum -> {
            val num = toString().substringBeforeLast(".")
            val numCode = if (num.first() == '(') {
                listOf(
                    CodeBlock("(-", CodeElement.Plain),
                    CodeBlock(num.drop(2).dropLast(1), CodeElement.Number),
                    CodeBlock(")", CodeElement.Plain),
                )
            } else listOf(CodeBlock(num, CodeElement.Number))

            numCode + listOf(
                CodeBlock(".", CodeElement.Plain),
                CodeBlock(type, CodeElement.Property),
            )
        }

        is Arg.Calc -> {
            arg1.arg.asCodeBlocks(indentLevel) + CodeBlock(" $operation ", CodeElement.Plain) +
                    arg2.arg.asCodeBlocks(indentLevel)
        }

        is Arg.NamedArg -> listOf(CodeBlock("$name = ", CodeElement.NamedArg)) + value.asCodeBlocks(indentLevel)

        is Arg.Function -> buildList {
            val indents = "\t".repeat(indentLevel + 1)
            add(CodeBlock(name, functionType))
            if (args.isNotEmpty() || lambdaStatements.isEmpty()) {
                val longArgs = args.toString().length > 100 // number chosen arbitrarily

                val separator = if (longArgs) ",\n\t$indents" else ", "
                val start = if (longArgs) "(\n\t$indents" else "("
                val end = if (longArgs) "\n$indents)" else ")"

                add(CodeBlock(start, CodeElement.Plain))
                addAll(args.flatMapIndexed { index, arg ->
                    val argBlocks = arg.asCodeBlocks(indentLevel + if (longArgs) 1 else 0)
                    if (index < args.size - 1) {
                        argBlocks + CodeBlock(separator, CodeElement.Plain)
                    } else argBlocks
                })
                add(CodeBlock(end, CodeElement.Plain))
            }
            if (lambdaStatements.isNotEmpty()) {
                add(CodeBlock(" {", CodeElement.Plain))
                val lambdaLines = lambdaStatements.flatMap {
                    listOf(CodeBlock("\n\t$indents", CodeElement.Plain)) + it.asCodeBlocks(indentLevel)
                }
                addAll(lambdaLines)
                add(CodeBlock("\n$indents}", CodeElement.Plain))
            }
        }
    }
}