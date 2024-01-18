package lol.unsession.db.repo

import lol.unsession.Utils
import lol.unsession.db.UnsessionSchema.Companion.dbQuery
import lol.unsession.db.UnsessionSchema.Teacher
import lol.unsession.db.UnsessionSchema.TeacherReview
import lol.unsession.db.models.ReviewDto
import lol.unsession.db.models.ReviewDto.Companion.toReviewDto
import lol.unsession.db.models.TeacherDto
import lol.unsession.security.user.User
import lol.unsession.test.TestSDK
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

interface TeachersRepo {
    suspend fun getTeacher(id: Int): TeacherDto?
    suspend fun getTeachers(page: Int): List<TeacherDto>
    suspend fun addTeacher(teacher: TeacherDto): TeacherDto?
}

interface ReviewsRepository {
    suspend fun getReview(id: Int): ReviewDto?
    suspend fun getReviews(page: Int): List<ReviewDto>
    suspend fun getReviewsByTeacher(teacherId: Int, page: Int): List<ReviewDto>
    suspend fun getReviewsByUser(userId: Int, page: Int): List<ReviewDto>
    suspend fun addReview(rating: ReviewDto): Boolean
}

object TeachersReviewsRepositoryImpl : TeachersRepo, ReviewsRepository {
    override suspend fun getTeacher(id: Int): TeacherDto? {
        return dbQuery {
            Teacher.select { Teacher.id eq id }.firstOrNull()?.let {
                TeacherDto(
                    it[Teacher.id],
                    it[Teacher.name],
                    it[Teacher.email],
                    it[Teacher.department],
                )
            }
        }
    }

    override suspend fun getTeachers(page: Int): List<TeacherDto> {
        return dbQuery {
            Teacher.selectAll().limit(20, (page * 20).toLong()).map {
                TeacherDto(
                    it[Teacher.id],
                    it[Teacher.name],
                    it[Teacher.email],
                    it[Teacher.department],
                )
            }
        }
    }

    override suspend fun addTeacher(teacher: TeacherDto): TeacherDto? {
        if (dbQuery {
                Teacher.insert {
                    it[id] = teacher.id
                    it[name] = teacher.name
                    it[email] = teacher.email
                    it[department] = teacher.department
                }
            }.insertedCount != 1) {
            return null
        }
        return teacher
    }

    override suspend fun getReview(id: Int): ReviewDto? {
        return dbQuery { TeacherReview.select { TeacherReview.id eq id }.firstOrNull()?.toReviewDto() }
    }

    override suspend fun getReviews(page: Int): List<ReviewDto> {
        return dbQuery {
            TeacherReview.selectAll().limit(20, (page * 20).toLong()).map {
                it.toReviewDto()
            }
        }
    }

    override suspend fun getReviewsByTeacher(teacherId: Int, page: Int): List<ReviewDto> {
        return dbQuery {
            TeacherReview.select { TeacherReview.teacher eq teacherId }
                .limit(20, (page * 20).toLong()).map {
                    it.toReviewDto()
                }
        }
    }

    override suspend fun getReviewsByUser(userId: Int, page: Int): List<ReviewDto> {
        return dbQuery {
            TeacherReview.select { TeacherReview.user eq userId }
                .limit(20, (page * 20).toLong()).map {
                    it.toReviewDto()
                }
        }
    }

    override suspend fun addReview(rating: ReviewDto): Boolean {
        return dbQuery {
            TeacherReview.insert {
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
}

suspend fun generateTestData() {
    val usersRepo = UsersRepositoryImpl
    val teachersRepo = TeachersReviewsRepositoryImpl
    val teachersId = mutableListOf<Int>()
    repeat(20) {
        teachersId.add(
            teachersRepo.addTeacher(
                TeacherDto(TestSDK.globalPersonId, TestSDK.name, TestSDK.email, TestSDK.department)
            )!!.id
        )
    }
    repeat(5) {
        usersRepo.tryRegisterUser(
            User.UserLoginData(
                TestSDK.username,
                TestSDK.email,
                TestSDK.password,
            ), "1.1.1.1",
            onSuccess = { user ->
                repeat(5) {
                    teachersRepo.addReview(
                        ReviewDto(
                            null,
                            user.id,
                            teachersId.random(),
                            (1..5).random(),
                            (1..5).random(),
                            (1..5).random(),
                            (1..5).random(),
                            (1..5).random(),
                            (1..5).random(),
                            (1..5).random(),
                            (1..5).random(),
                            Utils.now,
                            TestSDK.lorem(
                                (6..20).random()
                            )
                        )
                    )
                }
            }
        )
    }
}