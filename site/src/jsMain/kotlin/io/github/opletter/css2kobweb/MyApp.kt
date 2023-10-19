package io.github.opletter.css2kobweb

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.marginBlock
import com.varabyte.kobweb.compose.ui.modifiers.minHeight
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.silk.SilkApp
import com.varabyte.kobweb.silk.components.layout.Surface
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.components.style.common.SmoothColorStyle
import com.varabyte.kobweb.silk.components.style.toModifier
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.init.registerStyleBase
import com.varabyte.kobweb.silk.theme.colors.palette.background
import com.varabyte.kobweb.silk.theme.colors.palette.button
import com.varabyte.kobweb.silk.theme.colors.palette.color
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.vh

@InitSilk
fun updateTheme(ctx: InitSilkContext) {
    with(ctx.stylesheet) {
        registerStyleBase("body") {
            Modifier.fontFamily("system-ui", "Segoe UI", "Tahoma", "Helvetica", "sans-serif")
        }
        registerStyle("h1") {
            base {
                Modifier
                    .fontSize(2.5.cssRem)
                    .marginBlock(0.5.cssRem, 0.5.cssRem)
            }
            Breakpoint.MD {
                Modifier.fontSize(3.cssRem)
            }
        }
        registerStyleBase("h2") {
            Modifier.marginBlock(0.cssRem, 0.cssRem)
        }
    }

    // https://coolors.co/1e1f22-3f334d-8bdbe2-f97068-f7e733
    // todo: unique color modes
    ctx.theme.palettes.light.apply {
        background = Color.rgb(188, 190, 196)
        color = Color.rgb(30, 31, 34)
        button.set(
            default = Color.rgb(0xF97068),
            hover = Color.rgb(0xF97068).darkened(0.1f),
            focus = ctx.theme.palettes.light.button.focus,
            pressed = Color.rgb(0xF97068).darkened(0.25f),
        )
    }
}

@App
@Composable
fun MyApp(content: @Composable () -> Unit) {
    SilkApp {
        Surface(SmoothColorStyle.toModifier().minHeight(100.vh)) {
            content()
        }
    }
}