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

            fun of(str: String, zeroUnit: String = "px"): UnitNum =
                ofOrNull(str, zeroUnit) ?: throw IllegalArgumentException("Not a unit number: $str")
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

        internal companion object // for extensions
    }

    override fun toString(): String = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean = other is Arg && other.value == value

    internal companion object // for extensions
}