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
import lol.unsession.db.UsersRepositoryImpl
import lol.unsession.security.user.User
import lol.unsession.security.utils.Crypto
import java.util.*

fun Application.configureSecurity() {
    val usersRepo = UsersRepositoryImpl()
    val secret = System.getenv("secret")
    val issuer = System.getenv("jwt.issuer")

    install(Authentication) {
        jwt("user-auth") {
            verifier(JWT
                .require(Algorithm.HMAC256(secret))
                .withIssuer(issuer)
                .build())
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }

    routing {
        post("/login") {
            val loginData = call.receive<User.UserLoginData>()
            val user = usersRepo.getUser(loginData.email)
            val storedLoginData = user!!.userLoginData

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
        .withClaim("username", user.name)
        .withExpiresAt(Date(Clock.System.now().epochSeconds.toInt() + 600.toLong()))
        .sign(Algorithm.HMAC512(System.getenv("secret")))
}
