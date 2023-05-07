package io.github.opletter.css2kobweb.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.text.SpanText
import io.github.opletter.css2kobweb.pages.ColorScheme
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text

@Composable
fun KotlinCode(outputText: String, syntaxHighlight: Boolean) {
    Pre(Modifier.margin(0.px).toAttrs()) {
        if (outputText.isBlank()) return@Pre

        if (!syntaxHighlight) {
            Text(outputText)
            return@Pre
        }

        if ("val " !in outputText) {
            HighlightedModifier(outputText)
            return@Pre
        }

        outputText.split("val ").filter { it.isNotBlank() }.forEach { style ->
            SpanText("val ", Modifier.color(ColorScheme.keyword))
            SpanText(style.substringBefore(" by"), Modifier.color(ColorScheme.property))
            SpanText(" by ", Modifier.color(ColorScheme.keyword))

            val x = style.substringAfter(" by ").split("Modifier")
            Text(x[0])

            x.drop(1).forEach {
                HighlightedModifier(it.substringBefore("}"))
                Text("}" + it.substringAfter("}").substringBefore("Modifier"))
            }
        }
    }
}

@Composable
fun HighlightedModifier(text: String) {
    val indentLevel = text.substringAfter("Modifier").substringBefore(".").count { it == '\t' }
    val indents = "\t".repeat(indentLevel)
    Text("Modifier\n")
    text.substringAfter("Modifier").split("$indents.").drop(1).forEach {
        Text("$indents.")
        SpanText(it.substringBefore("("), Modifier.color(ColorScheme.function))
        Text("(")
        HighLightedArgs(it)
        Text(")" + it.substringAfterLast(")"))
    }
}

@Composable
fun HighLightedArgs(args: String) {
    val commaCount: Int
    args.substringAfter("(").substringBeforeLast(")")
        .split("\\s*(?![^()]*\\)), \\s*".toRegex()) // split on ", ", but not in parens
        .also { commaCount = it.size - 1 }
        .forEachIndexed { index, param ->
            if ("(" in param) {
                println("yes")
                Text(param.substringBefore("(") + "(")
                HighLightedArgs(param.substringAfter("(").substringBeforeLast(")"))
                Text(")" + param.substringAfter("(").substringAfterLast(")"))
            } else if (param.firstOrNull() == '"' && param.lastOrNull() == '"') {
                SpanText(param, Modifier.color(ColorScheme.string))
            } else if (param.all { it.isDigit() || it == '.' || it == 'f' }) {
                SpanText(param, Modifier.color(ColorScheme.number))
            } else if ("." in param && "(" !in param) {
                val startText = param.substringBeforeLast(".")
                if (startText.toDoubleOrNull() != null) {
                    SpanText(startText, Modifier.color(ColorScheme.number))
                } else {
                    Text(startText)
                }
                Text(".")
                SpanText(param.substringAfterLast("."), Modifier.color(ColorScheme.property))
            } else if (param.startsWith("0x")) {
                SpanText(param, Modifier.color(ColorScheme.number))
            } else {
                Text(param)
            }

            if (index < commaCount) {
                Text(", ")
            }
        }
}