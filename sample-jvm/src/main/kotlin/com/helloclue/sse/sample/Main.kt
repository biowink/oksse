package com.helloclue.sse.sample

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option

fun main(args: Array<String>) {
    object : NoRunCliktCommand(name = "sample-jvm") {

        val url: String by argument(
            help = "Endpoint URL")

        val authToken: String? by option("-t", "--token",
            help = "Authorization Token")

        init {
            when {
                args.isEmpty() -> {
                    echo(getFormattedHelp())
                }
                else -> {
                    main(args) // parse args
                    sseLoop(url, authToken)
                }
            }
        }
    }
}
