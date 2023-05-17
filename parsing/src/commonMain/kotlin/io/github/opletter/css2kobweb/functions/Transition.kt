package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg
import io.github.opletter.css2kobweb.kebabToPascalCase

internal fun Arg.Function.Companion.transition(transition: String): Arg.Function {
    val params = transition.split(" ").filter { it.isNotBlank() }
    val firstParams = listOf(Arg.Literal("\"${params[0]}\""), Arg.UnitNum.of(params[1])).toTypedArray()

    return when (params.size) {
        2 -> transitionOf(*firstParams)
        4 -> {
            transitionOf(
                *firstParams,
                Arg.Property("TransitionTimingFunction", kebabToPascalCase(params[2])),
                Arg.UnitNum.of(params[3]),
            )
        }

        3 -> {
            val delay = Arg.UnitNum.ofOrNull(params[2])
            if (delay == null) {
                transitionOf(
                    *firstParams,
                    Arg.Property("TransitionTimingFunction", kebabToPascalCase(params[2])),
                )
            } else {
                transitionOf(
                    *firstParams,
                    Arg.NamedArg("delay", delay),
                )
            }
        }

        else -> error("Invalid transition: $transition")
    }
}

private fun transitionOf(vararg args: Arg): Arg.Function = Arg.Function("CSSTransition", args.toList())