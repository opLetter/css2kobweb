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
        class Normal(val value: Number, val type: String) :
            UnitNum("${if (value.toDouble() < 0.0) "($value)" else "$value"}.$type")

        class Calc(val arg1: CalcNumber, val arg2: CalcNumber, val operation: Char) : UnitNum(run {
            val arg1Str = arg1.let { if (it is Calc) "($it)" else "$it" }
            val arg2Str = arg2.let { if (it is Calc) "($it)" else "$it" }
            "$arg1Str $operation $arg2Str"
        })

        object Auto : UnitNum("numericAuto")

        companion object {
            private fun String.prependCalcToParens(): String = fold("") { result, c ->
                result + if (c == '(' && result.takeLast(4) != "calc") "calc$c" else c
            }

            private fun parseCalcNum(str: String, zeroUnit: String): CalcNumber? {
                if (str == "0") return Normal(0, zeroUnit)

                if (str.startsWith("calc(")) {
                    // whitespace isn't required for / & *, so we add it for parsing (extra space gets trimmed anyway)
                    val expr = parenContents(str)
                        .replace("/", " / ")
                        .replace("*", " * ")
                    val parts = expr.splitNotInParens(' ')

                    return when (parts.size) {
                        0, 2 -> null
                        1 -> parseCalcNum(parts.single(), zeroUnit)
                        3 -> {
                            val (arg1, operation, arg2) = parts
                            Calc(parseCalcNum(arg1, zeroUnit)!!, parseCalcNum(arg2, zeroUnit)!!, operation.single())
                        }

                        else -> {
                            // For chained operations (e.g. "1px + 2px + 3px..."), we recursively add "calc(..)"
                            // wrappings so that the rest of the parsing logic can handle it.
                            val newCalc = parts.take(3).joinToString(" ", prefix = "calc(", postfix = ") ") +
                                    parts.drop(3).joinToString(" ")
                            parseCalcNum("calc($newCalc)", zeroUnit)
                        }
                    }
                }

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
                val unitNum = if (str == "auto") Auto else ofOrNull(str, zeroUnit)
                return requireNotNull(unitNum) { "Not a unit number: $str" }
            }
        }
    }

    class Property(val className: String?, val value: String) : Arg(className?.let { "$it." }.orEmpty() + value) {
        companion object {
            fun fromKebabValue(className: String?, value: String) = Property(className, kebabToPascalCase(value))
        }
    }

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

    class ExtensionCall(val property: Arg, val function: Function) : Arg("$property.$function")

    override fun toString(): String = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean = other is Arg && other.value == value

    internal companion object // for extensions
}


fun Arg.asCodeBlocks(
    indentLevel: Int,
    functionType: CodeElement = CodeElement.Plain,
    nestedCalc: Boolean = false,
): List<CodeBlock> {
    return when (this) {
        is Arg.Literal -> listOf(CodeBlock(toString(), CodeElement.String))
        is Arg.FancyNumber, is Arg.RawNumber -> listOf(CodeBlock(toString(), CodeElement.Number))
        is Arg.Property -> listOfNotNull(
            className?.let { CodeBlock("$it.", CodeElement.Plain) },
            CodeBlock(value, CodeElement.Property),
        )

        is Arg.UnitNum.Normal -> buildList {
            add(CodeBlock(value.toString(), CodeElement.Number))
            if (value.toDouble() < 0.0) {
                add(0, CodeBlock("(", CodeElement.Plain))
                add(CodeBlock(")", CodeElement.Plain))
            }
            add(CodeBlock(".", CodeElement.Plain))
            add(CodeBlock(type, CodeElement.Property))
        }

        is Arg.UnitNum.Calc -> buildList {
            addAll(arg1.asCodeBlocks(indentLevel, nestedCalc = true))
            add(CodeBlock(" $operation ", CodeElement.Plain))
            addAll(arg2.asCodeBlocks(indentLevel, nestedCalc = true))

            if (nestedCalc) {
                add(0, CodeBlock("(", CodeElement.Plain))
                add(CodeBlock(")", CodeElement.Plain))
            }
        }

        is Arg.UnitNum.Auto -> listOf(CodeBlock(toString(), CodeElement.Property))

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
                args.forEachIndexed { index, arg ->
                    addAll(arg.asCodeBlocks(indentLevel + if (longArgs) 1 else 0))
                    if (index < args.size - 1)
                        add(CodeBlock(separator, CodeElement.Plain))
                }
                add(CodeBlock(end, CodeElement.Plain))
            }
            if (lambdaStatements.isNotEmpty()) {
                add(CodeBlock(" {", CodeElement.Plain))
                // todo: consider making this a setting
                val sameLine = lambdaStatements.size == 1 && lambdaStatements.single().toString().length < 50

                lambdaStatements.forEach {
                    add(CodeBlock(if (sameLine) " " else "\n\t$indents", CodeElement.Plain))
                    addAll(it.asCodeBlocks(indentLevel + 1))
                }
                add(CodeBlock(if (sameLine) " }" else "\n$indents}", CodeElement.Plain))
            }
        }

        is Arg.ExtensionCall -> {
            property.asCodeBlocks(indentLevel) + CodeBlock(".", CodeElement.Plain) +
                    function.asCodeBlocks(indentLevel, functionType = CodeElement.ExtensionFun)
        }
    }
}