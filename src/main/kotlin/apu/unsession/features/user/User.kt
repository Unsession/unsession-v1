package apu.unsession.features.user

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import apu.unsession.features.db.UnsessionSchema
import apu.unsession.features.security.roles.Access
import apu.unsession.models.UserDto
import org.jetbrains.exposed.sql.ResultRow

@Serializable
class User(
    val id: Int,
    val name: String,
    var userLoginData: UserLoginData?,
    val permissions: HashSet<Access>,
    val roleName: String,
    var banData: BanData?,
    val created: Int,
    var lastLogin: Int?,
    var lastIp: String?,
) {

    @Serializable
    data class UserLoginData(
        val username: String?,
        val email: String,
        val password: String,
        val salt: String? = null
    ) {
        fun validate(): Boolean {
            return (email.contains("^[a-zA-Z0-9_.+-]+@(niuitmo.ru|itmo.ru)$".toRegex()) &&
                    password.length >= 8 &&
                    username.toString().length >= 4)
        }
    }

    @Serializable
    data class BanData(
        val bannedUntil: Int,
        val bannedReason: String,
    )

    val isBanned: Boolean
        get() {
            if (this.banData == null) return false
            return this.banData!!.bannedUntil >= Clock.System.now().epochSeconds.toInt()
        }

    fun clearPersonalData() {
        userLoginData = null
        lastIp = null
        lastLogin = null
    }

    fun hasAccess(access: Access): Boolean {
        if (isBanned) return false
        return this.permissions.contains(access)
    }

    fun hasAccess(access: Collection<Access>): Boolean {
        return this.permissions.containsAll(access)
    }

    companion object {
        fun ResultRow.toUser(permission: HashSet<Access>): UserDto {
            return UserDto(
                this[UnsessionSchema.Users.id],
                this[UnsessionSchema.Users.username],
                this[UnsessionSchema.Users.email],
                this[UnsessionSchema.Users.password],
                this[UnsessionSchema.Users.salt],
                permission.map { it.name },
                this[UnsessionSchema.Users.roleName],
                this[UnsessionSchema.Users.bannedReason],
                this[UnsessionSchema.Users.bannedUntil],
                this[UnsessionSchema.Users.created],
                this[UnsessionSchema.Users.lastLogin],
                this[UnsessionSchema.Users.lastIp],
            )
        }
    }
}
