package io.github.opletter.css2kobweb

sealed class Arg(private val value: String) {
    class Literal(val value: String) : Arg(value)

    sealed class Number(val value: String) : Arg(value)

    class RawNumber(value: kotlin.Number) : Number(value.toString())
    class Hex(value: String) : Number("0x$value")
    class Float(value: kotlin.Number) : Number("${value}f")

    // technically this behaves the same as [Property], but t be more explicit we treat it as a separate type
    class UnitNum(val value: kotlin.Number, val type: String) : Arg("$value.$type") {
        companion object {
            fun ofOrNull(str: String): UnitNum? {
                val potentialUnit = str.dropWhile { it.isDigit() || it == '.' }
                val unit = units[potentialUnit]
                if (unit != null) {
                    val num = str.dropLast(potentialUnit.length)
                    return UnitNum(num.toIntOrNull() ?: num.toDouble(), unit)
                }
                return null
            }

            fun of(str: String): UnitNum {
                return ofOrNull(str) ?: throw IllegalArgumentException("Not a unit number: $str")
            }
        }
    }

    class Property(val className: String, val value: String) : Arg("$className.$value")

    class NamedArg(val name: String, val value: Arg) : Arg("$name = $value")

    class Function(val name: String, val args: List<Arg>) : Arg("$name(${args.joinToString(", ")})") {
        internal companion object // for extensions
    }

    override fun toString(): String = value
}