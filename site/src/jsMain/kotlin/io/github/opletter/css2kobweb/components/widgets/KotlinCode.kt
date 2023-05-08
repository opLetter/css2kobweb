package io.github.opletter.css2kobweb.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.text.SpanText
import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.CssParseResult
import io.github.opletter.css2kobweb.ParsedComponentStyles
import io.github.opletter.css2kobweb.ParsedModifier
import io.github.opletter.css2kobweb.pages.ColorScheme
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text

@Composable
fun KotlinCode(output: CssParseResult, syntaxHighlight: Boolean) {
    Pre(Modifier.margin(0.px).toAttrs()) {
        if (!syntaxHighlight) {
            Text(output.toString())
            return@Pre
        }

        if (output is ParsedModifier) {
            HighlightedModifier(output)
            return@Pre
        }
        check(output is ParsedComponentStyles)

        output.styles.forEach { style ->
            val onlyBaseStyle = style.modifiers.size == 1 && style.modifiers.keys.first() == "base"
            val extra = if (onlyBaseStyle) ".base" else ""

            SpanText("val ", Modifier.color(ColorScheme.keyword))
            SpanText("${style.name}Style", Modifier.color(ColorScheme.property))
            SpanText(" by ", Modifier.color(ColorScheme.keyword))
            Text("ComponentStyle$extra {\n")
            if (onlyBaseStyle) {
                HighlightedModifier(style.modifiers["base"]!!, indentLevel = 1)
                Text("\n")
            } else {
                style.modifiers.forEach { (selectorName, modifier) ->
                    Text("\t$selectorName {\n")
                    HighlightedModifier(modifier, indentLevel = 2)
                    Text("\n\t}\n")
                }
            }
            Text("}\n")
        }
    }
}

@Composable
fun HighlightedModifier(parsedModifier: ParsedModifier, indentLevel: Int = 0) {
    val indents = "\t".repeat(indentLevel)
    Text("${indents}Modifier")
    parsedModifier.properties.forEach { a ->
        Text("\n\t$indents.")
        SpanText(a.function, Modifier.color(ColorScheme.function))
        Text("(")
        HighLightedArgs(a.args)
        Text(")")
    }
}

@Composable
fun HighLightedArgs(args: List<Arg>) {
    args.forEachIndexed { index, arg ->
        when (arg) {
            is Arg.Literal -> SpanText(arg.value, Modifier.color(ColorScheme.string))
            is Arg.Number -> SpanText(arg.value, Modifier.color(ColorScheme.number))
            is Arg.Property -> {
                Text(arg.className + ".")
                SpanText(arg.value, Modifier.color(ColorScheme.property))
            }

            is Arg.UnitNum -> {
                Text(arg.value.toString() + ".")
                SpanText(arg.type, Modifier.color(ColorScheme.property))
            }

            is Arg.NamedArg -> {
                SpanText(arg.name + " = ", Modifier.color(ColorScheme.namedArg))
                HighLightedArgs(listOf(arg.value))
            }

            is Arg.Function -> {
                Text(arg.name + "(")
                HighLightedArgs(arg.args)
                Text(")")
            }
        }
        if (index < args.size - 1) {
            Text(", ")
        }
    }
}