package lol.unsession.db.repo

import lol.unsession.Utils
import lol.unsession.db.UnsessionSchema.Companion.dbQuery
import lol.unsession.db.UnsessionSchema.Teacher
import lol.unsession.db.UnsessionSchema.TeacherReview
import lol.unsession.db.models.ReviewDto.Companion.toReviewDto
import lol.unsession.db.models.TeacherDto
import lol.unsession.db.models.client.Review
import lol.unsession.security.user.User
import lol.unsession.test.TestSDK
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

interface TeachersRepo {
    suspend fun getTeacher(id: Int): TeacherDto?
    suspend fun getTeachers(page: Int, pageSize: Int): List<TeacherDto>
    suspend fun addTeacher(teacher: TeacherDto): TeacherDto?
    suspend fun searchTeachers(prompt: String, page: Int, pageSize: Int): List<TeacherDto>
}

interface ReviewsRepository {
    suspend fun getReview(id: Int): Review?
    suspend fun getReviews(page: Int, pageSize: Int): List<Review>
    suspend fun getReviewsByTeacher(teacherId: Int, page: Int, pageSize: Int): List<Review>
    suspend fun getReviewsByUser(userId: Int, page: Int, pageSize: Int): List<Review>
    suspend fun addReview(rating: Review): Boolean
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

    override suspend fun getTeachers(page: Int, pageSize: Int): List<TeacherDto> {
        return dbQuery {
            Teacher.selectAll().limit(pageSize, (page * pageSize).toLong()).map {
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

    override suspend fun searchTeachers(prompt: String, page: Int, pageSize: Int): List<TeacherDto> {
        return dbQuery {
            Teacher.select { Teacher.name like "%$prompt%" }.limit(pageSize, (page * pageSize).toLong()).map {
                TeacherDto(
                    it[Teacher.id],
                    it[Teacher.name],
                    it[Teacher.email],
                    it[Teacher.department],
                )
            }
        }
    }

    override suspend fun getReview(id: Int): Review? {
        return dbQuery { TeacherReview.select { TeacherReview.id eq id }.firstOrNull()?.toReviewDto()?.toReview() }
    }

    override suspend fun getReviews(page: Int, pageSize: Int): List<Review> {
        return dbQuery {
            TeacherReview.selectAll().limit(pageSize, (page * pageSize).toLong()).map {
                it.toReviewDto().toReview()
            }
        }
    }

    override suspend fun getReviewsByTeacher(teacherId: Int, page: Int, pageSize: Int): List<Review> {
        return dbQuery {
            TeacherReview.select { TeacherReview.teacher eq teacherId }
                .limit(pageSize, (page * pageSize).toLong()).map {
                    it.toReviewDto().toReview()
                }
        }
    }

    override suspend fun getReviewsByUser(userId: Int, page: Int, pageSize: Int): List<Review> {
        return dbQuery {
            TeacherReview.select { TeacherReview.user eq userId }
                .limit(pageSize, (page * pageSize).toLong()).map {
                    it.toReviewDto().toReview()
                }
        }
    }

    override suspend fun addReview(rating: Review): Boolean {
        return dbQuery {
            TeacherReview.insert {
                it[user] = rating.user.id
                it[teacher] = rating.teacher.id
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

suspend fun generateTestData(onExecuted: suspend (String) -> Unit) {
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
                        Review(
                            null,
                            user,
                            TeachersReviewsRepositoryImpl.getTeacher(teachersId.random())!!,
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
                onExecuted("Test data generated successfully")
            }
        )
    }
}