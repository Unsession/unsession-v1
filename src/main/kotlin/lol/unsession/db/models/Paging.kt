package lol.unsession.db.models

import io.ktor.server.application.*

data class Paging(
    val page: Int,
    val size: Int = 20
) {
    companion object {
        fun from(call: ApplicationCall): Paging =
            Paging(call.parameters["page"]?.toIntOrNull() ?: 1, call.parameters["size"]?.toIntOrNull() ?: 20)
    }
}