package lol.unsession.ai.request

import kotlinx.serialization.Serializable

@Serializable
data class CompletionOptions(
    var stream: Boolean? = null,
    var temperature: Double? = null,
    var maxTokens: Int? = null
)