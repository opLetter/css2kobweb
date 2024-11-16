import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
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
    js {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions.target = "es2015"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
        }

        jsMain.dependencies {
            implementation(libs.compose.html.core)
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
            implementation(libs.silk.icons.fa)
            implementation(projects.parsing)
        }
    }
}

composeCompiler {
    includeTraceMarkers = false
}