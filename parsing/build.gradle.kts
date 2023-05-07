plugins {
    alias(libs.plugins.kotlin.multiplatform)
    application
}

group = "io.github.opletter.css2kobweb"
version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        withJava()
    }
    js(IR) {
        browser()
    }
}

application {
    mainClass.set("io.github.opletter.css2kobweb.MainKt")
}