package io.github.opletter.css2kobweb.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.css.OverflowWrap
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
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.components.style.ComponentStyle
import com.varabyte.kobweb.silk.components.style.base
import com.varabyte.kobweb.silk.components.style.toAttrs
import com.varabyte.kobweb.silk.components.style.toModifier
import com.varabyte.kobweb.silk.theme.toSilkPalette
import io.github.opletter.css2kobweb.CodeBlock
import io.github.opletter.css2kobweb.components.layouts.PageLayout
import io.github.opletter.css2kobweb.components.widgets.KotlinCode
import io.github.opletter.css2kobweb.css2kobwebAsCode
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import kotlin.time.Duration.Companion.milliseconds

val TextAreaStyle by ComponentStyle.base {
    Modifier
        .fillMaxSize()
        .padding(topBottom = 0.5.cssRem, leftRight = 1.cssRem)
        .borderRadius(bottomLeft = 8.px, bottomRight = 8.px)
        .resize(Resize.None)
        .overflowY(Overflow.Auto)
        .overflowWrap(OverflowWrap.Normal)
        .styleModifier { property("tab-size", 4) }
        .backgroundColor(colorMode.toSilkPalette().color)
        .color(colorMode.toSilkPalette().background)
}

val TextAreaLabelBarStyle by ComponentStyle.base {
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
            if (cssInput.length > 5000) // debounce after semi-arbitrarily chosen number of characters
                delay(50.milliseconds)
            outputCode = if (cssInput.isNotBlank()) css2kobwebAsCode(cssInput) else emptyList()
        } catch (e: Exception) {
            // exceptions are expected while css is being typed / not invalid so only log them to verbose
            @Suppress("UNUSED_VARIABLE") val debug = e.stackTraceToString()
            js("console.debug(debug);") // why is console.debug not a thing?
            Unit
        }
    }

    PageLayout("CSS 2 Kobweb") {
        SimpleGrid(
            numColumns(1, md = 2),
            Modifier
                .fillMaxWidth()
                .flex(1)
                .gap(1.cssRem)
                .gridAutoRows { size(1.fr) }
        ) {
            Column(Modifier.fillMaxHeight()) {
                Label(attrs = Modifier.display(DisplayStyle.Contents).toAttrs()) {
                    H2(TextAreaLabelBarStyle.toAttrs()) { Text("CSS Input") }
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
            // outer column needed for child's flex-grow (while keeping overflowY)
            Column(Modifier.overflowX(Overflow.Auto)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(0.px) // set to make overflow work
                        .flexGrow(1)
                ) {
                    Row(
                        TextAreaLabelBarStyle.toModifier().columnGap(1.cssRem),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        H2(Modifier.fillMaxWidth().toAttrs()) {
                            Text("Kobweb Code")
                        }

                        CopyTextButton(outputCode.joinToString(""))
                    }
                    KotlinCode(outputCode, TextAreaStyle.toModifier())
                }
            }
        }
    }
}

@Composable
fun CopyTextButton(textToCopy: String) {
    var buttonText by remember { mutableStateOf("Copy") }
    Button(
        {
            window.navigator.clipboard.writeText(textToCopy)
            buttonText = "Copied!"
            window.setTimeout({ buttonText = "Copy" }, 2000)
        },
        Modifier
            .padding(topBottom = 0.25.cssRem, leftRight = 0.75.cssRem)
            .fontWeight(FontWeight.Bold)
    ) { Text(buttonText) }
}