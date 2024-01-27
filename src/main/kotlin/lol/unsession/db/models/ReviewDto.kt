package lol.unsession.db.models

import kotlinx.serialization.Serializable
import lol.unsession.db.UnsessionSchema.TeacherReview
import lol.unsession.db.models.client.Review
import lol.unsession.db.repo.Repository
import org.jetbrains.exposed.sql.ResultRow

@Serializable
data class ReviewDto (
    val id: Int?,
    val userId: Int,
    val teacherId: Int,

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
        fun ResultRow.toReviewDto(): ReviewDto {
            return ReviewDto(
                this[TeacherReview.id],
                this[TeacherReview.userId],
                this[TeacherReview.teacherId],
                this[TeacherReview.global_rating],
                this[TeacherReview.labs_rating],
                this[TeacherReview.hw_rating],
                this[TeacherReview.exam_rating],
                this[TeacherReview.kindness_rating],
                this[TeacherReview.responsibility_rating],
                this[TeacherReview.individuality_rating],
                this[TeacherReview.humor_rating],
                this[TeacherReview.created],
                this[TeacherReview.comment],
            )
        }
    }

    suspend fun toReview(): Review {
        val users = Repository.Users
        val user = users.getUser(userId)
        val teacher = Repository.Teachers.getTeacher(teacherId)
        return Review.fromReviewAndUser(this, user!!, teacher!!)
    }
}
