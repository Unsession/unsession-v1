package lol.unsession.features.application

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import lol.unsession.features.db.configureDatabases
import lol.unsession.features.security.configureSecurity

// http://51.250.13.148:7575/

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 7575,
        host = System.getenv("HOST") ?: "",
        module = Application::module
    )
        .start(wait = true)
}

fun Application.module() {
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureSecurity()
    configureRouting()
}
