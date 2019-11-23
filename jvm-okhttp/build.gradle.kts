import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("platform.jvm")
}

val coroutinesVersion: String by ext
val okHttpVersion: String by ext
val okioVersion: String by ext

dependencies {
    expectedBy(project(":common"))

    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))

    // using `compile` instead of `api` because: https://youtrack.jetbrains.com/issue/KT-28223#focus=streamItem-27-3164342-0-0
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    compile("com.squareup.okhttp3:okhttp:$okHttpVersion")
    compile("com.squareup.okio:okio:$okioVersion")
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
