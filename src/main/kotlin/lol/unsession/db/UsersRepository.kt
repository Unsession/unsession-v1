package lol.unsession.db

import kotlinx.datetime.Clock
import lol.unsession.db.UnsessionSchema.*
import lol.unsession.db.UnsessionSchema.Companion.dbQuery
import lol.unsession.db.models.UserDto
import lol.unsession.db.models.UserDto.Companion.toUser
import lol.unsession.security.Roles
import lol.unsession.security.user.User
import lol.unsession.security.utils.Crypto
import lol.unsession.utils.getLogger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

interface UsersDao {
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
        usernameExists: suspend () -> Unit,
        userExists: suspend () -> Unit,
        onFailure: suspend () -> Unit
    ): Boolean

    suspend fun registerUser(userLoginData: User.UserLoginData, ip: String): User?
}

class UsersRepositoryImpl : UsersDao {

    override suspend fun checkUsernameExists(username: String): Boolean {
        return dbQuery {
            Users.select { Users.username eq username }.firstOrNull() != null
        }
    }

    override suspend fun checkEmailExists(email: String): Boolean {
        return dbQuery {
            Users.select { Users.email eq email }.firstOrNull() != null
        }
    }

    override suspend fun getUser(id: Int): User? {
        // select users and join with permissions
        try {
            lateinit var user: ResultRow
            lateinit var permissions: List<String>
            dbQuery {
                user = Users
                    .select { Users.id eq id }
                    .firstOrNull()!!
                permissions = Users
                    .innerJoin(Permissions)
                    .select { Users.id eq id }
                    .map {
                        it[Permissions.name]
                    }
            }
            return UserDto(
                user[Users.id],
                user[Users.username],
                user[Users.email],
                user[Users.password],
                user[Users.salt],
                permissions,
                user[Users.roleName],
                user[Users.bannedReason],
                user[Users.bannedUntil],
                user[Users.created],
                user[Users.last_login],
                user[Users.last_ip],
            ).toUser()
        } catch (e: Exception) {
            return null
        }
    }

    override suspend fun getUser(email: String, withPermissions: Boolean): User? {
        try {
            lateinit var user: ResultRow
            var permissions: List<String> = listOf()
            return dbQuery {
                user = Users
                    .select { Users.email eq email }
                    .singleOrNull() ?: throw Exception("User not found")
                getLogger("UsersRepo").info("requestedUser: ${user[Users.id]} ${user[Users.username]} ${user[Users.email]} ${user[Users.password]} ${user[Users.salt]} ${user[Users.roleName]} ${user[Users.bannedReason]} ${user[Users.bannedUntil]} ${user[Users.created]} ${user[Users.last_login]} ${user[Users.last_ip]}}")
                if (withPermissions) {
                    permissions = Users
                        .innerJoin(UsersPermissions)
                        .innerJoin(Permissions)
                        .select { Users.id eq user[Users.id] }
                        .map {
                            it[Permissions.name]
                        }

                }
                return@dbQuery UserDto(
                    user[Users.id],
                    user[Users.username],
                    user[Users.email],
                    user[Users.password],
                    user[Users.salt],
                    permissions,
                    user[Users.roleName],
                    user[Users.bannedReason],
                    user[Users.bannedUntil],
                    user[Users.created],
                    user[Users.last_login],
                    user[Users.last_ip],
                ).toUser()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override suspend fun deleteUser(id: Int): Boolean {
        return dbQuery {
            Users.deleteWhere { Users.id eq id } > 0
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
        try {
            dbQuery {
                Users.update({ Users.id eq id }) {
                    it[username] = updatedUser.name
                    it[email] = updatedUser.userLoginData?.email ?: throw UpdateException()
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
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
        checkEmailExists(userLoginData.email).let {
            if (it) {
                userExists()
                onFailure()
                return false
            }
        }
        checkUsernameExists(userLoginData.username).let {
            if (it) {
                usernameExists()
                onFailure()
                return false
            }
        }
        val newUser = registerUser(userLoginData, ip)
        onSuccess(newUser)
        getLogger("Auth").info("Registered user ${userLoginData.username} (${userLoginData.email})")
        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun registerUser(userLoginData: User.UserLoginData, ip: String): User {
        val salt = Crypto.generateRandomSalt().toHexString()
        val pwdHash = Crypto.generateHash(userLoginData.password, salt)
        dbQuery {
            Users.insert {
                it[id] = -1
                it[username] = userLoginData.username
                it[email] = userLoginData.email
                it[password] = pwdHash
                it[this.salt] = salt
                it[roleName] = Roles.User.roleData.name
                it[bannedReason] = null
                it[bannedUntil] = null
                it[created] = Clock.System.now().epochSeconds.toInt()
                it[last_login] = Clock.System.now().epochSeconds.toInt()
                it[last_ip] = ip
            }
        }
        val newUser = getUser(userLoginData.email, withPermissions = false)
        val roleSet = setRole(newUser!!.id, Roles.User)
        if (!roleSet) {
            throw Exception("Failed to set role for user ${newUser.id}")
        }
        return newUser
    }

    override suspend fun setRole(id: Int, role: Roles): Boolean {
        return try {
            dbQuery {
                val newRolePermissions = Permissions.select {
                    Permissions.name inList role.roleData.permissions.map { it.name }
                }.map {
                    it[Permissions.id]
                }
                UsersPermissions.deleteWhere {
                    userId eq id
                }
                Users.update({ Users.id eq id }) {
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
        return dbQuery {
            Users.update({ Users.id eq id }) {
                it[bannedReason] = reason
                it[bannedUntil] = until
            } > 0
        }
    }

    override suspend fun getUserLoginData(id: Int): User.UserLoginData? {
        return dbQuery {
            Users.select { Users.id eq id }.firstOrNull()?.let {
                User.UserLoginData(
                    it[Users.username],
                    it[Users.email],
                    it[Users.password],
                    it[Users.salt],
                )
            }
        }
    }

    override suspend fun getUserLoginData(email: String): User.UserLoginData? {
        return dbQuery {
            Users.select { Users.email eq email }.firstOrNull()?.let {
                User.UserLoginData(
                    it[Users.username],
                    it[Users.email],
                    it[Users.password],
                    it[Users.salt],
                )
            }
        }
    }
}
