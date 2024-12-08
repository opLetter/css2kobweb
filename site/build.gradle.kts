import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication

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
    js().compilerOptions {
        target = "es2015"
        freeCompilerArgs.add("-Xir-generate-inline-anonymous-functions")
    }

    sourceSets {
        jsMain.dependencies {
            implementation(libs.compose.runtime)
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