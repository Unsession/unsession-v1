package lol.unsession.features.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.datetime.Clock
import lol.unsession.application.LoginResponse
import lol.unsession.application.logger
import lol.unsession.features.db.Repository
import lol.unsession.features.security.roles.Access
import lol.unsession.features.user.User
import lol.unsession.utils.getLogger
import java.util.*

fun Application.configureSecurity() {
    authenticationPlugin()
    routing {
        route("/users") {
            post("/login") {
                val loginData = call.receive<User.UserLoginData>()

                val user = Repository.Users.getUser(loginData.email)
                if (user == null) call.respond(HttpStatusCode.Unauthorized)
                if (user!!.isBanned) call.respond(HttpStatusCode.Forbidden, user.banData!!)

                val storedLoginData = user.userLoginData
                if (storedLoginData == null) call.respond(HttpStatusCode.Unauthorized)

                if (!Crypto.checkPassword(
                        inputPassword = loginData.password,
                        salt = storedLoginData!!.salt!!,
                        storedHash = storedLoginData.password
                    )
                ) call.respond(HttpStatusCode.Unauthorized)

                val token = createToken(user)

                call.respond(LoginResponse(token, user))
            }
            post("/register") {
                try {
                    val loginData = call.receive<User.UserLoginData>()
                    if (!loginData.validate()) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid login data")
                    }
                    Repository.Users.tryRegisterUser(loginData, call.request.origin.remoteAddress, onSuccess = {
                        val token = createToken(it)
                        logger.info("Registered user ${loginData.username}; ${call.request.origin.remoteHost}; ${call.request.headers}")
                        call.respond(LoginResponse(token, it))
                    }, usernameExists = {
                        logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost} [user-username-exists]")
                        call.respond(HttpStatusCode.Conflict)
                    }, userExists = {
                        logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost} [user-id-exists]")
                        call.respond(HttpStatusCode.Conflict)
                    }, onFailure = {
                        logger.error("Failed to register user ${loginData.username}; ${call.request.origin.remoteHost}")
                    })
                } catch (e: Exception) {
                    logger.error(e)
                }
            }
        }
    }
}

fun createToken(user: User): String {
    return JWT.create()
        .withIssuer("unsession")
        .withClaim("userId", user.id)
        .withClaim("username", user.name)
        .withArrayClaim("permissions", user.permissions.map { it.name }.toTypedArray())
        .withExpiresAt(Date(Clock.System.now().toEpochMilliseconds() + System.getenv("tokenLifetime").toInt()))
        .sign(Algorithm.HMAC512(System.getenv("secret")))
}

fun getPermissionsFromToken(token: String): HashSet<Access> {
    val decodedToken = JWT.decode(token)
    val permissions = decodedToken.getClaim("permissions").asArray(String::class.java)
    val permissionsSet = HashSet<Access>()
    permissions.forEach {
        permissionsSet.add(Access.valueOf(it))
    }
    return permissionsSet
}

fun ApplicationCall.getPermissions(): HashSet<Access>? {
    val token = this.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return null
    return getPermissionsFromToken(token)
}

suspend fun RoutingContext.verify(vararg access: Access) {
    val call = this.call
    val userId = call.getUserDataFromToken().id
    val permissions = Repository.Users.getUser(userId)?.permissions
    if (permissions == null) {
        call.respond(HttpStatusCode.Forbidden)
        return
    }
    if (!permissions.containsAll(access.toList())) {
        call.respond(HttpStatusCode.Forbidden)
        return
    }
}

data class TokenUserData(
    val id: Int,
    val name: String,
    val permissions: HashSet<Access>
)

fun ApplicationCall.getUserDataFromToken(): TokenUserData {
    val token = this.request.headers["Authorization"]?.removePrefix("Bearer ") ?: throw Exception("No token")
    val decodedToken = JWT.decode(token)
    val userId = decodedToken.getClaim("userId").asInt()
    val username = decodedToken.getClaim("username").asString()
    val permissions = getPermissionsFromToken(token)
    return TokenUserData(userId, username, permissions)
}

fun Application.authenticationPlugin() {
    val secret = System.getenv("secret")

    install(Authentication) {
        jwt("user-auth") {
            val logger = getLogger("validator")
            verifier(JWT
                .require(Algorithm.HMAC512(secret))
                .withIssuer("unsession")
                .build())
            validate { credential ->
                try {
                    val user = Repository.Users.getUser(credential.payload.getClaim("userId").asInt())
                    logger.debug("requested from token: $user")
                    if (credential.payload.getClaim("username").asString() != "" && credential.expiresAt!!.time > Clock.System.now().toEpochMilliseconds()
                    ) {
                        JWTPrincipal(credential.payload)
                    } else {
                        logger.error("verification failed")
                        null
                    }
                } catch (e: Exception) {
                    logger.error("verification failed with exception: ${e.message}")
                    null
                }
            }
            challenge { defaultScheme, realm ->
                logger.debug("Token is not valid or has expired; scheme=$defaultScheme realm=\"$realm\", ${this.call.request.headers.entries()}")
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired; scheme=$defaultScheme realm=\"$realm\"")
            }
        }
        basic("admin-auth") {
            validate { credentials ->
                if (credentials.name == "admin" && credentials.password == System.getenv("adminPassword")) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
}