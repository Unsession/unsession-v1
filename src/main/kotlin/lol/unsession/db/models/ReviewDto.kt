package lol.unsession.db.models

import kotlinx.serialization.Serializable
import lol.unsession.db.Repository
import lol.unsession.db.UnsessionSchema.TeacherReview
import lol.unsession.db.models.client.Review
import org.jetbrains.exposed.sql.ResultRow

@Serializable
data class ReviewDto(
    val id: Int? = null,
    val userId: Int,
    val teacherId: Int,

    val globalRating: Int,
    val difficultyRating: Int? = null,
    val boredomRating: Int? = null,
    val toxicityRating: Int? = null,
    val educationalValueRating: Int? = null,
    val personalQualitiesRating: Int? = null,

    val createdTimestamp: Int,
    val comment: String? = null,
) {
    companion object {
        fun ResultRow.toReviewDto(): ReviewDto {
            return ReviewDto(
                this[TeacherReview.id],
                this[TeacherReview.userId],
                this[TeacherReview.teacherId],
                this[TeacherReview.global_rating],
                this[TeacherReview.difficulty_rating],
                this[TeacherReview.boredom_rating],
                this[TeacherReview.toxicity_rating],
                this[TeacherReview.educational_value_rating],
                this[TeacherReview.personal_qualities_rating],
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
