package lol.unsession.db

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import lol.unsession.db.UnsessionSchema.PersonalRating.nullable
import lol.unsession.db.UnsessionSchema.TeacherRating.check
import lol.unsession.db.UnsessionSchema.TeacherRating.default
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.match
import kotlin.random.Random

// генерал рандом ахахахаха
val generalRandom = Random(System.getenv("coderndseed").toInt())
class UnsessionSchema(private val database: Database) {
    object Users : Table() {
        val id = integer("seqno").autoIncrement()
        val name = varchar("name", 64)
        val email = varchar("email", 64).uniqueIndex().match("""^[a-zA-Z0-9._%+-]+@(niuitmo\.ru|itmo\.ru)$""")

        val password = varchar("hash", 64)
        val salt = varchar("salt", 64)

        val permissions = binary("permissions", 255)
        val roleName = varchar("roleName", 32)

        val isBanned = bool("isBanned").default(false)
        val bannedReason = varchar("bannedReason", 255).nullable()
        val bannedUntil = integer("bannedUntil").nullable()

        val created = integer("created").default(System.currentTimeMillis().toInt())

        val last_login = integer("last_login")
        val last_ip = varchar("last_ip", 15)

        val referer = integer("referer").references(id).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    object Codes : Table() {
        val id = integer("seqno").autoIncrement()
        val creator = integer("creator").references(Users.id)

        val code = varchar("code", 4).default(generalRandom.nextBits(4).toString())
        val activations = integer("activations").default(0)
        val maxActivations = integer("maxActivations").default(1)
        val validUntil = integer("validUntil")

        override val primaryKey = PrimaryKey(id)
    }

    object Subject : Table() {
        val id = integer("seqno").autoIncrement()
        val name = varchar("name", 128)

        override val primaryKey = PrimaryKey(id)
    }

    object Teacher : Table() {
        val id = integer("seqno").check("check_global_person_id") { it lessEq 999999 and (it greaterEq 100000)}.uniqueIndex("global_person_id")
        val name = varchar("full_name", 128)
        val email = varchar("email", 64).match("""^[a-zA-Z0-9._%+-]+@(niuitmo\.ru|itmo\.ru)$""")
        val subject = integer("subject").references(Subject.id)

        override val primaryKey = PrimaryKey(id)
    }

    object PersonalRating : Table() {
        val id = integer("seqno").autoIncrement()
        val ratingId = integer("ratingId").references(TeacherRating.id)

        val kindness_rating = integer("kindness_rating").check("check_kindness_rating") { it greaterEq 0 and (it lessEq 5)}.nullable()
        val responsibility_rating = integer("responsibility_rating").check("check_responsibility_rating") { it greaterEq 0 and (it lessEq 5)}.nullable()
        val individuality_rating = integer("individuality_rating").check("check_individuality_rating") { it greaterEq 0 and (it lessEq 5)}.nullable()
        val humor_rating = integer("humor_rating").check("check_humor_rating") { it greaterEq 0 and (it lessEq 5)}.nullable()

        override val primaryKey = PrimaryKey(id)
    }

    object TeacherRating : Table() {
        val id = integer("seqno").autoIncrement()
        val user = integer("user").references(Users.id)
        val teacher = integer("teacher").references(Teacher.id)

        val global_rating = integer("global_rating").check("check_global_rating") { it greaterEq 0 and (it lessEq 5)}
        val labs_rating = integer("labs_rating").check("check_labs_rating") { it greaterEq 0 and (it lessEq 5)}.nullable()
        val hw_rating = integer("homework_rating").check("check_hw_rating") { it greaterEq 0 and (it lessEq 5)}.nullable()
        val exam_rating = integer("exam_rating").check("check_exam_rating") { it greaterEq 0 and (it lessEq 5)}.nullable()

        val comment = varchar("comment", 1024).nullable()
        val created = integer("created").default(System.currentTimeMillis().toInt())

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
            SchemaUtils.create(Codes)
            SchemaUtils.create(Subject)
            SchemaUtils.create(Teacher)
            SchemaUtils.create(PersonalRating)
            SchemaUtils.create(TeacherRating)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
