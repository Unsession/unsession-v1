package lol.unsession.models.ai.request

import kotlinx.serialization.Serializable

@Serializable
data class AiRequest(
    val input: String
)