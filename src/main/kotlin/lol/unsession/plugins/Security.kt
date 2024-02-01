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
    }
}

fun createToken(user: User): String {
    return JWT.create()
        .withIssuer("unsession")
        .withClaim("userId", user.id)
        .withArrayClaim("permissions", user.permissions.map { it.name }.toTypedArray())
        .withClaim("username", user.name)
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
    val permissions = call.getPermissions()
    if (permissions == null) {
        call.respond(HttpStatusCode.Forbidden)
        return
    }
    if (!permissions.containsAll(access.toList())) {
        call.respond(HttpStatusCode.Forbidden)
        return
    }
}
