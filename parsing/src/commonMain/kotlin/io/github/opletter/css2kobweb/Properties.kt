package io.github.opletter.css2kobweb

import io.github.opletter.css2kobweb.functions.*
import kotlin.math.min

private val GlobalValues = setOf("initial", "inherit", "unset", "revert")

internal fun parseCssProperty(propertyName: String, value: String): ParsedProperty {
    if (propertyName == "transition") {
        val transitions = value.splitNotInParens(',').map { transition ->
            val args = transition.splitNotInParens(' ').mapNotNull { arg ->
                if (listOf("linear", "cubic", "ease", "step").any { arg.startsWith(it) }) {
                    parseCssProperty("transitionTimingFunction", arg).args.singleOrNull()
                } else {
                    Arg.UnitNum.ofOrNull(arg) ?: Arg.Literal("\"$arg\"")
                }
            }
            val durationIndex = args.indexOfFirst { it is Arg.UnitNum }
            Arg.Function.transition(
                property = args.singleOrNull { it is Arg.Literal } ?: Arg.Literal("\"all\""),
                duration = args.getOrNull(durationIndex),
                remainingArgs = listOfNotNull(
                    args.firstOrNull { it !is Arg.UnitNum && it !is Arg.Literal },
                    args.drop(durationIndex + 1).firstOrNull { it is Arg.UnitNum }
                )
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
            if (it in GlobalValues) parseCssProperty(propertyName, it).args.single()
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
                Arg.Property.fromKebabValue("BackgroundSize", subValue)
            } else {
                Arg.Function("BackgroundSize.of", subValue.splitNotInParens(' ').map { Arg.UnitNum.of(it) })
            }
        }
        return ParsedProperty(propertyName, args)
    }
    if (propertyName == "backgroundRepeat") {
        val args = value.splitNotInParens(',').map { subValue ->
            val values = subValue.splitNotInParens(' ')
                .map { Arg.Property.fromKebabValue(kebabToPascalCase(propertyName), it) }

            values.singleOrNull() ?: Arg.Function("BackgroundRepeat.of", values)
        }
        return ParsedProperty(propertyName, args)
    }
    if (propertyName == "animation") {
        return ParsedProperty(propertyName, parseAnimation(value))
    }
    if (propertyName == "animationName") {
        return ParsedProperty(propertyName, Arg.Literal("\"$value\""))
    }
    if (propertyName == "animationIterationCount") {
        val num = value.toIntOrNull() ?: value.toDoubleOrNull()
        val arg = if (num != null) {
            Arg.Function("AnimationIterationCount.of", Arg.RawNumber(num))
        } else {
            Arg.Property.fromKebabValue("AnimationIterationCount", value)
        }
        return ParsedProperty(propertyName, arg)
    }
    if (
        value !in GlobalValues && value != "none"
        && propertyName in setOf("gridAutoRows", "gridAutoColumns", "gridTemplateRows", "gridTemplateColumns")
    ) {
        return ParsedProperty(propertyName, lambdaStatements = parseGridRowCol(value))
    }
    if (value !in GlobalValues && propertyName == "flexFlow") {
        val subValues = value.splitNotInParens(' ')
        val indexOfWrap = subValues.indexOfFirst { "wrap" in it }

        return if (subValues.size == 2) {
            ParsedProperty(
                propertyName,
                parseCssProperty("flexDirection", subValues[1 - indexOfWrap]).args +
                        parseCssProperty("flexWrap", subValues[indexOfWrap]).args
            )
        } else {
            val property = if (indexOfWrap != -1) "flexWrap" else "flexDirection"
            ParsedProperty(property, parseCssProperty(property, value).args)
        }
    }
    // kobweb treats "nowrap" as if it was "no-wrap", so we need to handle it separately
    if (propertyName == "whiteSpace" && value == "nowrap") {
        return ParsedProperty(propertyName, Arg.Property("WhiteSpace", "NoWrap"))
    }

    return value.splitNotBetween(setOf('(' to ')'), setOf(' ', ',', '/')).map { prop ->
        if (prop in GlobalValues) {
            return@map Arg.Property.fromKebabValue(classNamesFromProperty(propertyName), prop)
        }

        val unit = Arg.UnitNum.ofOrNull(prop)
        if (unit != null) {
            val takeRawZero = setOf(
                "zIndex", "opacity", "lineHeight", "flexGrow", "flexShrink", "flex", "order",
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
            val functionPropertyName = if (propertyName.endsWith("TimingFunction") && prop.startsWith("steps(")) {
                "StepPosition"
            } else propertyName

            val filterFunctions = setOf(
                "blur", "brightness", "contrast", "dropShadow", "grayscale", "hueRotate", "invert", "saturate", "sepia",
            )
            val mathFunctions = setOf("clamp", "min", "max")
            val simpleGlobalFunctions = filterFunctions + mathFunctions

            val functionName = kebabToCamelCase(prop.substringBefore("("))
            val prefix = if (functionName in simpleGlobalFunctions) "" else "$className."

            val adjustedArgs = parenContents(prop).let { args ->
                // math function can contain expressions, so wrap them in calc() for parsing purposes
                if (functionName in mathFunctions)
                    args.splitNotInParens(',').joinToString { "calc($it)" }
                else args
            }

            return@map Arg.Function("$prefix$functionName", parseCssProperty(functionPropertyName, adjustedArgs).args)
        }

        Arg.Property.fromKebabValue(className, prop).let {
            if (it.value == "Auto" && it.className != null && takesAutoLength(it.className)) {
                Arg.UnitNum.Auto
            } else it
        }
    }.let { ParsedProperty(propertyName, it) }
}

// Loose check for properties that often use "auto" as a length
private fun takesAutoLength(className: String): Boolean {
    return className.startsWith("Padding") || className.startsWith("Margin")
            || className.endsWith("Width") || className.endsWith("Height")
}

private fun classNamesFromProperty(propertyName: String): String {
    return when (propertyName) {
        "display" -> "DisplayStyle"
        "overflowY", "overflowX" -> "Overflow"
        "float" -> "CSSFloat"
        "gridTemplateRows", "gridTemplateColumns" -> "GridTemplate"
        "gridAutoRows", "gridAutoColumns" -> "GridAuto"
        "border", "borderStyle", "borderTop", "borderBottom", "borderLeft", "borderRight",
        "borderTopStyle", "borderBottomStyle", "borderLeftStyle", "borderRightStyle",
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
                val imageArg = parseCssProperty("backgroundImage", image).args.single().let {
                    if (it is Arg.Function) {
                        if (it.name == "url") Arg.Function("BackgroundImage.of", it)
                        else Arg.ExtensionCall(it, Arg.Function("toImage"))
                    } else it
                }
                add(Arg.NamedArg("image", imageArg))
            }

            val repeat = repeatRegex.find(background)?.value
            if (repeat != null) {
                val repeatArg = parseCssProperty("backgroundRepeat", repeat).args.single()
                add(Arg.NamedArg("repeat", repeatArg))
            }

            val attachment = attachmentRegex.find(background)?.value
            if (attachment != null) {
                val attachmentArg = parseCssProperty("backgroundAttachment", attachment).args.single()
                add(Arg.NamedArg("attachment", attachmentArg))
            }

            val boxMatches = boxRegex.findAll(background).toList()
            if (boxMatches.isNotEmpty()) {
                val (origin, clip) = if (boxMatches.size == 2) boxMatches else List(2) { boxMatches.single() }
                val originArg = parseCssProperty("backgroundOrigin", origin.value).args.single()
                val clipArg = parseCssProperty("backgroundClip", clip.value).args.single()
                add(Arg.NamedArg("origin", originArg))
                add(Arg.NamedArg("clip", clipArg))
            }

            val otherProps = background.splitNotInParens(' ') - setOfNotNull(image, repeat, attachment) -
                    boxMatches.map { it.value }.toSet()

            val slashIndex = otherProps.indexOf("/")
            if (slashIndex != -1) {
                val position = ((slashIndex - 4).coerceAtLeast(0)..<slashIndex).firstNotNullOf {
                    val positionStr = otherProps.subList(it, slashIndex).joinToString(" ")
                    Arg.Function.positionOrNull(positionStr)
                }
                val size = otherProps.subList(slashIndex + 1, min(otherProps.size, slashIndex + 3))
                    .mapNotNull { if (it == "auto") Arg.UnitNum.Auto else Arg.UnitNum.ofOrNull(it) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { Arg.Function("BackgroundSize.of", it) }
                    ?: parseCssProperty("backgroundSize", otherProps[slashIndex + 1]).args.single()

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
        Arg.Function("Background.of", backgroundArgs)
    }.filter { it.args.isNotEmpty() }

    val color = backgrounds.first().splitNotInParens(' ').firstNotNullOfOrNull { Arg.asColorOrNull(it) }
    return listOfNotNull(color) + backgroundObjects
}

private fun parseAnimation(value: String): List<Arg> {
    val animations = value.splitNotInParens(',')
    val animationObjects = animations.map { animation ->
        val timingRegex =
            """((ease-in-out|ease-in|ease-out|ease|linear|step-start|step-end)|cubic-bezier\([^)]*\)|steps\([^)]*\))""".toRegex()
        val directionRegex = """(normal|reverse|alternate|alternate-reverse)\b""".toRegex()
        val fillModeRegex = """(none|forwards|backwards|both)\b""".toRegex()
        val playStateRegex = """(running|paused)\b""".toRegex()

        val parts = animation.splitNotInParens(' ')

        val units = parts.mapNotNull { Arg.UnitNum.ofOrNull(it) }

        val animationArgs = buildList {
            if (units.isNotEmpty()) {
                add(Arg.NamedArg("duration", units.first()))
            }

            val timing = timingRegex.find(animation)?.value
            if (timing != null) {
                val repeatArg = parseCssProperty("animationTimingFunction", timing).args.single()
                add(Arg.NamedArg("timingFunction", repeatArg))
            }

            if (units.size > 1) {
                add(Arg.NamedArg("delay", units[1]))
            }

            val iterationCount = parts.firstNotNullOfOrNull { it.toIntOrNull() ?: it.toDoubleOrNull() }
                ?: "infinite".takeIf { it in parts }

            if (iterationCount != null) {
                val iterationCountArg =
                    parseCssProperty("animationIterationCount", iterationCount.toString()).args.single()
                add(Arg.NamedArg("iterationCount", iterationCountArg))
            }

            val direction = directionRegex.find(animation)?.value
            if (direction != null) {
                val directionArg = parseCssProperty("animationDirection", direction).args.single()
                add(Arg.NamedArg("direction", directionArg))
            }

            val fillMode = fillModeRegex.find(animation)?.value
            if (fillMode != null) {
                val fillModeArg = parseCssProperty("animationFillMode", fillMode).args.single()
                add(Arg.NamedArg("fillMode", fillModeArg))
            }

            val playState = playStateRegex.find(animation)?.value
            if (playState != null) {
                val playStateArg = parseCssProperty("animationPlayState", playState).args.single()
                add(Arg.NamedArg("playState", playStateArg))
            }

            val otherProps = parts.filter { Arg.UnitNum.ofOrNull(it) == null } -
                    setOfNotNull(timing, iterationCount.toString(), direction, fillMode, playState)

            val name = otherProps.lastOrNull { it.isNotBlank() } // search from end per css best practice
            if (name != null) {
                add(0, Arg.NamedArg("name", parseCssProperty("animationName", name).args.single()))
            }
        }
        Arg.Function("Animation.of", animationArgs)
    }.filter { it.args.isNotEmpty() }

    return animationObjects
}

private fun parseGridRowCol(value: String): List<Arg.Function> {
    return value.splitNotBetween(setOf('(' to ')', '[' to ']'), setOf(' ')).map { subValue ->
        if (subValue.startsWith("[")) {
            Arg.Function(
                "lineNames",
                subValue.drop(1).dropLast(1).splitNotInParens(' ').map { Arg.Literal.withQuotesIfNecessary(it) }
            )
        } else if (subValue.startsWith("minmax") || subValue.startsWith("fit-content")) {
            Arg.Function(
                kebabToCamelCase(subValue.substringBefore("(")),
                parenContents(subValue).splitNotInParens(',').map {
                    Arg.UnitNum.ofOrNull(it.trim()) ?: Arg.Property(null, kebabToCamelCase(it.trim()))
                }
            )
        } else if (subValue.startsWith("repeat")) {
            val repeatArgs = parenContents(subValue).splitNotInParens(',')
            val repeatCount = repeatArgs[0].toIntOrNull()?.let { Arg.RawNumber(it) }
                ?: Arg.Property(null, kebabToCamelCase(repeatArgs[0]))
            Arg.Function("repeat", listOf(repeatCount), parseGridRowCol(repeatArgs[1]))
        } else {
            Arg.Function(
                "size",
                Arg.UnitNum.ofOrNull(subValue) ?: Arg.Property(null, kebabToCamelCase(subValue))
            )
        }
    }
}