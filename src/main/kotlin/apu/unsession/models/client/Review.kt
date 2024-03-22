package apu.unsession.models.client

import kotlinx.serialization.Serializable
import apu.unsession.models.ReviewDto
import apu.unsession.models.TeacherDto
import apu.unsession.features.user.User

@Serializable
data class Review (
    val id: Int?,
    val user: User?,
    val teacher: TeacherDto,

    val globalRating: Int,
    val difficultyRating: Int?,
    val boredomRating: Int?,
    val toxicityRating: Int?,
    val educationalValueRating: Int?,
    val personalQualitiesRating: Int?,

    val createdTimestamp: Int?,
    val comment: String?,
) {
    companion object {
        fun fromReviewAndUser(review: ReviewDto, user: User, teacher: TeacherDto): Review {
            return Review(
                review.id,
                user,
                teacher,
                review.globalRating,
                review.difficultyRating,
                review.boredomRating,
                review.toxicityRating,
                review.educationalValueRating,
                review.personalQualitiesRating,
                review.createdTimestamp,
                review.comment
            )
        }
    }
    fun toReviewDto(): ReviewDto {
        return ReviewDto(
            id = null,
            userId = user!!.id,
            teacherId = teacher.id,
            globalRating = globalRating,
            difficultyRating = difficultyRating,
            boredomRating = boredomRating,
            toxicityRating = toxicityRating,
            educationalValueRating = educationalValueRating,
            personalQualitiesRating = personalQualitiesRating,
            createdTimestamp = createdTimestamp?: -1,
            comment = comment
        )
    }
}