package lol.unsession.db.repo

import lol.unsession.db.UnsessionSchema.Teacher
import lol.unsession.db.UnsessionSchema.TeacherReview
import lol.unsession.db.models.ReviewDto
import lol.unsession.db.models.ReviewDto.Companion.toReviewDto
import lol.unsession.db.models.TeacherDto
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

interface TeachersRepo {
    fun getTeacher(id: Int): TeacherDto?
    fun getTeachers(page: Int): List<TeacherDto>
    fun addTeacher(teacher: TeacherDto): Boolean
}

interface ReviewsRepository {
    fun getReview(id: Int): ReviewDto?
    fun getReviewsByTeacher(teacherId: Int, page: Int): List<ReviewDto>
    fun getReviewsByUser(userId: Int, page: Int): List<ReviewDto>
    fun addReview(rating: ReviewDto): Boolean
}

class TeachersReviewsRepository : TeachersRepo, ReviewsRepository {
    override fun getTeacher(id: Int): TeacherDto? {
        return Teacher.select { Teacher.id eq id }.firstOrNull()?.let {
            TeacherDto(
                it[Teacher.id],
                it[Teacher.name],
                it[Teacher.email],
                it[Teacher.department],
            )
        }
    }

    override fun getTeachers(page: Int): List<TeacherDto> {
        return Teacher.selectAll().limit(20, (page * 20).toLong()).map {
            TeacherDto(
                it[Teacher.id],
                it[Teacher.name],
                it[Teacher.email],
                it[Teacher.department],
            )
        }
    }

    override fun addTeacher(teacher: TeacherDto): Boolean {
        return Teacher.insert {
            it[name] = teacher.name
            it[email] = teacher.email
            it[department] = teacher.department
        }.insertedCount > 0
    }

    override fun getReview(id: Int): ReviewDto? {
        return TeacherReview.select { TeacherReview.id eq id }.firstOrNull()?.toReviewDto()
    }

    override fun getReviewsByTeacher(teacherId: Int, page: Int): List<ReviewDto> {
        return TeacherReview.select { TeacherReview.teacher eq teacherId }
            .limit(20, (page * 20).toLong()).map {
                it.toReviewDto()
            }
    }

    override fun getReviewsByUser(userId: Int, page: Int): List<ReviewDto> {
        return TeacherReview.select { TeacherReview.user eq userId }
            .limit(20, (page * 20).toLong()).map {
                it.toReviewDto()
            }
    }

    override fun addReview(rating: ReviewDto): Boolean {
        return TeacherReview.insert {
            it[user] = rating.userId
            it[teacher] = rating.teacherId
            it[global_rating] = rating.globalRating
            it[labs_rating] = rating.labsRating
            it[hw_rating] = rating.hwRating
            it[exam_rating] = rating.examRating
            it[kindness_rating] = rating.kindness
            it[responsibility_rating] = rating.responsibility
            it[individuality_rating] = rating.individuality
            it[humor_rating] = rating.humour
            it[comment] = rating.comment
            it[created] = rating.createdTimestamp
        }.insertedCount > 0
    }
}