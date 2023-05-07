package io.github.opletter.css2kobweb.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.css.Resize
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.*
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.theme.breakpoint.breakpointFloor
import io.github.opletter.css2kobweb.components.widgets.KotlinCode
import io.github.opletter.css2kobweb.css2kobweb
import kotlinx.browser.window
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea

object ColorScheme {
    val background = Color.rgb(0x1E1F22)
    val color = Color.rgb(0xBCBEC4)
    val keyword = Color.rgb(0xcF8E6D)
    val property = Color.rgb(0xC77DBB)
    val function = Color.rgb(0x56A8F5)
    val string = Color.rgb(0x6AAB73)
    val number = Color.rgb(0x2AACB8)
}

@Page
@Composable
fun HomePage() {
    var textValue by remember { mutableStateOf("hello: world") }
    var outputText by remember { mutableStateOf("") }

    var syntaxHighlight by remember { mutableStateOf(true) }
    Column(
        Modifier.fillMaxSize().rowGap(0.5.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            Modifier
                .height(75.percent)
                .width(90.percent)
                .columnGap(1.cssRem)
        ) {
            val textAreaModifier = Modifier
                .fillMaxSize()
                .padding(1.cssRem)
                .borderRadius(8.px)
                .resize(Resize.None)
                .overflow(Overflow.Auto)
                .styleModifier { property("tab-size", 4) }
                .backgroundColor(ColorScheme.background)
                .color(ColorScheme.color)
                .attrsModifier {
                    attr("spellcheck", "false")
                    attr("data-enable-grammarly", "false")
                }
            TextArea(
                textValue,
                textAreaModifier.toAttrs {
                    onInput { textValue = it.value }
                }
            )
            Box(textAreaModifier.position(Position.Relative)) {
                KotlinCode(outputText, syntaxHighlight)
                Button(
                    {
                        @Suppress("UNUSED_VARIABLE") // needed & used in js call
                        val textToCopy = outputText
                        js("navigator.clipboard.writeText(textToCopy)")
                    },
                    Modifier
                        .position(Position.Absolute)
                        .right(1.cssRem)
                ) {
                    Text("Copy")
                }
            }
        }
        Button({ outputText = css2kobweb(textValue) }) {
            SpanText("css 2 kobweb")
        }
        Label {
            CheckboxInput(syntaxHighlight, Modifier.toAttrs { onChange { syntaxHighlight = it.value } })
            Text("Syntax Highlighting")
        }
    }
}