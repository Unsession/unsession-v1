package lol.unsession.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import lol.unsession.createFileAndWrite
import java.io.File

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    install(CORS) {
        allowMethod(HttpMethod.Post)
        anyHost()
    }
    routing {
        get("/test") {
            call.respondText("Hello, world!")
        }
        route("/parser") {
            post("/sdo/answers") {
                createFileAndWrite("S:\\Projects\\Intelij\\unsessionserver\\src\\main\\kotlin\\lol\\unsession\\parsers\\sdo.html") {
                    runBlocking {
                        it.writeText(call.receiveText())
                    }
                }
            }
        }
    }
}
