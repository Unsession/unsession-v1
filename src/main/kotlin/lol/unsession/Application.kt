package lol.unsession

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.datetime.Clock
import lol.unsession.plugins.*
import java.net.InetAddress

// http://51.250.13.148:7575/
fun main() {
    if (!isDebug()) {
        embeddedServer(
            Netty,
            port = System.getenv("app_port")!!.toInt(),
            host = System.getenv("app_ip")!!,
            module = Application::module
        )
            .start(wait = true)
    } else {
        embeddedServer(
            Netty,
            port = 5050,
            host = InetAddress.getLocalHost().hostAddress,
            module = Application::module
        )
            .start(wait = true)
    }
}

fun Application.module() {
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    if (!isDebug()) {
        configureDatabases()
    } else {
        configureDatabasesLocalhost()
    }
    configureAdministration()
    configureSecurity()
    configureRouting()
}
