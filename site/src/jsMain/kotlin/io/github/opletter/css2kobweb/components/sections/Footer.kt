package io.github.opletter.css2kobweb.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.AlignSelf
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.alignSelf
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.rowGap
import com.varabyte.kobweb.compose.ui.modifiers.textAlign
import com.varabyte.kobweb.silk.components.icons.fa.FaGithub
import com.varabyte.kobweb.silk.components.navigation.Link
import com.varabyte.kobweb.silk.components.style.ComponentStyle
import com.varabyte.kobweb.silk.components.style.base
import com.varabyte.kobweb.silk.components.style.toModifier
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.cssRem

val FooterStyle by ComponentStyle.base {
    Modifier
        .margin(topBottom = 1.cssRem)
        .alignSelf(AlignSelf.Center)
        .textAlign(TextAlign.Center)
        .rowGap(0.1.cssRem)
}

@Composable
fun Footer(modifier: Modifier = Modifier) {
    Column(
        FooterStyle.toModifier().then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FaGithub()
            SpanText(" This site is ")
            Link(path = "https://github.com/opLetter/css2kobweb", text = "open source")
        }
        Row {
            SpanText("Made with ")
            Link(path = "https://github.com/varabyte/kobweb", text = "Kobweb")
        }
    }
}