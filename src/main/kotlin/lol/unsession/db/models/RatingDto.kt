package lol.unsession.db.models

data class PersonalRatingDto (
    val id: Int?,
    val ratingId: Int,

    val kindness: Int,
    val responsibility: Int,
    val individuality: Int,
    val humour: Int,
)

data class RatingDto (
    val id: Int?,
    val userId: Int,
    val teacherId: Int,

    val globalRating: Int,
    val labsRating: Int?,
    val hwRating: Int?,
    val examRating: Int?,

    val comment: String?,
    val createdTimestamp: Int,
)
