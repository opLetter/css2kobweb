package io.github.opletter.css2kobweb.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.css.Resize
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.style.ComponentStyle
import com.varabyte.kobweb.silk.components.style.base
import com.varabyte.kobweb.silk.components.style.toAttrs
import com.varabyte.kobweb.silk.components.style.toModifier
import com.varabyte.kobweb.silk.theme.toSilkPalette
import io.github.opletter.css2kobweb.CodeBlock
import io.github.opletter.css2kobweb.components.widgets.KotlinCode
import io.github.opletter.css2kobweb.css2kobwebAsCode
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

val TextAreaStyle by ComponentStyle.base {
    Modifier
        .fillMaxSize()
        .padding(topBottom = 0.5.cssRem, leftRight = 1.cssRem)
        .borderRadius(bottomLeft = 8.px, bottomRight = 8.px)
        .resize(Resize.None)
        .overflow(Overflow.Auto)
        .styleModifier { property("tab-size", 4) }
        .backgroundColor(colorMode.toSilkPalette().color)
        .color(colorMode.toSilkPalette().background)
}

val TextAreaLabelStyle by ComponentStyle.base {
    Modifier
        .fillMaxWidth()
        .backgroundColor(Colors.Black)
        .padding(topBottom = 0.5.cssRem, leftRight = 1.cssRem)
        .color(Color.rgb(0x8bdbe2))
        .borderRadius(topLeft = 8.px, topRight = 8.px)
}

@Page
@Composable
fun HomePage() {
    var cssInput by remember { mutableStateOf("") }
    var outputCode: List<CodeBlock> by remember { mutableStateOf(emptyList()) }

    // get code here to avoid lagging onInput
    LaunchedEffect(cssInput) {
        try {
            outputCode = if (cssInput.isNotBlank()) css2kobwebAsCode(cssInput) else emptyList()
        } catch (_: Exception) {
            // ignore exceptions from invalid css
        }
    }

    Column(
        Modifier.fillMaxSize().rowGap(0.5.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        H1 {
            Text("CSS 2 Kobweb")
        }

        Row(
            Modifier
                .height(75.vh) // wanted to use 75% but that causes issues when kotlin code is too long
                .width(90.percent)
                .columnGap(1.cssRem)
        ) {
            Column(Modifier.fillMaxSize()) {
                Label(attrs = Modifier.display(DisplayStyle.Contents).toAttrs()) {
                    H2(TextAreaLabelStyle.toAttrs()) { Text("CSS Input") }
                    TextArea(
                        cssInput,
                        TextAreaStyle.toModifier()
                            .outlineStyle(LineStyle.None)
                            .borderStyle(LineStyle.None)
                            .toAttrs {
                                attr("spellcheck", "false")
                                attr("data-enable-grammarly", "false")
                                ref {
                                    it.focus()
                                    onDispose { }
                                }
                                onInput {
                                    cssInput = it.value
                                }
                            }
                    )
                }
            }
            Column(Modifier.fillMaxSize().minWidth(0.px)) { // minWidth needed for text overflow
                Row(
                    TextAreaLabelStyle.toModifier().columnGap(1.cssRem),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    H2(
                        Modifier
                            .fillMaxWidth()
                            .toAttrs()
                    ) { Text("Kobweb Code Output") }

                    var buttonText by remember { mutableStateOf("Copy") }
                    Button(
                        {
                            window.navigator.clipboard.writeText(outputCode.joinToString(""))
                            buttonText = "Copied!"
                            window.setTimeout({ buttonText = "Copy" }, 2500)
                        },
                        Modifier.padding(topBottom = 0.25.cssRem, leftRight = 0.75.cssRem)
                    ) {
                        Text(buttonText)
                    }
                }
                KotlinCode(outputCode, TextAreaStyle.toModifier())
            }
        }
    }
}