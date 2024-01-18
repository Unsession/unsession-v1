package lol.unsession.plugins

import io.kotlintest.shouldBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.util.pipeline.*
import lol.unsession.security.user.User
import lol.unsession.test.TestSDK

fun main() = testApplication {
    application {
        configureHTTP()
        configureMonitoring()
        configureSerialization()
        configureDatabases()
        configureAdministration()
        configureSecurity()
        configureRouting()
    }
    val client = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            url("https://localhost:7777/")
        }
    }
    val data = User.UserLoginData(
        TestSDK.username,
        TestSDK.email,
        TestSDK.password
    )
    client.post("/register") {
        setBody(data)
        println("--- $body ---")
    }.apply {
        call.response.status shouldBe HttpStatusCode.OK
        val body = call.response.body<LoginResponse>()
        body.user.userLoginData!!.email shouldBe data.email
        client.get("/authtest") {
            header(HttpHeaders.Authorization, "Bearer ${body.token}")
        }.apply {
            call.response.status shouldBe HttpStatusCode.OK
            call.response.bodyAsText() shouldBe "Hello, world!"
        }
    }
}
