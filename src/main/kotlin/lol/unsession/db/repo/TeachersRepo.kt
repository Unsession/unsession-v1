package lol.unsession.db.repo

import lol.unsession.db.UnsessionSchema
import lol.unsession.db.UnsessionSchema.Teacher
import lol.unsession.db.UnsessionSchema.TeacherRating
import lol.unsession.db.models.RatingDto
import lol.unsession.db.models.TeacherDto
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

interface TeachersRepo {
    /*
    object Teacher : Table() {
        val id = integer("id").check("check_global_person_id") { it lessEq 999999 and (it greaterEq 100000) }
            .uniqueIndex("global_person_id")
        val name = varchar("full_name", 256)
        val email = varchar("email", 64).check { it like "%@niuitmo.ru" or (it like "%@itmo.ru") }
            .nullable()
        val department = varchar("department", 128)

        override val primaryKey = PrimaryKey(id)
    }*/
    fun getTeacher(id: Int): TeacherDto?
    fun getTeachers(page: Int): List<TeacherDto>
    fun addTeacher(teacher: TeacherDto): Boolean
}

interface RatingsRepository {
    /*object PersonalRating : Table() {
        val id = integer("id").autoIncrement().uniqueIndex()
        val ratingId = integer("ratingId").references(TeacherRating.id)

        val kindness_rating =
            integer("kindness_rating").check("check_kindness_rating") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val responsibility_rating =
            integer("responsibility_rating").check("check_responsibility_rating") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val individuality_rating =
            integer("individuality_rating").check("check_individuality_rating") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val humor_rating =
            integer("humor_rating").check("check_humor_rating") { it greaterEq 0 and (it lessEq 5) }
                .nullable()

        override val primaryKey = PrimaryKey(id)
    }

    object TeacherRating : Table() {
        val id = integer("id").autoIncrement().uniqueIndex()
        val user = integer("user").references(Users.id)
        val teacher = integer("teacher").references(Teacher.id)

        val global_rating = integer("global_rating").check("check_global_rating") { it greaterEq 0 and (it lessEq 5) }
        val labs_rating =
            integer("labs_rating").check("check_labs_rating") { it greaterEq 0 and (it lessEq 5) }.nullable()
        val hw_rating =
            integer("homework_rating").check("check_hw_rating") { it greaterEq 0 and (it lessEq 5) }.nullable()
        val exam_rating =
            integer("exam_rating").check("check_exam_rating") { it greaterEq 0 and (it lessEq 5) }.nullable()

        val comment = varchar("comment", 1024).nullable()
        val created = integer("created").default(Clock.System.now().epochSeconds.toInt())

        override val primaryKey = PrimaryKey(id)
    }*/
    fun getRating(id: Int): RatingDto?
    fun getRatingsByTeacher(teacherId: Int, page: Int): List<RatingDto>
    fun getRatingsByUser(userId: Int, page: Int): List<RatingDto>
    fun addRating(rating: RatingDto): Boolean
}

class TeachersRatingsRepository : TeachersRepo, RatingsRepository {
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

    override fun getRating(id: Int): RatingDto? {
        return TeacherRating.select { TeacherRating.id eq id }.firstOrNull()?.let {
            RatingDto(
                it[TeacherRating.id],
                it[TeacherRating.user],
                it[TeacherRating.teacher],
                it[TeacherRating.global_rating],
                it[TeacherRating.labs_rating],
                it[TeacherRating.hw_rating],
                it[TeacherRating.exam_rating],
                it[TeacherRating.comment],
                it[TeacherRating.created],
            )
        }
    }

    override fun getRatingsByTeacher(teacherId: Int, page: Int): List<RatingDto> {
        return TeacherRating.select { TeacherRating.teacher eq teacherId }
            .limit(20, (page * 20).toLong()).map {
                RatingDto(
                    it[TeacherRating.id],
                    it[TeacherRating.user],
                    it[TeacherRating.teacher],
                    it[TeacherRating.global_rating],
                    it[TeacherRating.labs_rating],
                    it[TeacherRating.hw_rating],
                    it[TeacherRating.exam_rating],
                    it[TeacherRating.comment],
                    it[TeacherRating.created],
                )
            }
    }

    override fun getRatingsByUser(userId: Int, page: Int): List<RatingDto> {
        return TeacherRating.select { TeacherRating.user eq userId }
            .limit(20, (page * 20).toLong()).map {
                RatingDto(
                    it[TeacherRating.id],
                    it[TeacherRating.user],
                    it[TeacherRating.teacher],
                    it[TeacherRating.global_rating],
                    it[TeacherRating.labs_rating],
                    it[TeacherRating.hw_rating],
                    it[TeacherRating.exam_rating],
                    it[TeacherRating.comment],
                    it[TeacherRating.created],
                )
            }
    }

    override fun addRating(rating: RatingDto): Boolean {
        return TeacherRating.insert {
            it[user] = rating.userId
            it[teacher] = rating.teacherId
            it[global_rating] = rating.globalRating
            it[labs_rating] = rating.labsRating
            it[hw_rating] = rating.hwRating
            it[exam_rating] = rating.examRating
            it[comment] = rating.comment
            it[created] = rating.createdTimestamp
        }.insertedCount > 0
    }
}