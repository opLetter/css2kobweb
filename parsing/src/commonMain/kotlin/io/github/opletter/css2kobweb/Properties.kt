package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.functions.*
import kotlin.math.max
import kotlin.math.min

private val GlobalValues = setOf("initial", "inherit", "unset", "revert")

internal fun parseValue(propertyName: String, value: String): ParsedProperty {
    if (propertyName == "transition") {
        val transitions = value.splitNotInParens(',').map { transition ->
            val params = transition.splitNotInParens(' ').filter { it.isNotBlank() }
                .let { if (Arg.UnitNum.ofOrNull(it.first()) != null) listOf("all") + it else it }
            val thirdArg = params.getOrNull(2)?.let {
                Arg.UnitNum.ofOrNull(it) ?: parseValue("transitionTimingFunction", it).args.singleOrNull()
            }
            val fourthArg = params.getOrNull(3)?.let { Arg.UnitNum.of(it) }

            Arg.Function.transition(
                property = Arg.Literal("\"${params[0]}\""),
                duration = params.getOrNull(1)?.let { Arg.UnitNum.of(it) },
                remainingArgs = listOfNotNull(thirdArg, fourthArg),
            )
        }
        return ParsedProperty(propertyName, transitions)
    }
    if (propertyName == "transform") {
        val statements = value.splitNotInParens(' ').map { func ->
            val args = parenContents(func).splitNotInParens(',').map {
                if (it.toDoubleOrNull() == 0.0 && (func.startsWith("matrix") || func.startsWith("scale")))
                    Arg.RawNumber(0)
                else
                    Arg.UnitNum.ofOrNull(it) ?: Arg.RawNumber(it.toIntOrNull() ?: it.toDouble())
            }
            Arg.Function(func.substringBefore('('), args)
        }
        return ParsedProperty(propertyName, lambdaStatements = statements)
    }
    if (propertyName == "aspectRatio" && '/' in value) {
        return ParsedProperty(
            propertyName,
            value.split('/').map { Arg.RawNumber(it.toIntOrNull() ?: it.toDouble()) }
        )
    }
    if (propertyName == "fontFamily") {
        return ParsedProperty(
            propertyName,
            value.splitNotInParens(',').map { Arg.Literal.withQuotesIfNecessary(it) }
        )
    }
    if (propertyName == "background") {
        return ParsedProperty(propertyName, parseBackground(value))
    }
    if (propertyName == "backgroundPosition" && value !in GlobalValues) {
        val args = value.splitNotInParens(',').map {
            if (it in GlobalValues) parseValue(propertyName, it).args.single()
            else Arg.Function("BackgroundPosition.of", Arg.Function.position(it))
        }
        return ParsedProperty(propertyName, args)
    }
    if (propertyName == "backgroundPositionX" || propertyName == "backgroundPositionY") {
        // will be handled in postProcessing, preserve values for now
        return ParsedProperty(propertyName, Arg.Literal(value))
    }
    if (propertyName == "backgroundSize") {
        val args = value.splitNotInParens(',').map { subValue ->
            if (subValue in GlobalValues || subValue in setOf("cover", "contain")) {
                Arg.Property("BackgroundSize", kebabToPascalCase(subValue))
            } else {
                Arg.Function("BackgroundSize.of", subValue.splitNotInParens(' ').map { Arg.UnitNum.of(it) })
            }
        }
        return ParsedProperty(propertyName, args)
    }
    if (propertyName == "backgroundRepeat") {
        val args = value.splitNotInParens(',').map { subValue ->
            val values = subValue.splitNotInParens(' ')
                .map { Arg.Property(kebabToPascalCase(propertyName), kebabToPascalCase(it)) }

            values.singleOrNull() ?: Arg.Function("BackgroundRepeat.of", values)
        }
        return ParsedProperty(propertyName, args)
    }

    return value.splitNotBetween('(', ')', setOf(' ', ',', '/')).map { prop ->
        if (prop in GlobalValues) {
            return@map Arg.Property(classNamesFromProperty(propertyName), kebabToPascalCase(prop))
        }

        val unit = Arg.UnitNum.ofOrNull(prop)
        if (unit != null) {
            val takeRawZero = setOf(
                "zIndex", "opacity", "lineHeight", "flexGrow", "flexShrink", "flex", "order", "tabIndex",
                "gridColumnEnd", "gridColumnStart", "gridRowEnd", "gridRowStart",
            )

            return@map if (unit.toString().substringBeforeLast('.') == "0" && propertyName in takeRawZero)
                Arg.RawNumber(0)
            else unit
        }

        val rawNum = prop.toIntOrNull() ?: prop.toDoubleOrNull()
        if (rawNum != null) {
            return@map Arg.RawNumber(rawNum)
        }

        Arg.asColorOrNull(prop)?.let { return@map it }

        if (prop.startsWith("linear-gradient(")) {
            return@map Arg.Function.linearGradient(parenContents(prop))
        }
        if (prop.startsWith("radial-gradient(")) {
            return@map Arg.Function.radialGradient(parenContents(prop))
        }
        if (prop.startsWith("conic-gradient(")) {
            return@map Arg.Function.conicGradient(parenContents(prop))
        }

        if (prop.startsWith("url(")) {
            val contents = parenContents(prop)
            return@map Arg.Function("url", Arg.Literal.withQuotesIfNecessary(contents))
        }

        if (prop.startsWith('"')) {
            return@map Arg.Literal(prop)
        }
        if (propertyName == "transitionProperty") {
            return@map Arg.Literal("\"$prop\"")
        }

        val className = classNamesFromProperty(propertyName)

        if (prop.endsWith(")")) {
            val functionPropertyName = if (propertyName == "transitionTimingFunction" && prop.startsWith("steps(")) {
                "StepPosition"
            } else propertyName

            val filterFunctions = setOf(
                "blur", "brightness", "contrast", "dropShadow", "grayscale", "hueRotate", "invert", "saturate", "sepia",
            )
            val mathFunctions = setOf("clamp", "min", "max")
            val simpleGlobalFunctions = filterFunctions + mathFunctions

            val functionName = kebabToCamelCase(prop.substringBefore("("))
            val prefix = if (functionName in simpleGlobalFunctions) "" else "$className."

            return@map Arg.Function(
                "$prefix$functionName",
                parseValue(functionPropertyName, parenContents(prop)).args
            )
        }

        Arg.Property(className, kebabToPascalCase(prop))
    }.let { ParsedProperty(propertyName, it) }
}

private fun classNamesFromProperty(propertyName: String): String {
    return when (propertyName) {
        "display" -> "DisplayStyle"
        "overflowY", "overflowX" -> "Overflow"
        "float" -> "CSSFloat"
        "border", "borderStyle", "borderTop", "borderBottom", "borderLeft", "borderRight",
        "outline", "outlineStyle",
        -> "LineStyle"

        else -> propertyName.replaceFirstChar { it.uppercase() }
    }
}

private fun parseBackground(value: String): List<Arg> {
    // kobweb reverses order of backgrounds
    val backgrounds = value.splitNotInParens(',').reversed()
        .map { it.splitNotInParens('/').joinToString(" / ") }

    val backgroundObjects = backgrounds.map { background ->
        val repeatRegex = """(repeat-x|repeat-y|repeat|space|round|no-repeat)\b""".toRegex()
        val attachmentRegex = """(scroll|fixed|local)\b""".toRegex()
        val boxRegex = """(border-box|padding-box|content-box)\b""".toRegex()

        val backgroundArgs = buildList {
            val image = background.splitNotInParens(' ').firstOrNull {
                it.startsWith("url(") || it.startsWith("linear-gradient(")
                        || it.startsWith("radial-gradient(") || it.startsWith("conic-gradient(")
            }
            if (image != null) {
                val imageArg = parseValue("backgroundImage", image).args.single()
                    .let { if (it is Arg.Function) Arg.Function("BackgroundImage.of", it) else it }
                add(Arg.NamedArg("image", imageArg))
            }

            val repeat = repeatRegex.find(background)?.value
            if (repeat != null) {
                val repeatArg = parseValue("backgroundRepeat", repeat).args.single()
                add(Arg.NamedArg("repeat", repeatArg))
            }

            val attachment = attachmentRegex.find(background)?.value
            if (attachment != null) {
                val attachmentArg = parseValue("backgroundAttachment", attachment).args.single()
                add(Arg.NamedArg("attachment", attachmentArg))
            }

            val boxMatches = boxRegex.findAll(background).toList()
            if (boxMatches.isNotEmpty()) {
                val (origin, clip) = if (boxMatches.size == 2) boxMatches else List(2) { boxMatches.single() }
                val originArg = parseValue("backgroundOrigin", origin.value).args.single()
                val clipArg = parseValue("backgroundClip", clip.value).args.single()
                add(Arg.NamedArg("origin", originArg))
                add(Arg.NamedArg("clip", clipArg))
            }

            val otherProps = background.splitNotInParens(' ') - setOfNotNull(image, repeat, attachment) -
                    boxMatches.map { it.value }.toSet()

            val slashIndex = otherProps.indexOf("/")
            if (slashIndex != -1) {
                val positionStr = otherProps.subList(max(0, slashIndex - 2), slashIndex).joinToString(" ")
                val position = Arg.Function.positionOrNull(positionStr)
                    ?: Arg.Function.position(otherProps[slashIndex - 1])
                val size = otherProps.subList(slashIndex + 1, min(otherProps.size, slashIndex + 3))
                    .mapNotNull { Arg.UnitNum.ofOrNull(it) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { Arg.Function("BackgroundSize.of", it) }
                    ?: parseValue("backgroundSize", otherProps[slashIndex + 1]).args.single()

                add(Arg.NamedArg("position", Arg.Function("BackgroundPosition.of", position)))
                add(Arg.NamedArg("size", size))
            } else if (otherProps.isNotEmpty()) {
                val position = (0..otherProps.size - 2).firstNotNullOfOrNull {
                    val positionStr = otherProps.subList(it, it + 2).joinToString(" ")
                    Arg.Function.positionOrNull(positionStr)
                } ?: Arg.Function.positionOrNull(otherProps.first())
                ?: Arg.Function.positionOrNull(otherProps.last())

                position?.let {
                    add(Arg.NamedArg("position", Arg.Function("BackgroundPosition.of", it)))
                }
            }
        }
        Arg.Function("CSSBackground", backgroundArgs)
    }.filter { it.args.isNotEmpty() }

    val color = backgrounds.first().splitNotInParens(' ').firstNotNullOfOrNull { Arg.asColorOrNull(it) }
    return listOfNotNull(color) + backgroundObjects
}