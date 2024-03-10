package lol.unsession.features.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import lol.unsession.AiResponse
import lol.unsession.Results
import lol.unsession.models.ReviewDto
import lol.unsession.models.ai.request.AiRequest
import lol.unsession.utils.getLogger

val aiClient = HttpClient(OkHttp) {
    defaultRequest {
        headers {
            append("Content-Type", ContentType.Application.Json)
            append("Authorization", ("Bearer ${System.getenv("ai_apiKey")}"))
        }
    }
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
}

sealed class AiModule {
    val liteLLM = "gpt://${System.getenv("folder-id")}/yandexgpt-lite"
    val heavyLLM =
        "gpt://${System.getenv("folder-id")}/yandexgpt" // это дорогая модель, поэтому используем ее только для сложных запросов
    val chatModeration7 = "https://api.openai.com/v1/moderations"

    val logger = getLogger("Ai")

    suspend fun HttpResponse.toResult(): Result<Results> {
        return when (status) {
            HttpStatusCode.OK -> {
                val response = this.body<AiResponse>().results.first()
                Result.success(response)
            }
            else -> {
                val response = this.bodyAsText()
                Result.failure(Exception(response))
            }
        }
    }

    data object Censor : AiModule() {

        private suspend fun censoringRequest(
            request: String
        ): Result<Results> {
            val response = aiClient.post {
                url(chatModeration7)
                setBody(AiRequest(request))
            }
            println(response.bodyAsText())
            return response.toResult()
        }

        suspend fun ReviewDto.allowed(): Result<String> {
            val review = this
            if (review.comment.isNullOrEmpty()) return Result.failure(Exception("Comment is empty"))
            val censor = censoringRequest(
                request = review.comment.toString()
            )
            logger.info("Censoring result: ${review.comment}")
            val body =
                censor.getOrThrow() // ?: return Result.failure(Exception("Call Failure ${censor.exceptionOrNull()?.localizedMessage} ; ${censor.exceptionOrNull()?.cause?.localizedMessage} ; ${censor.exceptionOrNull().toString()}"))
            return if (!body.flagged!!) {
                logger.info("Censoring result: ${body.flagged}")
                logger.info("Censoring result: ${body.categories}")
                logger.info("Censoring result: ${body.categoryScores}")
                Result.success("Ok")
            } else {
                logger.info("Censoring result: ${body.flagged}")
                logger.info("Censoring result: ${body.categories}")
                logger.info("Censoring result: ${body.categoryScores}")
                with(body.categories) {
                    Result.failure(
                        when {
                            sexual -> Exception("Найди себе девушку, хз...\nОтказано: сексуальный контент (01)")
                            hate -> Exception("Я щас блять покажу тебе как материться нахуй. Придурок, блть\nОтказано: ругань/хейт (02)")
                            harassment -> Exception("Без херасмента тут!\nОтказано: харассмент (03)")
                            selfHarm -> Exception("Не надо так\nОтказано: селф-харм (04)")
                            sexualMinors -> Exception("Ну это уже слишком\nОтказано: сексуальный контент (05)")
                            hateThreatening -> Exception("Не угрожай, пожалуйста. Я ж тебя забаню\nОтказано: угрозы (06)")
                            violenceGraphic -> Exception("Отказано: насилие (07)")
                            selfHarmIntent -> Exception("Не надо так\nОтказано: селф-харм (08)")
                            selfHarmInstructions -> Exception("Не надо так\nОтказано: селф-харм (09)")
                            harassmentThreatening -> Exception("Не угрожай, пожалуйста. Я ж тебя забаню\nОтказано: угрозы (10)")
                            violence -> Exception("Мы за мир без насилия!\nОтказано: насилие (11)")
                            else -> Exception("Unknown (12)")
                        }
                    )
                }
            }
        }
    }
}
