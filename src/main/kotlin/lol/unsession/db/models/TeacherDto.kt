package lol.unsession.db.models

data class TeacherDto (
    val id: Int,
    val name: String,
    val email: String?,
    val department: String
)