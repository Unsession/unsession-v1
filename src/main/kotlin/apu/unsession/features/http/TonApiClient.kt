package apu.unsession.features.http

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val clientTonApi =
    HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        defaultRequest {
            header("Content-Type", "application/json")
            url("https://tonapi.io/")
        }
    }
