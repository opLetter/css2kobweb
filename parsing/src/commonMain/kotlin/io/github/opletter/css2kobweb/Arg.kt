package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.constants.units

sealed class Arg(private val value: String) {
    class Literal(val value: String) : Arg(value) {
        companion object {
            fun withQuotesIfNecessary(value: String): Literal {
                val str = if (value.firstOrNull() == '"') value else "\"$value\""
                return Literal(str)
            }
        }
    }

    sealed class Number(val value: String) : Arg(value)

    class RawNumber(value: kotlin.Number) : Number(value.toString()), CalcNumber
    class Hex(value: String) : Number("0x$value")
    class Float(value: kotlin.Number) : Number("${value}f")

    // technically this behaves the same as [Property], but t be more explicit we treat it as a separate type
    class UnitNum(value: kotlin.Number, val type: String) :
        Arg("${if (value.toDouble() < 0.0) "($value)" else "$value"}.$type"), CalcNumber {

        companion object {
            fun ofOrNull(str: String, zeroUnit: String = "px"): UnitNum? {
                if (str == "0") return UnitNum(0, zeroUnit)

                val potentialUnit = str.dropWhile { it.isDigit() || it == '.' || it == '-' || it == '+' }.lowercase()
                val unit = units[potentialUnit]
                if (unit != null) {
                    val num = str.dropLast(potentialUnit.length)
                    return UnitNum(num.toIntOrNull() ?: num.toDouble(), unit)
                }
                return null
            }

            fun of(str: String, zeroUnit: String = "px"): UnitNum {
                return ofOrNull(str, zeroUnit) ?: throw IllegalArgumentException("Not a unit number: $str")
            }
        }
    }

    sealed interface CalcNumber {
        val arg: Arg
            get() = when (this) {
                is RawNumber -> this
                is UnitNum -> this
            }
    }

    class Calc(val arg1: CalcNumber, val arg2: CalcNumber, val operation: Char) : Arg("$arg1 $operation $arg2")

    class Property(val className: String, val value: String) : Arg("$className.$value")

    class NamedArg(val name: String, val value: Arg) : Arg("$name = $value")

    class Function(
        val name: String,
        val args: List<Arg> = emptyList(),
        val lambdaStatements: List<Function> = emptyList(),
    ) : Arg(
        if (lambdaStatements.isEmpty()) "$name(${args.joinToString(", ")})"
        else {
            val argsStr = if (args.isEmpty()) "" else args.joinToString(", ", prefix = "(", postfix = ")")
            val lambdaStr = lambdaStatements.joinToString("\n\t\t", prefix = " {\n\t\t", postfix = "\n\t}")
            name + argsStr + lambdaStr
        }
    ) {
        internal companion object // for extensions
    }

    override fun toString(): String = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean = other is Arg && other.value == value

    internal companion object // for extensions
}