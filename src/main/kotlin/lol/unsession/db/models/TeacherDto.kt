package lol.unsession.db.models

import kotlinx.serialization.Serializable

@Serializable
data class TeacherDto (
    val id: Int,
    val name: String,
    val email: String?,
    val department: String
)