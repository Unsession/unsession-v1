package lol.unsession

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import lol.unsession.plugins.*

fun main() {
    embeddedServer(Netty, port = 7777, host = "localhost", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureAdministration()
    configureRouting()
}
