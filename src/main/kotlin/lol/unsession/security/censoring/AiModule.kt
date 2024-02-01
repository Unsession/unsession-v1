package lol.unsession.security.censoring

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import lol.unsession.AiResponse
import lol.unsession.db.models.ai.request.AiRequest
import lol.unsession.db.models.ai.request.CompletionOptions
import lol.unsession.db.models.ai.request.Messages
import lol.unsession.db.models.client.Review
import lol.unsession.utils.Result

object AiModule {
    val censorClient = HttpClient {
        defaultRequest {
            headers {
                append("Content-Type", ContentType.Application.Json.contentType)
                append("Authorization", ("Api-Key ${System.getenv("ai-apiKey")}"))
                append("x-folder-id", System.getenv("folder-id"))
            }
        }
    }

    private suspend fun censoringRequest(
        request: String
    ): Result {
        val call = censorClient.post {
            setBody(
                AiRequest(
                    modelUri = "gpt://${System.getenv("folder-id")}/yandexgpt-lite",
                    completionOptions = CompletionOptions(
                        stream = false,
                        temperature = 0.7,
                        maxTokens = 1000,
                    ),
                    messages = listOf(
                        Messages(
                            role = "System",
                            text = """
                                Тебе нужно проанализировать отзыв на преподавателя и ответить, можно ли
                                 его публиковать или если нельзя, то почему нельзя.
                                 Не допускаются отзывы, содержащие личные оскорбления другого человека, переходы на 
                                 личности, упоминания других персон или каких либо персональных данных, исключая ФИО людей. 
                                 Формат ответа: "К публикации: разрешено/запрещено. И тут пояснение по решению, если оно нужно. 
                                 Если разрешено, пояснение писать не нужно." Пример ответа: если 
                                 запрещено, то "К публикации: запрещено. Отзыв содержит персональные данные Риты Сергеевны" 
                                 или если разрешено, то "К публикации: разрешено."
                            """.trimIndent().lines().joinToString("")
                        ),
                        Messages(
                            role = "user",
                            text = request
                        )
                    )
                )
            )
        }
        val response = call.body<AiResponse>().result
        return if (call.status == HttpStatusCode.OK && response != null) {
            Result.success()
        } else {
            Result.error((response?.alternatives?.get(0)?.message?: "Unknown error").toString())
        }
    }
    suspend fun Review.allowed(): Result {
        val review = this
        if (review.comment == null || review.comment.isNotEmpty()) return Result(true, "Отзыв не содержит комментария")
        val result = censoringRequest(
            request = review.comment.toString()
        )
        return if (result.success) {
            Result.success()
        } else {
            Result.error(result.message.toString())
        }
    }
}
