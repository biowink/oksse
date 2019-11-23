plugins {
    kotlin("jvm")
    application
}

val okHttpVersion: String by ext
val cliktVersion: String by ext
val gsonVersion: String by ext

dependencies {
    implementation(project(":jvm-okhttp"))

    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))

    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")
    implementation("com.github.ajalt:clikt:$cliktVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
}

application {
    mainClassName = "com.helloclue.sse.sample.MainKt"
}
