package io.github.opletter.css2kobweb

import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

fun createUnitMappings() {
    println(Paths.get("").absolutePathString())
    val mappings = File("parsing/src/jvmMain/resources/units.txt").readText()
        .split("\n")
        .filter { it.isNotBlank() }
        .associate {
            val key = it.substringAfter("inline val ").substringBefore(' ')
            val rawStr = it.substringAfter('"').substringBefore('"')

            rawStr to key
        }

    mappings.forEach { (k, v) ->
        println("\"$k\" to \"$v\",")
    }
}

fun getColors() {
    File("parsing/src/jvmMain/resources/colors.txt").readLines()
        .joinToString("\", \"", "\"", "\"") {
            it.substringAfter("val ").substringBefore(" get()")
        }.also { println(it) }
}