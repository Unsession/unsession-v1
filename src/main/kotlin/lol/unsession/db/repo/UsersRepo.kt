package lol.unsession.db.repo

import lol.unsession.db.UnsessionSchema
import lol.unsession.db.UnsessionSchema.Companion.dbQuery
import lol.unsession.db.models.UserDto
import lol.unsession.db.repo.UsersRepo.Companion.asUserDto
import lol.unsession.security.Access.Companion.serialize
import lol.unsession.security.user.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

interface UsersRepo {
    suspend fun getUserById(id: Int): User?
    suspend fun deleteUser(id: Int): Boolean
    suspend fun updateUser(id: Int, updatedUser: User): Boolean
    suspend fun registerUser(newUser: User): Boolean
    companion object {
        fun ResultRow.asUserDto(): UserDto {
            return UserDto(
                id = this[UnsessionSchema.Users.id],
                name = this[UnsessionSchema.Users.name],
                email = this[UnsessionSchema.Users.email],
                password = this[UnsessionSchema.Users.password],
                salt = this[UnsessionSchema.Users.salt],
                permissions = this[UnsessionSchema.Users.permissions],
                group = this[UnsessionSchema.Users.group],
                isBanned = this[UnsessionSchema.Users.isBanned],
                bannedReason = this[UnsessionSchema.Users.bannedReason],
                bannedUntil = this[UnsessionSchema.Users.bannedUntil],
                created = this[UnsessionSchema.Users.created],
                lastLogin = this[UnsessionSchema.Users.lastLogin],
                lastIp = this[UnsessionSchema.Users.lastIp],
            )
        }
    }
}

class UsersRepoImpl : UsersRepo {
    override suspend fun getUserById(id: Int): User? = dbQuery {
        UnsessionSchema.Users.select(UnsessionSchema.Users.id.eq(id)).firstOrNull()?.asUserDto()?.toUser()
    }

    override suspend fun deleteUser(id: Int): Boolean = dbQuery {
        UnsessionSchema.Users.deleteWhere { UnsessionSchema.Users.id eq id } > 0
    }

    override suspend fun updateUser(id: Int, updatedUser: User): Boolean = dbQuery {
        UnsessionSchema.Users.update({ UnsessionSchema.Users.id eq id }) {
            it[name] = updatedUser.name
            it[email] = updatedUser.email
            it[password] = updatedUser.password
            it[salt] = updatedUser.salt
            it[permissions] = serialize(updatedUser.permissions)
            it[group] = updatedUser.group.name
            it[isBanned] = updatedUser.isBanned
            it[bannedReason] = updatedUser.bannedReason
            it[bannedUntil] = updatedUser.bannedUntil
            it[created] = updatedUser.created
            it[lastLogin] = updatedUser.lastLogin
            it[lastIp] = updatedUser.lastIp
        } > 0
    }

    override suspend fun registerUser(newUser: User): Boolean = dbQuery {
        UnsessionSchema.Users.insertIgnore {
            it[name] = newUser.name
            it[email] = newUser.email
            it[password] = newUser.password
            it[salt] = newUser.salt
            it[permissions] = serialize(newUser.permissions)
            it[group] = newUser.group.name
            it[isBanned] = newUser.isBanned
            it[bannedReason] = newUser.bannedReason
            it[bannedUntil] = newUser.bannedUntil
            it[created] = newUser.created
            it[lastLogin] = newUser.lastLogin
            it[lastIp] = newUser.lastIp
        }.resultedValues?.isNotEmpty() ?: false
    }
}
