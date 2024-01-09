package lol.unsession.plugins

import io.kotlintest.matchers.string.shouldMatch
import io.kotlintest.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lol.unsession.db.UnsessionSchema
import lol.unsession.module
import lol.unsession.security.user.User
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

fun main() = testApplication {
    application {
        configureHTTP()
        configureMonitoring()
        configureDatabases()
        configureAdministration()
        configureSerialization()
        configureSecurity()
        configureRouting()
    }
    client.post("/register") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody("""
            {
              "username": "perfmidssionsuser",
              "email": "permisfsidons@niuitmo.ru",
              "password": "0000",
              "salt": null  
            }
        """.trimIndent())
        println("--- $body ---")
    }.apply {
        this.call.response.status shouldBe HttpStatusCode.OK
        val token = this.call.response.bodyAsText().drop(10).dropLast(2)
        this.call.response.bodyAsText() shouldMatch  "\\{\"token\":\".*\"}".toRegex()

        client.get("/authtest") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            this.call.response.status shouldBe HttpStatusCode.OK
            this.call.response.bodyAsText() shouldBe "Hello, world!"
        }
    }
}
