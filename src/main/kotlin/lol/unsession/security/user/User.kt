package lol.unsession.security.user

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import lol.unsession.db.UnsessionSchema
import lol.unsession.db.models.UserDto
import lol.unsession.security.Access
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
    val lastLogin: Int,
    val lastIp: String,
) {

    @Serializable
    data class UserLoginData(
        val username: String,
        val email: String,
        val password: String,
        val salt: String? = null,
    )

    @Serializable
    data class BanData(
        val bannedUntil: Int,
        val bannedReason: String,
    )

    val isBanned: Boolean
        get() {
            if (this.banData == null) return false
            if (this.banData!!.bannedUntil < Clock.System.now().epochSeconds.toInt()) return false
            return true
        }

    fun clearLoginData() {
        this.userLoginData = null
    }

    fun clearBanData() {
        this.banData = null
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
                this[UnsessionSchema.Users.last_login],
                this[UnsessionSchema.Users.last_ip],
            )
        }
    }
}
