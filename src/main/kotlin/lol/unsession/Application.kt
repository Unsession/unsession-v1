package lol.unsession

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import lol.unsession.plugins.*

// http://51.250.13.148:7575/

fun main() {
    embeddedServer(
        Netty,
        port = 5050,
        host = "10.128.0.30",
        module = Application::module
    )
        .start(wait = true)
}

fun Application.module() {
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureAdministration()
    configureSecurity()
    configureRouting()
}
