package lol.unsession.ai.request

import kotlinx.serialization.Serializable

@Serializable
data class AiRequest(
    var modelUri: String? = null,
    var completionOptions: CompletionOptions? = CompletionOptions(),
    var messages: List<Messages> = arrayListOf()
)