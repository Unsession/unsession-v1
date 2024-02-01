package lol.unsession.db.models.ai.request

import kotlinx.serialization.Serializable

@Serializable
data class Messages(
    var role: String? = null,
    var text: String? = null
)
