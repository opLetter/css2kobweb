package io.github.opletter.css2kobweb

enum class CodeElement {
    Plain, Keyword, Property, ExtensionFun, String, Number, NamedArg
}

class CodeBlock(val text: String, val type: CodeElement) {
    override fun toString(): String = text
}

fun css2kobwebAsCode(rawCSS: String, extractOutCommonModifiers: Boolean = true): List<CodeBlock> {
    // fold adjacent code blocks of the same type into one to hopefully improve rendering performance
    // we use a mutable list as otherwise this can become a performance bottleneck
    return css2kobweb(rawCSS, extractOutCommonModifiers).asCodeBlocks().toMutableList().apply {
        var i = 0
        while (i < size - 1) {
            if (this[i].type == this[i + 1].type) {
                this[i] = CodeBlock(this[i].text + this[i + 1].text, CodeElement.Plain)
                removeAt(i + 1)
            } else {
                i++
            }
        }
    }
}