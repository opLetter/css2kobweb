import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kobweb.application)
}

group = "io.github.opletter.css2kobweb"
version = "1.0-SNAPSHOT"

kobweb {
    app {
        index {
            description = "Convert CSS to Kobweb modifiers & styles"
        }
    }
}

kotlin {
    configAsKobwebApplication("css2kobweb")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
                implementation(libs.kobweb.core)
                implementation(libs.kobweb.silk.core)
                implementation(libs.kobweb.silk.icons.fa)
                implementation(projects.parsing)
             }
        }
    }
}