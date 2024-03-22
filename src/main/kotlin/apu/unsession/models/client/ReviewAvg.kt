package apu.unsession.models.client

import kotlinx.serialization.Serializable
import apu.unsession.models.TeacherDto
import apu.unsession.features.user.User

@Serializable
data class ReviewAvg (
    val id: Int?,
    val user: User,
    val teacher: TeacherDto,

    val globalRating: Float,
    val labsRating: Float?,
    val hwRating: Float?,
    val examRating: Float?,

    val kindness: Float?,
    val responsibility: Float?,
    val individuality: Float?,
    val humour: Float?,

    val createdTimestamp: Float,
    val comment: String?,
)
