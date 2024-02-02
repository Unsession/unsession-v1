package lol.unsession.db

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import lol.unsession.db.UnsessionSchema.Companion.dbQuery
import lol.unsession.db.models.ReviewDto
import lol.unsession.db.models.TeacherDto
import lol.unsession.db.models.UserDto
import lol.unsession.security.permissions.Access
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

// генерал рандом ахахахаха
//val generalRandom = Random(System.getenv("coderndseed").toInt())

class UnsessionSchema(private val database: Database) {
    object Users : Table("Users") {

        val id = integer("id").autoIncrement("users_id_seq").uniqueIndex()
        val username = varchar("username", 64)
        val email = varchar("email", 64).check {
            it like "%@niuitmo.ru" or (it like "%@itmo.ru")
        }

        val password = varchar("hash", 128)
        val salt = varchar("salt", 16 * 8)

        val roleName = varchar("roleName", 32)

        val bannedReason = varchar("bannedReason", 255).nullable()
        val bannedUntil = integer("bannedUntil").nullable()

        val created = integer("created")

        val lastLogin = integer("last_login")
        val lastIp = varchar("last_ip", 15)

        val referer = integer("referer").references(id).nullable()

        override val primaryKey = PrimaryKey(id)

        suspend fun create(user: UserDto): UserDto? {
            if (dbQuery {
                    Users.insert {
                        it[username] = user.name
                        it[email] = user.email
                        it[password] = user.password
                        it[salt] = user.salt
                        it[roleName] = user.roleName
                        it[bannedReason] = user.bannedReason
                        it[bannedUntil] = user.bannedUntil
                        it[created] = user.created
                        it[lastLogin] = user.lastLogin
                        it[lastIp] = user.lastIp
                    }
                }.insertedCount == 1)
                return user
            return null
        }

        fun fromRow(row: ResultRow, permissions: List<String>): UserDto {
            return UserDto(
                row[id],
                row[username],
                row[email],
                row[password],
                row[salt],
                permissions,
                row[roleName],
                row[bannedReason],
                row[bannedUntil],
                row[created],
                row[lastLogin],
                row[lastIp],
            )
        }
    }

    object Permissions : Table() {
        val id = integer("id").autoIncrement().uniqueIndex()
        val name = varchar("name", 64).uniqueIndex()
        val description = varchar("description", 255)

        override val primaryKey = PrimaryKey(id)

        fun insertPermissions() {
            if (Permissions.selectAll().count().toInt() > 0) return
            Access.entries.forEach { access ->
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
        fun getPermissions(userId: Int): List<String> {
            return transaction {
                Users
                    .innerJoin(UsersPermissions)
                    .innerJoin(Permissions)
                    .select { Users.id eq userId }
                    .map {
                        it[Permissions.name]
                    }
            }

        }

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
        val id =
            integer("id").autoIncrement().check("check_global_person_id") { it lessEq 999999 and (it greaterEq 100000) }
                .uniqueIndex("global_person_id")
        val name = varchar("full_name", 256)
        val email = varchar("email", 64).check { it like "%@niuitmo.ru" or (it like "%@itmo.ru") }
            .nullable()
        val department = varchar("department", 128)

        fun fromRow(row: ResultRow): TeacherDto {
            return TeacherDto(
                row[id],
                row[name],
                row[email],
                row[department],
            )
        }

        suspend fun create(teacher: TeacherDto): TeacherDto? {
            if (
                dbQuery {
                    Teacher.insert {
                        it[id] = teacher.id
                        it[name] = teacher.name
                        it[email] = teacher.email
                        it[department] = teacher.department
                    }
                }.insertedCount == 1) {
                return teacher
            }
            return null
        }

        override val primaryKey = PrimaryKey(id)
    }

    object TeacherReview : Table() {
        val id = integer("id").autoIncrement().uniqueIndex()
        val userId = integer("user").references(Users.id)
        val teacherId = integer("teacher").references(Teacher.id)

        val global_rating = integer("global_review").check("check_global_review") { it greaterEq 0 and (it lessEq 5) }
        val labs_rating =
            integer("labs_review").check("check_labs_review") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val hw_rating =
            integer("homework_review").check("check_hw_review") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val exam_rating =
            integer("exam_review").check("check_exam_review") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val kindness_rating =
            integer("kindness_review").check("check_kindness_review") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val responsibility_rating =
            integer("responsibility_review").check("check_responsibility_review") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val individuality_rating =
            integer("individuality_review").check("check_individuality_review") { it greaterEq 0 and (it lessEq 5) }
                .nullable()
        val humor_rating =
            integer("humor_review").check("check_humor_review") { it greaterEq 0 and (it lessEq 5) }
                .nullable()

        val comment = varchar("comment", 1024).nullable()
        val created = integer("created").default(Clock.System.now().epochSeconds.toInt())

        fun fromRow(row: ResultRow): ReviewDto {
            return ReviewDto(
                row[id],
                row[userId],
                row[teacherId],
                row[global_rating],
                row[labs_rating],
                row[hw_rating],
                row[exam_rating],
                row[kindness_rating],
                row[responsibility_rating],
                row[individuality_rating],
                row[humor_rating],
                row[created],
                row[comment],
            )
        }

        suspend fun create(review: ReviewDto): ReviewDto? {
            if (
                dbQuery {
                    TeacherReview.insert {
                        it[userId] = review.userId
                        it[teacherId] = review.teacherId
                        it[global_rating] = review.globalRating
                        it[labs_rating] = review.labsRating
                        it[hw_rating] = review.hwRating
                        it[exam_rating] = review.examRating
                        it[kindness_rating] = review.kindness
                        it[responsibility_rating] = review.responsibility
                        it[individuality_rating] = review.individuality
                        it[humor_rating] = review.humour
                        it[created] = review.createdTimestamp
                        it[comment] = review.comment
                    }
                }.insertedCount == 1
            ) {
                return review
            }
            return null
        }

        override val primaryKey = PrimaryKey(id)
    }

    init {
        initial()
    }

    fun initial() {
        transaction(database) {
            SchemaUtils.create(Users, Codes, Teacher, TeacherReview, Permissions)
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

    fun wipeInitial() {
        transaction(database) {
            exec(
                """
                DROP SCHEMA public CASCADE;
                CREATE SCHEMA public;
            """.trimIndent()
            )
            SchemaUtils.create(Users, Codes, Teacher, TeacherReview, Permissions)
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

suspend fun selectData(
    query: Query,
    page: Int,
    pageSize: Int
): List<ResultRow> {
    return dbQuery {
        query.limit(pageSize, (page * pageSize).toLong())
        return@dbQuery query.toList()
    }
}

