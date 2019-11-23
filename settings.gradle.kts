rootProject.name = "sse"

include(":common")
include(":jvm-okhttp")
include(":sample-jvm")

val kotlinVersion = "1.3.70"
gradle.projectsLoaded {
    allprojects {
        extra["kotlinVersion"] = kotlinVersion
        extra["coroutinesVersion"] = "1.3.3"
        extra["okHttpVersion"] = "3.12.0"
        extra["okioVersion"] = "2.2.2"
        extra["cliktVersion"] = "1.5.0"
        extra["gsonVersion"] = "2.8.5"
        repositories {
            jcenter()
            kotlinx()
        }
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        kotlinx()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                kotlin("jvm"),
                kotlin("platform.common"),
                kotlin("platform.jvm"),
                kotlin("platform.android"),
                kotlin("platform.js")
                -> useKotlinModule("gradle-plugin")
            }
        }
    }
}

fun kotlin(module: String) = "org.jetbrains.kotlin.$module"

fun PluginResolveDetails.useKotlinModule(module: String) =
    useModule("org.jetbrains.kotlin:kotlin-$module:${requested.version ?: kotlinVersion}")

fun RepositoryHandler.kotlinx() =
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
