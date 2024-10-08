package apu.unsession.application

import apu.unsession.features.blockchain.auth.configureBlockchain
import apu.unsession.features.db.configureDatabases
import apu.unsession.features.security.configureSecurity
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

// http://51.250.13.148:7575

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
    configureAdminRouting()
    configureBlockchain()
}
