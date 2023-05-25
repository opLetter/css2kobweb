package io.github.opletter.css2kobweb.components.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.text.SpanText
import io.github.opletter.css2kobweb.CodeBlock
import io.github.opletter.css2kobweb.CodeElement
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text

@Composable
fun KotlinCode(code: List<CodeBlock>, modifier: Modifier = Modifier) {
    Pre(Modifier.margin(0.px).then(modifier).toAttrs()) {
        key(code.size) { // fixes performance issues
            code.forEach { HighlightedCode(it) }
        }
    }
}

object ColorScheme {
    val keyword = Color.rgb(0xcF8E6D)
    val property = Color.rgb(0xC77DBB)
    val extensionFun = Color.rgb(0x56A8F5)
    val string = Color.rgb(0x6AAB73)
    val number = Color.rgb(0x2AACB8)
    val namedArg = Color.rgb(0x56C1D6)
}

@Composable
private fun HighlightedCode(text: CodeBlock) {
    when (text.type) {
        CodeElement.Keyword -> ColorScheme.keyword
        CodeElement.Property -> ColorScheme.property
        CodeElement.ExtensionFun -> ColorScheme.extensionFun
        CodeElement.String -> ColorScheme.string
        CodeElement.Number -> ColorScheme.number
        CodeElement.NamedArg -> ColorScheme.namedArg
        CodeElement.Plain -> {
            Text(text.text)
            return
        }
    }.let { SpanText(text.text, Modifier.color(it)) }
}