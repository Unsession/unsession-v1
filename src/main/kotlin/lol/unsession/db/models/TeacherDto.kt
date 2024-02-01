package lol.unsession.db.models

import kotlinx.serialization.Serializable

@Serializable
open class TeacherDto(
    val id: Int,
    val name: String,
    val email: String?,
    val department: String,
    val rating: Double? = null,
)