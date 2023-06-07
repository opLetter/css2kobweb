package io.github.opletter.css2kobweb

sealed interface CssParseResult {
    fun asCodeBlocks(indentLevel: Int = 0): List<CodeBlock>
}

class ParsedStyleBlock(val properties: List<ParsedProperty>, val label: String = "") : CssParseResult {
    // this purposely excludes [label] as that is just metadata used by the code itself
    override fun asCodeBlocks(indentLevel: Int): List<CodeBlock> {
        val indents = "\t".repeat(indentLevel)
        val coloredModifiers = properties.flatMap {
            listOf(CodeBlock("\n\t$indents.", CodeElement.Plain)) +
                    it.asCodeBlocks(indentLevel + 1, functionType = CodeElement.ExtensionFun)
        }
        return listOf(CodeBlock("${indents}Modifier", CodeElement.Plain)) + coloredModifiers
    }

    override fun toString(): String = asCodeBlocks().joinToString("") { it.text }
}

class ParsedKeyframes(private val name: String, val modifiers: List<ParsedStyleBlock>) : CssParseResult {
    override fun asCodeBlocks(indentLevel: Int): List<CodeBlock> {
        return buildList {
            add(CodeBlock("val ", CodeElement.Keyword))
            add(CodeBlock(kebabToPascalCase(name), CodeElement.Property))
            add(CodeBlock(" by ", CodeElement.Keyword))
            add(CodeBlock("Keyframes {\n", CodeElement.Plain))
            modifiers.forEach { block ->
                add(CodeBlock("\t", CodeElement.Plain))

                val labelParts = block.label.split(',').mapNotNull { Arg.UnitNum.ofOrNull(it.trim()) }
                if (block.label == "from" || block.label == "to") {
                    add(CodeBlock(block.label, CodeElement.Plain))
                } else if (labelParts.size == 1) {
                    addAll(labelParts.single().asCodeBlocks(1))
                } else {
                    addAll(Arg.Function("each", labelParts).asCodeBlocks(1))
                }

                add(CodeBlock(" {\n", CodeElement.Plain))
                addAll(block.asCodeBlocks(2))
                add(CodeBlock("\n\t}\n", CodeElement.Plain))
            }
            add(CodeBlock("}\n", CodeElement.Plain))
        }
    }

    override fun toString(): String = asCodeBlocks().joinToString("") { it.text }
}

class ParsedComponentStyles(private val styles: List<ParsedComponentStyle>) : CssParseResult {
    override fun asCodeBlocks(indentLevel: Int): List<CodeBlock> {
        val globalModifierCode = styles.flatMap { style ->
            style.modifiers.values.flatMap { it.filterModifiers<StyleModifier.Global>() }
        }.distinctBy { it.value }.flatMap { modifier ->
            listOf(
                CodeBlock("private val ", CodeElement.Keyword),
                CodeBlock("${modifier.value} = ", CodeElement.Plain),
            ) + modifier.modifier.asCodeBlocks() + CodeBlock("\n", CodeElement.Plain)
        }
        val stylesCode = styles.flatMap { it.asCodeBlocks() }
        return globalModifierCode + stylesCode
    }

    override fun toString(): String = asCodeBlocks().joinToString("") { it.text }
}

class ParsedBlocks(
    private val styles: ParsedComponentStyles,
    private val keyframes: List<ParsedKeyframes>,
) : CssParseResult {
    override fun asCodeBlocks(indentLevel: Int): List<CodeBlock> {
        return styles.asCodeBlocks() + keyframes.flatMap { it.asCodeBlocks() }
    }

    override fun toString(): String = asCodeBlocks().joinToString("") { it.text }
}

// convenient to reuse the same type for both
typealias ParsedProperty = Arg.Function

class ParsedComponentStyle(private val name: String, val modifiers: Map<String, StyleModifier>) {
    fun asCodeBlocks(): List<CodeBlock> {
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

    override fun toString(): String = asCodeBlocks().joinToString("") { it.text }
}