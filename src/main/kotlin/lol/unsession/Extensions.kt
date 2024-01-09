package lol.unsession

import io.ktor.http.*

val HttpStatusCode.Teapot: HttpStatusCode
    get() = HttpStatusCode(418, "I'm a teapot")