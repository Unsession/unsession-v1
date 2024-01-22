package lol.unsession.db.models.client

import kotlinx.serialization.Serializable
import lol.unsession.db.UnsessionSchema
import lol.unsession.db.UnsessionSchema.TeacherReview.comment
import lol.unsession.db.models.ReviewDto
import lol.unsession.db.models.TeacherDto
import lol.unsession.security.user.User

@Serializable
data class Review (
    val id: Int?,
    val user: User,
    val teacher: TeacherDto,

    val globalRating: Int,
    val labsRating: Int?,
    val hwRating: Int?,
    val examRating: Int?,

    val kindness: Int?,
    val responsibility: Int?,
    val individuality: Int?,
    val humour: Int?,

    val createdTimestamp: Int,
    val comment: String?,
) {
    companion object {
        fun fromReviewAndUser(review: ReviewDto, user: User, teacher: TeacherDto): Review {
            return Review(
                review.id,
                user,
                teacher,
                review.globalRating,
                review.labsRating,
                review.hwRating,
                review.examRating,
                review.kindness,
                review.responsibility,
                review.individuality,
                review.humour,
                review.createdTimestamp,
                review.comment
            )
        }
    }
    fun toReviewDto(): ReviewDto {
        return ReviewDto(
            id = null,
            userId = user.id,
            teacherId = teacher.id,
            globalRating = globalRating,
            labsRating = labsRating,
            hwRating = hwRating,
            examRating = examRating,
            kindness = kindness,
            responsibility = responsibility,
            individuality = individuality,
            humour = humour,
            createdTimestamp = createdTimestamp,
            comment = comment
        )
    }
}