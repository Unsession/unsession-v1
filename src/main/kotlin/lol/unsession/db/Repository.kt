package lol.unsession.db

import kotlinx.datetime.Clock
import lol.unsession.Utils
import lol.unsession.db.Repository.Reviews.withRating
import lol.unsession.db.UnsessionSchema.*
import lol.unsession.db.UnsessionSchema.Companion.dbQuery
import lol.unsession.db.UnsessionSchema.Teacher.create
import lol.unsession.db.UnsessionSchema.Users.id
import lol.unsession.db.models.Paging
import lol.unsession.db.models.ReviewDto
import lol.unsession.db.models.TeacherDto
import lol.unsession.db.models.UserDto
import lol.unsession.db.models.UserDto.Companion.toUser
import lol.unsession.db.models.client.Review
import lol.unsession.plugins.logger
import lol.unsession.security.code.CodeUtils
import lol.unsession.security.permissions.Roles
import lol.unsession.security.user.User
import lol.unsession.security.utils.Crypto
import lol.unsession.test.TestSDK
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface TeachersRepoInterface {
    suspend fun getTeacher(id: Int): TeacherDto? // single object
    suspend fun getTeachers(paging: Paging): List<TeacherDto>
    suspend fun addTeacher(teacher: TeacherDto): TeacherDto?
    suspend fun searchTeachers(prompt: String, paging: Paging): List<TeacherDto>
}

interface ReviewsRepoInterface {
    suspend fun getReview(id: Int): Review? // single object
    suspend fun getReviews(paging: Paging): List<Review>
    suspend fun getReviewsByTeacher(teacherId: Int, paging: Paging): List<Review>
    suspend fun getReviewsByUser(userId: Int, paging: Paging): List<Review>
    suspend fun addReview(review: ReviewDto): ReviewDto?
    suspend fun calculateRatingForTeacher(teacherId: Int): Double?
    suspend fun TeacherDto.withRating(): TeacherDto {
        return TeacherDto(
            this.id,
            this.name,
            this.email,
            this.department,
            calculateRatingForTeacher(this.id)
        )
    }

    suspend fun removeReview(id: Int): Boolean
}

interface UsersDaoInterface {
    suspend fun checkUsernameExists(username: String): Boolean
    suspend fun checkEmailExists(email: String): Boolean
    suspend fun getUser(id: Int): User?
    suspend fun getUser(email: String, withPermissions: Boolean = true): User?
    suspend fun deleteUser(id: Int): Boolean
    suspend fun updateUserData(id: Int, updatedUser: User): Boolean
    suspend fun setRole(id: Int, role: Roles): Boolean
    suspend fun banUser(id: Int, reason: String, until: Int): Boolean
    suspend fun getUserLoginData(id: Int): User.UserLoginData?
    suspend fun getUserLoginData(email: String): User.UserLoginData?
    suspend fun tryRegisterUser(
        userLoginData: User.UserLoginData,
        ip: String,
        onSuccess: suspend (User) -> Unit,
        usernameExists: suspend () -> Unit = {},
        userExists: suspend () -> Unit = {},
        onFailure: suspend () -> Unit = {}
    ): Boolean

    suspend fun registerUser(userLoginData: User.UserLoginData, ip: String): User?
    suspend fun getUsers(): List<User>
    suspend fun removeUser(id: Int): Boolean
    suspend fun getUserCodeCreator(code: String): Repository.Users.UserCodeCreator?
    suspend fun addReferrer(userId: Int, referrerId: Int): Boolean
}

interface CodesRepoInterface {
    suspend fun create(userId: Int?, maxActivations: Int = Int.MAX_VALUE): String
    suspend fun create(userId: Int?, maxActivations: Int = Int.MAX_VALUE, validUntil: Int): String
    suspend fun activateCode(code: String, userId: Int): Boolean
    suspend fun getCodeByUser(userId: Int): String?
    suspend fun canActivateCode(code: String): Boolean
}

sealed class Repository {

    object Teachers : TeachersRepoInterface {
        override suspend fun getTeacher(id: Int): TeacherDto? {
            return dbQuery {
                Teacher.select { Teacher.id eq id }.map { Teacher.fromRow(it) }.firstOrNull()?.withRating()
            }
        }

        override suspend fun getTeachers(paging: Paging): List<TeacherDto> {
            return selectData(Teacher.selectAll(), paging.page, paging.size).map { Teacher.fromRow(it).withRating() }
        }

        override suspend fun addTeacher(teacher: TeacherDto): TeacherDto? {
            return create(teacher)
        }

        override suspend fun searchTeachers(prompt: String, paging: Paging): List<TeacherDto> {
            return selectData(
                Teacher.select {
                    (Teacher.name.lowerCase() like "%${prompt.lowercase()}%") or (Teacher.department.lowerCase() like "%${prompt.lowercase()}%")
                },
                paging.page,
                paging.size
            ).map { Teacher.fromRow(it).withRating() }
        }
    }

    object Reviews : ReviewsRepoInterface {
        /**
         * Если существует ревью с таким ид, то практически невозможно выпасть в null,
         * для этого схему нужно будет ломать, но вдруг я что-то не предусмотрел*/
        override suspend fun getReview(id: Int): Review? {
            return dbQuery {
                val review =
                    TeacherReview.select { TeacherReview.id eq id }.map { TeacherReview.fromRow(it) }.firstOrNull()
                        ?: return@dbQuery null
                val user = Users.getUser(review.userId)
                val teacher =
                    Teacher.select { Teacher.id eq review.teacherId }.map { Teacher.fromRow(it) }.firstOrNull()
                        ?: return@dbQuery null
                return@dbQuery user?.let { Review.fromReviewAndUser(review, it, teacher) } ?: return@dbQuery null
            }
        }

        override suspend fun getReviews(paging: Paging): List<Review> {
            return selectData(TeacherReview.selectAll(), paging.page, paging.size).map {
                TeacherReview.fromRow(it).toReview()
            }
        }

        override suspend fun getReviewsByTeacher(teacherId: Int, paging: Paging): List<Review> {
            return selectData(
                TeacherReview.select(TeacherReview.teacherId eq teacherId)
                    .orderBy(TeacherReview.created, SortOrder.DESC), paging.page, paging.size
            ).map {
                TeacherReview.fromRow(it).toReview()
            }
        }

        override suspend fun getReviewsByUser(userId: Int, paging: Paging): List<Review> {
            return selectData(TeacherReview.select(id eq userId), paging.page, paging.size).map {
                TeacherReview.fromRow(it).toReview()
            }
        }

        override suspend fun addReview(review: ReviewDto): ReviewDto? {
            return TeacherReview.create(review)
        }

        override suspend fun calculateRatingForTeacher(teacherId: Int): Double {
            val result =
                dbQuery {
                    TeacherReview.select { TeacherReview.teacherId eq teacherId }.map { TeacherReview.fromRow(it) }
                }
            if (result.isEmpty()) {
                return -1.0 // Нет ревью, нет рейтинга
            }
            return result.map { it.globalRating }.average()
        }

        override suspend fun removeReview(id: Int): Boolean {
            return dbQuery {
                TeacherReview.deleteWhere { TeacherReview.id eq id } > 0
            }
        }
    }

    object Users : UsersDaoInterface {

        override suspend fun checkUsernameExists(username: String): Boolean {
            return dbQuery {
                UnsessionSchema.Users.select { UnsessionSchema.Users.username eq username }.firstOrNull() != null
            }
        }

        override suspend fun checkEmailExists(email: String): Boolean {
            return dbQuery {
                UnsessionSchema.Users.select { UnsessionSchema.Users.email eq email }.firstOrNull() != null
            }
        }

        override suspend fun getUser(id: Int): User? {
            lateinit var user: ResultRow
            lateinit var permissions: List<String>
            return dbQuery {
                user = UnsessionSchema.Users
                    .select { UnsessionSchema.Users.id eq id }
                    .firstOrNull() ?: return@dbQuery null
                permissions = transaction {
                    UnsessionSchema.Users
                        .innerJoin(UsersPermissions)
                        .innerJoin(Permissions)
                        .select { UnsessionSchema.Users.id eq user[UnsessionSchema.Users.id] }
                        .map {
                            it[Permissions.name]
                        }
                }
                return@dbQuery UnsessionSchema.Users.fromRow(user, permissions).toUser()
            }
        }

        override suspend fun getUser(email: String, withPermissions: Boolean): User? {
            return dbQuery {
                var permissions: List<String> = listOf()
                val user = UnsessionSchema.Users
                    .select { UnsessionSchema.Users.email eq email }
                    .singleOrNull() ?: return@dbQuery null
                if (withPermissions) {
                    permissions = UsersPermissions.getPermissions(user[id])
                }
                UnsessionSchema.Users.fromRow(user, permissions)
            }?.toUser()
        }

        override suspend fun deleteUser(id: Int): Boolean {
            return dbQuery {
                UnsessionSchema.Users.deleteWhere { UnsessionSchema.Users.id eq id } > 0
            }
        }

        /**
         * @param updatedUser will update name and email.
         * For permissions use setRole() instead */
        override suspend fun updateUserData(id: Int, updatedUser: User): Boolean {
            class UpdateException : Exception() {
                override val message: String
                    get() = "Failed to update user data"
            }
            return try {
                Companion.dbQuery {
                    UnsessionSchema.Users.update({ UnsessionSchema.Users.id eq id }) {
                        it[username] = updatedUser.name
                        it[email] = updatedUser.userLoginData?.email ?: throw UpdateException()
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        override suspend fun tryRegisterUser(
            userLoginData: User.UserLoginData,
            ip: String,
            onSuccess: suspend (User) -> Unit,
            usernameExists: suspend () -> Unit,
            userExists: suspend () -> Unit,
            onFailure: suspend () -> Unit
        ): Boolean {
            if (!Repository.Codes.canActivateCode(userLoginData.code!!)) {
                onFailure()
                return false
            }
            val newUser = registerUser(userLoginData, ip)
            onSuccess(newUser)
            return true
        }

        @OptIn(ExperimentalStdlibApi::class)
        override suspend fun registerUser(userLoginData: User.UserLoginData, ip: String): User {
            val salt = Crypto.generateRandomSalt().toHexString()
            val pwdHash = Crypto.generateHash(userLoginData.password, salt)
            val UserDto = UserDto(
                id = -1,
                name = userLoginData.username!!,
                email = userLoginData.email,
                password = pwdHash,
                salt = salt,
                roleName = Roles.User.roleData.name,
                permissions = listOf(),
                bannedReason = null,
                bannedUntil = null,
                created = Clock.System.now().epochSeconds.toInt(),
                lastLogin = Clock.System.now().epochSeconds.toInt(),
                lastIp = ip,
            )
            val registeredUser = UnsessionSchema.Users.create(UserDto)
            // ReferralService.afterRegisterNewUser(registeredUser.id)
            // ReferralService.referralRegister(registeredUser, userLoginData.code!!)
            try {
                val newUser = getUser(userLoginData.email, withPermissions = false)
                val roleSet = setRole(newUser!!.id, Roles.User)
                if (!roleSet) {
                    throw Exception("Failed to set role for user ${newUser.id}")
                }
                return newUser
            } catch (e: Exception) {
                e.printStackTrace()
            }
            throw Exception("Failed to register user")
        }

        override suspend fun getUsers(): List<User> {
            return dbQuery {
                UnsessionSchema.Users.selectAll().map {
                    val permissions = UsersPermissions.getPermissions(it[id])
                    UnsessionSchema.Users.fromRow(it, permissions).toUser()
                }
            }
        }

        override suspend fun removeUser(id: Int): Boolean {
            return dbQuery {
                UnsessionSchema.Users.deleteWhere { UnsessionSchema.Users.id eq id } > 0
            }
        }

        data class UserCodeCreator(val refererCode: String, val id: Int)

        override suspend fun getUserCodeCreator(code: String): UserCodeCreator? {
            return dbQuery {
                val creatorId = UnsessionSchema.Code.select(UnsessionSchema.Code.code eq code).firstOrNull()
                    ?.get(UnsessionSchema.Code.creator)
                    ?: return@dbQuery null
                val token = UnsessionSchema.Code.select(UnsessionSchema.Code.creator eq creatorId).firstOrNull()
                    ?.get(UnsessionSchema.Code.code)
                    ?: return@dbQuery null
                UserCodeCreator(token, creatorId)
            }
        }

        override suspend fun addReferrer(userId: Int, referrerId: Int): Boolean {
            return dbQuery {
                UnsessionSchema.Users.update({ UnsessionSchema.Users.id eq userId }) {
                    it[referer] = referrerId
                } > 0
            }
        }

        override suspend fun setRole(id: Int, role: Roles): Boolean {
            return try {
                Companion.dbQuery {
                    val newRolePermissions = Permissions.select {
                        Permissions.name inList role.roleData.permissions.map { it.name }
                    }.map {
                        it[Permissions.id]
                    }
                    logger.info("New role permissions: $newRolePermissions")
                    UsersPermissions.deleteWhere {
                        userId eq id
                    }
                    logger.info("Deleted old permissions")
                    UnsessionSchema.Users.update({ UnsessionSchema.Users.id eq id }) {
                        it[roleName] = role.roleData.name
                    }
                    logger.info("Updated role")
                    newRolePermissions.forEach { permissionId ->
                        UsersPermissions.insert {
                            it[userId] = id
                            it[this.permissionId] = permissionId
                            it[active] = true
                            it[assigned] = Clock.System.now().epochSeconds.toInt()
                        }
                    }
                    logger.info("Inserted new permissions")
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("Failed to set role for user $id")
                false
            }
        }

        override suspend fun banUser(id: Int, reason: String, until: Int): Boolean {
            return Companion.dbQuery {
                UnsessionSchema.Users.update({ UnsessionSchema.Users.id eq id }) {
                    it[bannedReason] = reason
                    it[bannedUntil] = until
                } > 0
            }
        }

        override suspend fun getUserLoginData(id: Int): User.UserLoginData? {
            return Companion.dbQuery {
                UnsessionSchema.Users.select { UnsessionSchema.Users.id eq id }.firstOrNull()?.let {
                    User.UserLoginData(
                        it[UnsessionSchema.Users.username],
                        it[UnsessionSchema.Users.email],
                        it[UnsessionSchema.Users.password],
                        it[UnsessionSchema.Users.salt],
                        null
                    )
                }
            }
        }

        override suspend fun getUserLoginData(email: String): User.UserLoginData? {
            return Companion.dbQuery {
                UnsessionSchema.Users.select { UnsessionSchema.Users.email eq email }.firstOrNull()?.let {
                    User.UserLoginData(
                        it[UnsessionSchema.Users.username],
                        it[UnsessionSchema.Users.email],
                        it[UnsessionSchema.Users.password],
                        it[UnsessionSchema.Users.salt],
                        null
                    )
                }
            }
        }
    }

    object HolyTestObject {
        suspend fun generateTestData(onExecuted: suspend (String) -> Unit) {
            val teachersId = mutableListOf<Int>()
            repeat(20) {
                teachersId.add(
                    Teachers.addTeacher(
                        TeacherDto(TestSDK.globalPersonId, TestSDK.name, TestSDK.email, TestSDK.department)
                    )!!.id
                )
            }
            val code = "NORTON"
            repeat(5) {
                Users.tryRegisterUser(
                    User.UserLoginData(
                        TestSDK.username,
                        TestSDK.email,
                        TestSDK.password,
                        null,
                        code
                    ),
                    "0.0.0.0",
                    onSuccess = { user ->
                        repeat(10) {
                            Reviews.addReview(
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
    }

    object Global {
        fun dropDatabase() {
            transaction {
                SchemaUtils.dropDatabase("unsession")
            }
        }
    }

    object Notifications {
        suspend fun updateFcm(userId: Int, token: String) {
            dbQuery {
                if (FCM.select(FCM.userId eq userId).empty()) {
                    FCM.insert {
                        it[FCM.userId] = userId
                        it[FCM.token] = token
                    }
                } else {
                    FCM.update({ FCM.userId eq userId }) {
                        it[FCM.token] = token
                    }
                }
            }
        }
    }

    object Codes : CodesRepoInterface {
        private const val DEFAULT_VALID = 2051218800 // 12.12.2034 23:00:00

        override suspend fun create(userId: Int?, maxActivations: Int): String {
            val code = CodeUtils.generateCode()
            val now = Clock.System.now().epochSeconds
            dbQuery {
                UnsessionSchema.Code.insert {
                    userId.let { id -> it[Code.creator] = id }
                    it[Code.code] = code
                    it[Code.maxActivations] = maxActivations
                    it[Code.validUntil] = DEFAULT_VALID
                }
            }
            return code
        }

        override suspend fun create(userId: Int?, maxActivations: Int, validUntil: Int): String {
            val code = CodeUtils.generateCode()
            val now = Clock.System.now().epochSeconds
            dbQuery {
                UnsessionSchema.Code.insert {
                    userId.let { id -> it[Code.creator] = id }
                    it[Code.code] = code
                    it[Code.maxActivations] = maxActivations
                    it[Code.validUntil] = validUntil
                }
            }
            return code
        }

        override suspend fun activateCode(code: String, userId: Int): Boolean {
            if (!canActivateCode(code)) return false
            val now = Utils.now
            val result = dbQuery {
                UnsessionSchema.Code.select { UnsessionSchema.Code.code eq code }.firstOrNull()
            } ?: return false
            dbQuery {
                UnsessionSchema.Code.update({ UnsessionSchema.Code.code eq code }) {
                    it[UnsessionSchema.Code.activations] = result[UnsessionSchema.Code.activations] + 1
                }
            }
            return true
        }

        override suspend fun getCodeByUser(userId: Int): String? {
            val now = Clock.System.now().epochSeconds
            val result = dbQuery {
                logger.error("AAAAAA - User id: $userId")
                UnsessionSchema.Code.select { UnsessionSchema.Code.creator eq userId }.firstOrNull()
            } ?: return null
            return result[UnsessionSchema.Code.code]
        }

        override suspend fun canActivateCode(code: String): Boolean {
            val now = Clock.System.now().epochSeconds
            val result = dbQuery {
                UnsessionSchema.Code.select { UnsessionSchema.Code.code eq code }.firstOrNull()
            } ?: return false
            if (result[UnsessionSchema.Code.validUntil] < now) {
                return false
            }
            if (result[UnsessionSchema.Code.activations] >= result[UnsessionSchema.Code.maxActivations]) {
                return false
            }
            return true
        }
    }
}
