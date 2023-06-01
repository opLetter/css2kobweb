package io.github.opletter.css2kobweb.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.dom.ref
import com.varabyte.kobweb.compose.dom.registerRefScope
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.toAttrs
import io.github.opletter.css2kobweb.CodeBlock
import io.github.opletter.css2kobweb.CodeElement
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Pre

@Composable
fun KotlinCode(code: List<CodeBlock>, modifier: Modifier = Modifier) {
    Pre(Modifier.margin(0.px).then(modifier).toAttrs()) {
        registerRefScope(ref(code) {
            it.innerHTML = code.convertToHtml()
        })
    }
}

private object ColorScheme {
    val keyword = Color.rgb(0xcF8E6D)
    val property = Color.rgb(0xC77DBB)
    val extensionFun = Color.rgb(0x56A8F5)
    val string = Color.rgb(0x6AAB73)
    val number = Color.rgb(0x2AACB8)
    val namedArg = Color.rgb(0x56C1D6)
}

private fun colorForType(type: CodeElement): Color = when (type) {
    CodeElement.Keyword -> ColorScheme.keyword
    CodeElement.Property -> ColorScheme.property
    CodeElement.ExtensionFun -> ColorScheme.extensionFun
    CodeElement.String -> ColorScheme.string
    CodeElement.Number -> ColorScheme.number
    CodeElement.NamedArg -> ColorScheme.namedArg
    CodeElement.Plain -> error("Should not be plain")
}

private fun List<CodeBlock>.convertToHtml(): String = joinToString("") { block ->
    if (block.type == CodeElement.Plain) {
        block.text.htmlEscape()
    } else {
        """<span class="silk-span-text" style="color: ${colorForType(block.type)};">${block.text.htmlEscape()}</span>"""
    }
}

private fun String.htmlEscape(): String = this.replace("&", "&amp;").replace("<", "&lt;")