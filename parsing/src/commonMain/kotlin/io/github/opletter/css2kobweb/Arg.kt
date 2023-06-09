package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.constants.units

sealed class Arg(private val value: String) {
    class Literal(value: String) : Arg(value) {
        companion object {
            fun withQuotesIfNecessary(value: String): Literal {
                val str = if (value.firstOrNull() == '"') value else "\"$value\""
                return Literal(str)
            }
        }
    }

    sealed class FancyNumber(value: String) : Arg(value)
    class Hex(value: String) : FancyNumber("0x$value")
    class Float(value: Number) : FancyNumber("${value}f")

    /** A number that can be used in a calculation */
    sealed class CalcNumber(value: String) : Arg(value)

    class RawNumber(value: Number) : CalcNumber(value.toString())

    sealed class UnitNum(value: String) : CalcNumber(value) {
        class Normal(value: Number, val type: String) :
            UnitNum("${if (value.toDouble() < 0.0) "($value)" else "$value"}.$type")

        class Calc(val arg1: CalcNumber, val arg2: CalcNumber, val operation: Char) : UnitNum(run {
            val arg1Str = arg1.let { if (it is Calc) "($it)" else "$it" }
            val arg2Str = arg2.let { if (it is Calc) "($it)" else "$it" }
            "$arg1Str $operation $arg2Str"
        })

        object Auto : UnitNum("auto")

        companion object {
            private fun String.prependCalcToParens(): String = fold("") { result, c ->
                result + if (c == '(' && result.takeLast(4) != "calc") "calc$c" else c
            }

            private fun parseCalcNum(str: String, zeroUnit: String): CalcNumber? {
                if (str.startsWith("calc(")) {
                    // whitespace isn't required for / & *, so we add it for parsing (extra space gets trimmed anyway)
                    val expr = parenContents(str)
                        .replace("/", " / ")
                        .replace("*", " * ")

                    val parts = expr.splitNotInParens(' ')

                    return when {
                        parts.size == 1 -> parseCalcNum(parts.single(), zeroUnit)

                        parts.size > 3 -> {
                            val newCalc = parts.take(3).joinToString(" ", prefix = "calc(", postfix = ") ") +
                                    parts.drop(3).joinToString(" ")
                            parseCalcNum("calc($newCalc)", zeroUnit)
                        }

                        parts.size == 3 -> {
                            val (arg1, operation, arg2) = parts
                            Calc(parseCalcNum(arg1, zeroUnit)!!, parseCalcNum(arg2, zeroUnit)!!, operation.single())
                        }

                        else -> null
                    }
                }

                if (str == "0") return Normal(0, zeroUnit)

                val potentialUnit = str.dropWhile { it.isDigit() || it == '.' || it == '-' || it == '+' }.lowercase()
                val unit = units[potentialUnit]
                if (unit != null) {
                    val num = str.dropLast(potentialUnit.length)
                    return Normal(num.toIntOrNull() ?: num.toDouble(), unit)
                }
                return (str.toIntOrNull() ?: str.toDoubleOrNull())?.let { RawNumber(it) }
            }

            fun ofOrNull(str: String, zeroUnit: String = "px"): UnitNum? =
                parseCalcNum(str.prependCalcToParens(), zeroUnit) as? UnitNum

            fun of(str: String, zeroUnit: String = "px"): UnitNum {
                return (if (str == "auto") Auto else ofOrNull(str, zeroUnit))
                    ?: throw IllegalArgumentException("Not a unit number: $str")
            }
        }
    }

    class Property(val className: String, val value: String) : Arg("$className.$value")

    class NamedArg(val name: String, val value: Arg) : Arg("$name = $value")

    class Function(
        val name: String,
        val args: List<Arg> = emptyList(),
        val lambdaStatements: List<Function> = emptyList(),
    ) : CssParseResult, Arg(
        if (lambdaStatements.isEmpty()) "$name(${args.joinToString(", ")})"
        else {
            val argsStr = if (args.isEmpty()) "" else args.joinToString(", ", prefix = "(", postfix = ")")
            val lambdaStr = lambdaStatements.joinToString("\n\t\t", prefix = " {\n\t\t", postfix = "\n\t}")
            name + argsStr + lambdaStr
        }
    ) {
        constructor(name: String, arg: Arg) : this(name, listOf(arg))

        override fun asCodeBlocks(indentLevel: Int): List<CodeBlock> = (this as Arg).asCodeBlocks(indentLevel)

        internal companion object // for extensions
    }

    override fun toString(): String = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean = other is Arg && other.value == value

    internal companion object // for extensions
}


fun Arg.asCodeBlocks(
    indentLevel: Int,
    functionType: CodeElement = CodeElement.Plain,
    surroundWithParens: Boolean = false,
): List<CodeBlock> {
    return when (this) {
        is Arg.Literal -> listOf(CodeBlock(toString(), CodeElement.String))
        is Arg.FancyNumber, is Arg.RawNumber -> listOf(CodeBlock(toString(), CodeElement.Number))
        is Arg.Property -> listOf(
            CodeBlock("$className.", CodeElement.Plain),
            CodeBlock(value, CodeElement.Property),
        )

        is Arg.UnitNum.Normal -> {
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

        is Arg.UnitNum.Calc -> {
            val expression = buildList {
                addAll(arg1.asCodeBlocks(indentLevel, surroundWithParens = true))
                add(CodeBlock(" $operation ", CodeElement.Plain))
                addAll(arg2.asCodeBlocks(indentLevel, surroundWithParens = true))
            }
            if (surroundWithParens) {
                buildList {
                    add(CodeBlock("(", CodeElement.Plain))
                    addAll(expression)
                    add(CodeBlock(")", CodeElement.Plain))
                }
            } else expression
        }

        is Arg.UnitNum.Auto -> listOf(CodeBlock("auto", CodeElement.Property))

        is Arg.NamedArg -> listOf(CodeBlock("$name = ", CodeElement.NamedArg)) + value.asCodeBlocks(indentLevel)

        is Arg.Function -> buildList {
            val indents = "\t".repeat(indentLevel)
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

                // todo: consider making this a setting
                val sameLine = lambdaStatements.size == 1 && lambdaStatements.single().toString().length < 50
                val lambdaPrefix = if (sameLine) " " else "\n\t$indents"

                val lambdaLines = lambdaStatements.flatMap {
                    listOf(CodeBlock(lambdaPrefix, CodeElement.Plain)) + it.asCodeBlocks(indentLevel + 1)
                }
                addAll(lambdaLines)
                add(CodeBlock(if (sameLine) " }" else "\n$indents}", CodeElement.Plain))
            }
        }
    }
}