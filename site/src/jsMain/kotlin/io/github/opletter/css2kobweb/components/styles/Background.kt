package io.github.opletter.css2kobweb.components.styles

import com.varabyte.kobweb.compose.css.CSSBackground
import com.varabyte.kobweb.compose.css.CSSPosition
import com.varabyte.kobweb.compose.css.functions.RadialGradient
import com.varabyte.kobweb.compose.css.functions.linearGradient
import com.varabyte.kobweb.compose.css.functions.radialGradient
import com.varabyte.kobweb.compose.css.functions.toImage
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.background
import com.varabyte.kobweb.silk.components.style.ComponentStyle
import com.varabyte.kobweb.silk.components.style.base
import org.jetbrains.compose.web.css.deg
import org.jetbrains.compose.web.css.percent

// Image courtesy of gradientmagic.com
// https://www.gradientmagic.com/collection/popular/gradient/1583693118025
// translated with css2kobweb :)
val BackgroundGradientStyle by ComponentStyle.base {
    Modifier
        .background(
            CSSBackground(
                image = linearGradient(90.deg, Color.rgb(34, 222, 237), Color.rgb(135, 89, 215)).toImage()
            ),
            CSSBackground(
                image = radialGradient(RadialGradient.Shape.Circle, CSSPosition(75.percent, 99.percent)) {
                    add(Color.rgba(243, 243, 243, 0.04f), 0.percent)
                    add(Color.rgba(243, 243, 243, 0.04f), 50.percent)
                    add(Color.rgba(37, 37, 37, 0.04f), 50.percent)
                    add(Color.rgba(37, 37, 37, 0.04f), 100.percent)
                }.toImage()
            ),
            CSSBackground(
                image = radialGradient(RadialGradient.Shape.Circle, CSSPosition(15.percent, 16.percent)) {
                    add(Color.rgba(99, 99, 99, 0.04f), 0.percent)
                    add(Color.rgba(99, 99, 99, 0.04f), 50.percent)
                    add(Color.rgba(45, 45, 45, 0.04f), 50.percent)
                    add(Color.rgba(45, 45, 45, 0.04f), 100.percent)
                }.toImage()
            ),
            CSSBackground(
                image = radialGradient(RadialGradient.Shape.Circle, CSSPosition(86.percent, 7.percent)) {
                    add(Color.rgba(40, 40, 40, 0.04f), 0.percent)
                    add(Color.rgba(40, 40, 40, 0.04f), 50.percent)
                    add(Color.rgba(200, 200, 200, 0.04f), 50.percent)
                    add(Color.rgba(200, 200, 200, 0.04f), 100.percent)
                }.toImage()
            ),
            CSSBackground(
                image = radialGradient(RadialGradient.Shape.Circle, CSSPosition(66.percent, 97.percent)) {
                    add(Color.rgba(36, 36, 36, 0.04f), 0.percent)
                    add(Color.rgba(36, 36, 36, 0.04f), 50.percent)
                    add(Color.rgba(46, 46, 46, 0.04f), 50.percent)
                    add(Color.rgba(46, 46, 46, 0.04f), 100.percent)
                }.toImage()
            ),
            CSSBackground(
                image = radialGradient(RadialGradient.Shape.Circle, CSSPosition(40.percent, 91.percent)) {
                    add(Color.rgba(251, 251, 251, 0.04f), 0.percent)
                    add(Color.rgba(251, 251, 251, 0.04f), 50.percent)
                    add(Color.rgba(229, 229, 229, 0.04f), 50.percent)
                    add(Color.rgba(229, 229, 229, 0.04f), 100.percent)
                }.toImage()
            )
        )
}