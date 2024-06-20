package io.github.opletter.css2kobweb.functions

import io.github.opletter.css2kobweb.Arg

internal fun Arg.Function.Companion.transition(
    property: Arg,
    duration: Arg? = null,
    remainingArgs: List<Arg> = emptyList(),
): Arg.Function {
    val firstParams = listOfNotNull(property, duration)

    return when (remainingArgs.size) {
        0, 2 -> transitionOf(firstParams + remainingArgs)
        1 -> {
            val thirdArg = remainingArgs.single()
                .let { if (it is Arg.UnitNum) Arg.NamedArg("delay", it) else it }
            transitionOf(firstParams + thirdArg)
        }

        else -> error("Invalid transition")
    }
}

private fun transitionOf(args: List<Arg>): Arg.Function {
    val function = if (args.first() is Arg.Function) "group" else "of"
    return Arg.Function("Transition.$function", args)
}