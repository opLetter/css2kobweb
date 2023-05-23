package io.github.opletter.css2kobweb.components.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.text.SpanText
import io.github.opletter.css2kobweb.CodeBlock
import io.github.opletter.css2kobweb.CodeElement
import io.github.opletter.css2kobweb.pages.ColorScheme
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text

@Composable
fun KotlinCode(code: List<CodeBlock>, syntaxHighlight: Boolean) {
    Pre(Modifier.margin(0.px).toAttrs()) {
        if (!syntaxHighlight) {
            Text(code.joinToString(""))
        } else {
            key(code.size) { // fixes performance issues
                code.forEach { HighlightedCode(it) }
            }
        }
    }
}

@Composable
private fun HighlightedCode(text: CodeBlock) {
    when (text.type) {
        CodeElement.Keyword -> ColorScheme.keyword
        CodeElement.Property -> ColorScheme.property
        CodeElement.ExtensionFun -> ColorScheme.function
        CodeElement.String -> ColorScheme.string
        CodeElement.Number -> ColorScheme.number
        CodeElement.NamedArg -> ColorScheme.namedArg
        CodeElement.Plain -> {
            Text(text.text)
            null
        }
    }?.let { SpanText(text.text, Modifier.color(it)) }
}