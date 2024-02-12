package lol.unsession.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import lol.unsession.db.Repository
import lol.unsession.security.permissions.Access
import lol.unsession.security.user.User
import lol.unsession.utils.getLogger
import java.util.*

fun Application.configureSecurity() {
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
                logger.error("Даём пользователю по голове и посылаем нах**.401")
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
    }
    if (!permissions!!.containsAll(access.toList())) {
        call.respond(HttpStatusCode.Forbidden)
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