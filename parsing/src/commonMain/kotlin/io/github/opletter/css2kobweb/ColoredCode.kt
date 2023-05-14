package io.github.opletter.css2kobweb

enum class CodeElement {
    Plain, Keyword, Property, Function, String, Number, NamedArg
}

class CodeBlock(val text: String, val type: CodeElement) {
    override fun toString(): String = text
}

fun css2kobwebAsCode(rawCSS: String): List<CodeBlock> {
    val result = css2kobweb(rawCSS)

    if (result is ParsedModifier) {
        return result.asCodeBlocks()
    }
    check(result is ParsedComponentStyles)

    return result.styles.flatMap { style ->
        val onlyBaseStyle = style.modifiers.size == 1 && style.modifiers.keys.first() == "base"
        val extra = if (onlyBaseStyle) ".base" else ""

        val styleText = listOf(
            CodeBlock("val ", CodeElement.Keyword),
            CodeBlock("${style.name}Style", CodeElement.Property),
            CodeBlock(" by ", CodeElement.Keyword),
            CodeBlock("ComponentStyle$extra {\n", CodeElement.Plain)
        )
        val modifierText = if (onlyBaseStyle) {
            style.modifiers["base"]!!.asCodeBlocks(indentLevel = 1) + CodeBlock("\n", CodeElement.Plain)
        } else {
            style.modifiers.flatMap { (selectorName, modifier) ->
                listOf(CodeBlock("\t$selectorName {\n", CodeElement.Plain)) +
                        modifier.asCodeBlocks(indentLevel = 2) +
                        CodeBlock("\n\t}\n", CodeElement.Plain)
            }
        }
        styleText + modifierText + CodeBlock("}\n", CodeElement.Plain)
    }
}

internal fun ParsedModifier.asCodeBlocks(indentLevel: Int = 0): List<CodeBlock> {
    val indents = "\t".repeat(indentLevel)
    val coloredModifiers = properties.flatMap {
        listOf(
            CodeBlock("\n\t$indents.", CodeElement.Plain),
            CodeBlock(it.function, CodeElement.Function),
            CodeBlock("(", CodeElement.Plain)
        ) + it.args.asCodeBlocks() + CodeBlock(")", CodeElement.Plain)
    }
    return listOf(CodeBlock("${indents}Modifier", CodeElement.Plain)) + coloredModifiers
}

internal fun List<Arg>.asCodeBlocks(): List<CodeBlock> {
    return flatMapIndexed { index, arg ->
        when (arg) {
            is Arg.Literal -> listOf(CodeBlock(arg.value, CodeElement.String))
            is Arg.Number -> listOf(CodeBlock(arg.value, CodeElement.Number))
            is Arg.Property -> {
                listOf(
                    CodeBlock(arg.className + ".", CodeElement.Plain),
                    CodeBlock(arg.value, CodeElement.Property)
                )
            }

            is Arg.UnitNum -> {
                val num = arg.toString().substringBeforeLast(".")
                val numCode = if (num.first() == '(') {
                    listOf(
                        CodeBlock("(-", CodeElement.Plain),
                        CodeBlock(num.drop(2).dropLast(1), CodeElement.Number),
                        CodeBlock(")", CodeElement.Plain),
                    )
                } else listOf(CodeBlock(num, CodeElement.Number))

                numCode + listOf(
                    CodeBlock(".", CodeElement.Plain),
                    CodeBlock(arg.type, CodeElement.Property),
                )
            }

            is Arg.NamedArg -> {
                listOf(CodeBlock(arg.name + " = ", CodeElement.NamedArg)) + listOf(arg.value).asCodeBlocks()
            }

            is Arg.Function -> {
                listOf(CodeBlock(arg.name + "(", CodeElement.Plain)) +
                        arg.args.asCodeBlocks() +
                        CodeBlock(")", CodeElement.Plain)
            }
        }.let {
            if (index < size - 1) {
                it + CodeBlock(", ", CodeElement.Plain)
            } else it
        }
    }
}