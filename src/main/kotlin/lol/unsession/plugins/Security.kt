package lol.unsession.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimePeriod
import lol.unsession.db.repo.UsersRepositoryImpl
import lol.unsession.security.permissions.Access
import lol.unsession.security.user.User
import lol.unsession.security.utils.Crypto
import lol.unsession.utils.getLogger
import java.util.*
import kotlin.collections.HashSet
import kotlin.time.Duration

fun Application.configureSecurity() {
    val usersRepo = UsersRepositoryImpl()
    val secret = System.getenv("secret")
    val issuer = System.getenv("jwt.issuer")

    install(Authentication) {
        jwt("user-auth") {
            verifier(JWT
                .require(Algorithm.HMAC512(secret))
                .withIssuer(issuer)
                .build())
            validate { credential ->
                val user = usersRepo.getUser(credential.payload.getClaim("userId").asInt())
                if (credential.payload.getClaim("username").asString() != "" &&
                    user != null &&
                    credential.expiresAt!!.time > Clock.System.now().toEpochMilliseconds()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired; scheme=$defaultScheme realm=\"$realm\"")
            }
        }
    }

    routing {
        post("/login") {
            val loginData = call.receive<User.UserLoginData>()

            val user = usersRepo.getUser(loginData.email)
            if (user == null) call.respond(HttpStatusCode.Unauthorized)
            if (user!!.isBanned) call.respond(HttpStatusCode.Unauthorized)

            val storedLoginData = user.userLoginData
            if (storedLoginData == null) call.respond(HttpStatusCode.Unauthorized)

            if (!Crypto.checkPassword(
                inputPassword = loginData.password,
                salt = storedLoginData!!.salt!!,
                storedHash = storedLoginData.password
            )) call.respond(HttpStatusCode.Unauthorized)

            val token = createToken(user)

            call.respond(hashMapOf("token" to token))
        }
    }
}

fun createToken(user: User): String {
    return JWT.create()
        .withIssuer(System.getenv("jwt.issuer"))
        .withClaim("userId", user.id)
        .withArrayClaim("permissions", user.permissions.map { it.name }.toTypedArray())
        .withClaim("username", user.name)
        .withExpiresAt(Date(Clock.System.now().toEpochMilliseconds() + 60_000))
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

suspend fun ApplicationCall.verify(access: Access) {
    val permissions = this.getPermissions()
    if (permissions == null) {
        this.respond(HttpStatusCode.Forbidden)
        return
    }
    if (!permissions.contains(access)) {
        this.respond(HttpStatusCode.Forbidden)
        return
    }
}
