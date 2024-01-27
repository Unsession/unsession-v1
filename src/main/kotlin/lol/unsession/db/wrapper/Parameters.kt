package lol.unsession.db.wrapper

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


@Serializable
data class Sorter(
    val field: String,
    val a: Boolean,
)

@Serializable
data class DataSelectParameters(
    var filters: HashMap<String, @Contextual Any> = hashMapOf(),
    val sort: Sorter? = Sorter("id", true),
)

@Serializable
data class PagingFilterParameters(
    val page: Int,
    val pageSize: Int,
    val dataSelectParameters: DataSelectParameters?,
) {
    companion object {
        suspend fun from(call: ApplicationCall): PagingFilterParameters {
            val params = call.request.queryParameters
            val page = params["page"]?.toIntOrNull() ?: -1
            val pageSize = params["pageSize"]?.toIntOrNull() ?: -1
            val addParams = call.receiveNullable<DataSelectParameters>()
            if (page == -1 || pageSize == -1) {
                call.respond(HttpStatusCode.BadRequest, "No page or PageSize specified")
                return PagingFilterParameters(-1, -1, null) // never happens, but compiler doesn't know
            }
            return PagingFilterParameters(page, pageSize, addParams)
        }
    }

    fun addFilter(key: String, value: Any): PagingFilterParameters {
        dataSelectParameters?.filters?.put(key, value)
        return this
    }
}