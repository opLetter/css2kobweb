package io.github.opletter.css2kobweb.pages

import androidx.compose.runtime.*
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
import com.varabyte.kobweb.silk.components.forms.ButtonSize
import com.varabyte.kobweb.silk.components.icons.fa.FaTrashCan
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.base
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.palette.background
import com.varabyte.kobweb.silk.theme.colors.palette.color
import com.varabyte.kobweb.silk.theme.colors.palette.toPalette
import io.github.opletter.css2kobweb.CodeBlock
import io.github.opletter.css2kobweb.components.layouts.PageLayout
import io.github.opletter.css2kobweb.components.widgets.KotlinCode
import io.github.opletter.css2kobweb.css2kobwebAsCode
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.fr
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import kotlin.time.Duration.Companion.milliseconds

val TextAreaStyle = CssStyle.base {
    Modifier
        .fillMaxSize()
        .padding(topBottom = 0.5.cssRem, leftRight = 1.cssRem)
        .borderRadius(bottomLeft = 8.px, bottomRight = 8.px)
        .resize(Resize.None)
        .overflow { y(Overflow.Auto) }
        .overflowWrap(OverflowWrap.Normal)
        .styleModifier { property("tab-size", 4) }
        .backgroundColor(colorMode.toPalette().color)
        .color(colorMode.toPalette().background)
}

val TextAreaLabelBarStyle = CssStyle.base {
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
                TextAreaHeader("CSS Input") {
                    HeaderButton({ cssInput = "" }, Modifier.ariaLabel("clear text")) {
                        FaTrashCan()
                    }
                }
                TextArea(
                    cssInput,
                    TextAreaStyle.toModifier()
                        .outline { style(LineStyle.None) }
                        .border { style(LineStyle.None) }
                        .ariaLabel("CSS Input")
                        .toAttrs {
                            spellCheck(false)
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
            // outer column needed for child's flex-grow (while keeping overflowY)
            Column(Modifier.overflow { x(Overflow.Auto) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(0.px) // set to make overflow work
                        .flexGrow(1)
                ) {
                    TextAreaHeader("Kobweb Code") {
                        CopyTextButton(outputCode.joinToString(""))
                    }
                    KotlinCode(outputCode, TextAreaStyle.toModifier())
                }
            }
        }
    }
}

@Composable
fun TextAreaHeader(label: String, rightSideContent: @Composable () -> Unit) {
    Row(
        TextAreaLabelBarStyle.toModifier().columnGap(1.cssRem),
        verticalAlignment = Alignment.CenterVertically
    ) {
        H2(Modifier.fillMaxWidth().toAttrs()) {
            Text(label)
        }

        rightSideContent()
    }
}

@Composable
fun HeaderButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Button({ onClick() }, modifier.fontSize(1.cssRem), size = ButtonSize.SM) {
        content()
    }
}

@Composable
fun CopyTextButton(textToCopy: String) {
    var buttonText by remember { mutableStateOf("Copy") }
    HeaderButton(
        {
            window.navigator.clipboard.writeText(textToCopy)
            buttonText = "Copied!"
            window.setTimeout({ buttonText = "Copy" }, 2000)
        }
    ) { Text(buttonText) }
}