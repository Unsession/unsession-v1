package lol.unsession.db

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import lol.unsession.db.UnsessionSchema.Users.uniqueIndex
import lol.unsession.security.permissions.Access
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.match
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

// генерал рандом ахахахаха
val generalRandom = Random(System.getenv("coderndseed").toInt())

class UnsessionSchema(private val database: Database) {
    object Users : Table("Users") {
        val id = integer("id").autoIncrement("users_id_seq").uniqueIndex()
        val username = varchar("username", 64)
        val email = varchar("email", 64).check {
            it like "%@niuitmo.ru" or (it like "%@itmo.ru")
        }

        val password = varchar("hash", 128)
        val salt = varchar("salt", 16*8)

        val roleName = varchar("roleName", 32)

        val bannedReason = varchar("bannedReason", 255).nullable()
        val bannedUntil = integer("bannedUntil").nullable()

        val created = integer("created")

        val last_login = integer("last_login")
        val last_ip = varchar("last_ip", 15)

        val referer = integer("referer").references(id).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    object Permissions : Table() {
        val id = integer("id").autoIncrement().uniqueIndex()
        val name = varchar("name", 5).uniqueIndex()
        val description = varchar("description", 255)

        override val primaryKey = PrimaryKey(id)

        fun insertPermissions() {
            if (Permissions.selectAll().count().toInt() > 0) return
            Access.entries.forEach{ access ->
                transaction {
                    Permissions.insert {
                        it[name] = access.name
                        it[description] = "Permission for ${access.name} access."
                    }
                }
            }
        }
    }

    object UsersPermissions : Table() {
        val id = integer("id").autoIncrement().uniqueIndex()
        val userId = integer("user_id").references(Users.id)
        val permissionId = integer("permission_id").references(Permissions.id)

        override val primaryKey = PrimaryKey(id)
    }

    object Codes : Table() {
        val id = integer("id").autoIncrement().uniqueIndex()
        val creator = integer("creator_id").references(Users.id)

        val code = varchar("code", 4) //.default(generalRandom.nextBits(4).toString())
        val activations = integer("activations").default(0)
        val maxActivations = integer("maxActivations").default(1)
        val validUntil = integer("validUntil")

        override val primaryKey = PrimaryKey(id)
    }

    object Teacher : Table() {
        val id = integer("id").check("check_global_person_id") { it lessEq 999999 and (it greaterEq 100000) }
            .uniqueIndex("global_person_id")
        val name = varchar("full_name", 256)
        val email = varchar("email", 64).check { it like "%@niuitmo.ru" or (it like "%@itmo.ru") }
            .nullable()
        val department = varchar("department", 128)

        override val primaryKey = PrimaryKey(id)
    }

    object PersonalRating : Table() {
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
            integer("labs_rating").check("check_labs_rating") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val hw_rating =
            integer("homework_rating").check("check_hw_rating") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val exam_rating =
            integer("exam_rating").check("check_exam_rating") { it greaterEq 0 and (it lessEq 5) }
                .nullable()

        val comment = varchar("comment", 1024).nullable()
        val created = integer("created").default(Clock.System.now().epochSeconds.toInt())

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
            SchemaUtils.create(Codes)
            SchemaUtils.create(Teacher)
            SchemaUtils.create(PersonalRating)
            SchemaUtils.create(TeacherRating)
            SchemaUtils.create(Permissions)
            Permissions.insertPermissions()
            SchemaUtils.create(UsersPermissions)
            exec(
                """
                CREATE OR REPLACE FUNCTION assign_new_id()
                RETURNS TRIGGER AS ${'$'}${'$'}
                BEGIN
                    IF NEW.id = -1 THEN
                        NEW.id = nextval('users_id_seq');
                    END IF;
                    RETURN NEW;
                END;
                ${'$'}${'$'} LANGUAGE plpgsql;

                CREATE OR REPLACE TRIGGER assign_new_id_before_insert
                BEFORE INSERT ON Users
                FOR EACH ROW
                EXECUTE FUNCTION assign_new_id();
            """.trimIndent()
            )
        }
    }

    companion object {
        suspend fun <T> dbQuery(block: suspend (transaction: Transaction) -> T): T =
            newSuspendedTransaction(Dispatchers.IO) { block(this) }
    }
}
