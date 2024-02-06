package lol.unsession.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import lol.unsession.AiResponse
import lol.unsession.AiResult
import lol.unsession.ai.request.AiRequest
import lol.unsession.ai.request.CompletionOptions
import lol.unsession.ai.request.Messages
import lol.unsession.db.Repository
import lol.unsession.db.models.Paging
import lol.unsession.db.models.client.Review
import lol.unsession.getResourceUri
import lol.unsession.utils.Result

val aiClient = HttpClient {
    defaultRequest {
        headers {
            append("Content-Type", ContentType.Application.Json.contentType)
            append("Authorization", ("Api-Key ${System.getenv("ai-apiKey")}"))
            append("x-folder-id", System.getenv("folder-id"))
        }
    }
}

sealed class AiModule {
    val liteLLM = "gpt://${System.getenv("folder-id")}/yandexgpt-lite"
    val heavyLLM =
        "gpt://${System.getenv("folder-id")}/yandexgpt" // это дорогая модель, поэтому используем ее только для сложных запросов

    suspend fun HttpResponse.toResult(): Pair<AiResult?, Result> {
        val response = this.body<AiResponse>().aiResult
        return if (status == HttpStatusCode.OK && response != null) {
            response to Result.success()
        } else {
            response to Result.error((response?.alternatives?.get(0)?.message ?: "Unknown error").toString())
        }
    }

    data object Censor : AiModule() {

        private suspend fun censoringRequest(
            request: String
        ): Result {
            val call = aiClient.post {
                setBody(
                    AiRequest(
                        modelUri = liteLLM,
                        completionOptions = CompletionOptions(
                            stream = false,
                            temperature = 0.35,
                            maxTokens = 450,
                        ),
                        messages = listOf(
                            Messages(
                                role = "System",
                                text = getResourceUri("prompts/censor_review").readText()
                            ),
                            Messages(
                                role = "user",
                                text = request
                            )
                        )
                    )
                )
            }
            val response = call.toResult()
            return if (response.second.success) {
                Result.success()
            } else {
                Result.error(response.first?.alternatives?.get(0)?.message.toString())
            }
        }

        suspend fun Review.allowed(): Result {
            val review = this
            if (review.comment == null || review.comment.isNotEmpty()) return Result(
                true,
                "Отзыв не содержит комментария"
            )
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

    data object Reviews : AiModule() {
        suspend fun avgComment(
            teacherId: Int
        ): Result {
            val reviews = Repository.Reviews.getReviewsByTeacher(teacherId, Paging(0, 20))
            val prompt = constructPrompt(reviews)
            val call = aiClient.post {
                setBody(
                    AiRequest(
                        modelUri = heavyLLM,
                        completionOptions = CompletionOptions(
                            stream = false,
                            temperature = 0.75,
                            maxTokens = 2000,
                        ),
                        messages = listOf(
                            Messages(
                                role = "System",
                                text = getResourceUri("prompts/avg_comment").readText()
                            ),
                            Messages(
                                role = "user",
                                text = prompt
                            )
                        )
                    )
                )
            }
            val response = call.body<AiResponse>().aiResult
            return if (call.status == HttpStatusCode.OK && response != null) {
                Result.success()
            } else {
                Result.error((response?.alternatives?.get(0)?.message ?: "Unknown error").toString())
            }
        }

        private fun constructPrompt(reviews: List<Review>): String { // TODO : use formatted string
            val prompt = StringBuilder()
            prompt.append(getResourceUri("prompts/avg_comment").readText())
            prompt.append("\n\n")
            reviews.forEach {
                prompt.append("Общий рейтинг: ${it.globalRating}\n")
                it.labsRating?.let { labsRating -> prompt.append("Рейтинг лаб: $labsRating\n") }
                it.hwRating?.let { hwRating -> prompt.append("Рейтинг дз: $hwRating\n") }
                it.examRating?.let { examRating -> prompt.append("Рейтинг экзаменов: $examRating\n") }
                it.kindness?.let { kindness -> prompt.append("Доброта: $kindness\n") }
                it.responsibility?.let { responsibility -> prompt.append("Ответственность: $responsibility\n") }
                it.individuality?.let { individuality -> prompt.append("Индивидуальность: $individuality\n") }
                it.humour?.let { humour -> prompt.append("Юмор: $humour\n") }
                prompt.append("\n")
                prompt.append("Комментарий к оценкам:\n")
                prompt.append(it.comment)
                prompt.append("\n")
            }
            return prompt.toString()
        }
    }
}
