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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lol.unsession.db.repo.UsersRepositoryImpl
import lol.unsession.security.permissions.Access
import lol.unsession.security.user.User
import lol.unsession.security.utils.Crypto
import lol.unsession.utils.getLogger
import java.util.*
import kotlin.collections.HashSet
import kotlin.time.Duration

fun Application.configureSecurity() {
    val usersRepo = UsersRepositoryImpl
    val secret = "Ejrr]&HrrCr^DLy:xOfx}o5}%3_;x=U\$/J<H<l,4NTIRImVYBTXqB\\BQ(xlJdznP_GnZ3N_7*_FJXERo[nK4<5WByGtJtD}&_PJh}frdL%{N:usAbg5B<9*]]g;s,Ug;payment.key=TC-INVOICE_ee257eb3dc175e6ee5f7a58f89a09954651d7ae67381847f4d0c6c47cb47db1188"//System.getenv("secret")
    val issuer = ""//System.getenv("jwt.issuer")

    install(Authentication) {
        jwt("user-auth") {
            val logger = getLogger("validator")
            verifier(JWT
                .require(Algorithm.HMAC512(secret))
                .withIssuer(issuer)
                .build())
            validate { credential ->
                try {
                    val user = usersRepo.getUser(credential.payload.getClaim("userId").asInt())
                    logger.debug("requested from token: ${user.toString()}")
                    if (credential.payload.getClaim("username").asString() != "" &&
                        user != null &&
                        credential.expiresAt!!.time > Clock.System.now().toEpochMilliseconds()
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

            call.respond(LoginResponse(token, user))
        }
    }
}

fun createToken(user: User): String {
    return JWT.create()
        .withIssuer(System.getenv("jwt_issuer"))
        .withClaim("userId", user.id)
        .withArrayClaim("permissions", user.permissions.map { it.name }.toTypedArray())
        .withClaim("username", user.name)
        .withExpiresAt(Date(Clock.System.now().toEpochMilliseconds() + 10_000))
        .sign(Algorithm.HMAC512("Ejrr]&HrrCr^DLy:xOfx}o5}%3_;x=U\$/J<H<l,4NTIRImVYBTXqB\\BQ(xlJdznP_GnZ3N_7*_FJXERo[nK4<5WByGtJtD}&_PJh}frdL%{N:usAbg5B<9*]]g;s,Ug;payment.key=TC-INVOICE_ee257eb3dc175e6ee5f7a58f89a09954651d7ae67381847f4d0c6c47cb47db1188"/*System.getenv("secret")*/))
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
