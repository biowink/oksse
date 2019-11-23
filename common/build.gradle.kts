import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("platform.common")
}

val coroutinesVersion: String by ext

dependencies {
    implementation(kotlin("stdlib-common"))
    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
}

tasks {
    withType<KotlinCompile<*>>().configureEach {
        kotlinOptions {
            freeCompilerArgs += listOf(
                // for [kotlinx.coroutines.channels]
                "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
            )
        }
    }
}
