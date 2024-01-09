package lol.unsession.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import lol.unsession.db.repo.UsersRepositoryImpl
import lol.unsession.security.user.User
import lol.unsession.utils.getLogger

val logger = getLogger("Routing")

fun Application.configureRouting() {
    val usersRepo = UsersRepositoryImpl()
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondText("It works!")
        }
        route("/parser") {
            post("/sdo/answers") {
                call.respondText("Hello, world!")
            }
        }
        post("/register") {
            val loginData = call.receive<User.UserLoginData>()
            usersRepo.tryRegisterUser(loginData, call.request.origin.remoteHost, onSuccess = {
                val token = createToken(it)
                logger.info("Registered user ${loginData.username}; ${call.request.origin.remoteHost}")
                call.respond(hashMapOf("token" to token))
            }, usernameExists = {
                logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost} [user-username-exists]")
                call.respond(HttpStatusCode.Conflict)
            }, userExists = {
                logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost} [user-id-exists]")
                call.respond(HttpStatusCode.Conflict)
            }, onFailure = {
                logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost}")
            })
        }

        authenticate("user-auth") {
            get("/authtest") {
                    call.respondText("Hello, world!")
            }

            get("/user") {
                val user = call.authentication.principal<JWTPrincipal>()!!.payload
                val userDto = usersRepo.getUser(user.getClaim("userId").asInt())!!
                call.respond(userDto)
            }


        }
    }
}
