package apu.unsession.application

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureHTTP() {
    install(DefaultHeaders) {
        header("X-Engine", "Tomcat") // Ktor
    }
    routing {
        openAPI(path = "openapi")
    }
    routing {
        swaggerUI(path = "openapi")
    }
    install(CORS) {
        // allow all
        anyHost()
        // allow any headers
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.AccessControlAllowHeaders)
        allowHeader(HttpHeaders.AccessControlAllowMethods)
        allowHeader(HttpHeaders.AccessControlAllowCredentials)
        allowHeader(HttpHeaders.AccessControlMaxAge)
        // allow any methods
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
}
