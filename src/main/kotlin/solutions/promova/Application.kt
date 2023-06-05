package solutions.promova

import io.ktor.server.application.*

import solutions.promova.plugins.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    val version = environment.config.property("ktor.application.version").getString()

    configureRouting(version)
}
