package lol.unsession.db.repo

import kotlinx.datetime.Clock
import lol.unsession.containsAnyKey
import lol.unsession.containsAnyKeyNotIn
import lol.unsession.db.UnsessionSchema
import lol.unsession.db.UnsessionSchema.*
import lol.unsession.db.UnsessionSchema.Teacher.create
import lol.unsession.db.models.PagingFilterParameters
import lol.unsession.db.models.TeacherDto
import lol.unsession.db.models.UserDto
import lol.unsession.db.models.UserDto.Companion.toUser
import lol.unsession.db.models.client.Review
import lol.unsession.db.selectData
import lol.unsession.security.permissions.Roles
import lol.unsession.security.user.User
import lol.unsession.security.utils.Crypto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface TeachersRepoInterface {
    suspend fun getTeacher(id: Int): TeacherDto? // single object
    suspend fun getTeachers(paging: PagingFilterParameters): List<TeacherDto>
    suspend fun addTeacher(teacher: TeacherDto): TeacherDto?
    suspend fun searchTeachers(prompt: String, paging: PagingFilterParameters): List<TeacherDto>
}

interface ReviewsRepoInterface {
    suspend fun getReview(id: Int): Review? // single object
    suspend fun getReviews(paging: PagingFilterParameters): List<Review>
    suspend fun getReviewsByTeacher(teacherId: Int, paging: PagingFilterParameters): List<Review>
    suspend fun getReviewsByUser(userId: Int, paging: PagingFilterParameters): List<Review>
    suspend fun addReview(review: Review): Review?
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
}

sealed class Repository {

    object Teachers : TeachersRepoInterface {
        override suspend fun getTeacher(id: Int): TeacherDto? {
            return Teacher.select { Teacher.id eq id }.map { Teacher.fromRow(it) }.firstOrNull()
        }

        override suspend fun getTeachers(paging: PagingFilterParameters): List<TeacherDto> {
            return selectData(Teacher, paging).map { Teacher.fromRow(it) }
        }

        override suspend fun addTeacher(teacher: TeacherDto): TeacherDto? {
            return create(teacher)
        }

        override suspend fun searchTeachers(prompt: String, paging: PagingFilterParameters): List<TeacherDto> {
            return if ((paging.dataSelectParameters?.filters?.containsAnyKeyNotIn(Teacher.columns.map { it.name }) == true) && (paging.dataSelectParameters.filters.containsAnyKey(
                    TeacherReview.columns.map { it.name }))
            ) {
                selectData(Teacher.join(TeacherReview, JoinType.INNER, additionalConstraint = {
                    Teacher.id eq TeacherReview.teacherId
                }), paging).map { Teacher.fromRow(it) }
            } else {
                selectData(
                    columns = Teacher, query = Teacher.select { Teacher.name like "%$prompt%" }, pagingParameters = paging
                ).map { Teacher.fromRow(it) }
            }
        }
    }

    object Reviews : ReviewsRepoInterface {
        override suspend fun getReview(id: Int): Review? {
            val review = TeacherReview.select { TeacherReview.id eq id }.map { TeacherReview.fromRow(it) }.firstOrNull()
                ?: return null
            val user = Users.getUser(review.userId) ?: return null
            val teacher = Teacher.select { Teacher.id eq review.teacherId }.map { Teacher.fromRow(it) }.firstOrNull()
                ?: return null
            return Review.fromReviewAndUser(review, user, teacher)
        }

        override suspend fun getReviews(paging: PagingFilterParameters): List<Review> {
            return selectData(TeacherReview, paging).map { TeacherReview.fromRow(it).toReview() }
        }

        override suspend fun getReviewsByTeacher(teacherId: Int, paging: PagingFilterParameters): List<Review> {
            return selectData(Teacher, Teacher.select(Teacher.id eq teacherId), paging).map { TeacherReview.fromRow(it).toReview() }
        }

        override suspend fun getReviewsByUser(userId: Int, paging: PagingFilterParameters): List<Review> {
            return selectData(TeacherReview, TeacherReview.select(TeacherReview.userId eq userId), paging).map { TeacherReview.fromRow(it).toReview() }
        }

        override suspend fun addReview(review: Review): Review? {
            return TeacherReview.create(review.toReviewDto())?.toReview()
        }
    }

    object Users : UsersDaoInterface {

        override suspend fun checkUsernameExists(username: String): Boolean {
            return Companion.dbQuery {
                UnsessionSchema.Users.select { UnsessionSchema.Users.username eq username }.firstOrNull() != null
            }
        }

        override suspend fun checkEmailExists(email: String): Boolean {
            return Companion.dbQuery {
                UnsessionSchema.Users.select { UnsessionSchema.Users.email eq email }.firstOrNull() != null
            }
        }

        override suspend fun getUser(id: Int): User {
            lateinit var user: ResultRow
            lateinit var permissions: List<String>
            Companion.dbQuery {
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
            }
            return UnsessionSchema.Users.fromRow(user, permissions).toUser()
        }

        override suspend fun getUser(email: String, withPermissions: Boolean): User? {
            return Companion.dbQuery {
                var permissions: List<String> = listOf()
                val user = UnsessionSchema.Users
                    .select { UnsessionSchema.Users.email eq email }
                    .singleOrNull() ?: return@dbQuery null
                if (withPermissions) {
                    permissions = UsersPermissions.getPermissions(user[UnsessionSchema.Users.id])
                }
                UnsessionSchema.Users.fromRow(user, permissions)
            }?.toUser()
        }

        override suspend fun deleteUser(id: Int): Boolean {
            return Companion.dbQuery {
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
            val newUser = registerUser(userLoginData, ip)
            onSuccess(newUser)
            return true
        }

        @OptIn(ExperimentalStdlibApi::class)
        override suspend fun registerUser(userLoginData: User.UserLoginData, ip: String): User {
            val salt = Crypto.generateRandomSalt().toHexString()
            val pwdHash = Crypto.generateHash(userLoginData.password, salt)
            val newUserDto = UserDto(
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
            UnsessionSchema.Users.create(newUserDto)
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

        override suspend fun setRole(id: Int, role: Roles): Boolean {
            return try {
                Companion.dbQuery {
                    val newRolePermissions = Permissions.select {
                        Permissions.name inList role.roleData.permissions.map { it.name }
                    }.map {
                        it[Permissions.id]
                    }
                    UsersPermissions.deleteWhere {
                        userId eq id
                    }
                    UnsessionSchema.Users.update({ UnsessionSchema.Users.id eq id }) {
                        it[roleName] = role.roleData.name
                    }
                    newRolePermissions.forEach { permissionId ->
                        UsersPermissions.insert {
                            it[userId] = id
                            it[this.permissionId] = permissionId
                        }
                    }
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                    )
                }
            }
        }
    }
}
