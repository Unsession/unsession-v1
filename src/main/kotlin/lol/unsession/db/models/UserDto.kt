package lol.unsession.db.models

import kotlinx.serialization.Serializable
import lol.unsession.security.Access
import lol.unsession.security.user.User

@Serializable
data class UserDto (
    val id: Int,
    val name: String,
    val email: String,
    val password: String,
    val salt: String,
    val permissions: List<String>,
    val roleName: String,
    val bannedReason: String?,
    val bannedUntil: Int?,
    val created: Int,
    val lastLogin: Int,
    val lastIp: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserDto

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

    companion object {
        fun UserDto.toUser(): User {
            return User(
                this.id,
                this.name,
                User.UserLoginData(
                    this.name,
                    this.email,
                    this.password,
                    this.salt,
                ),
                this.permissions.map { Access.valueOf(it) }.toHashSet(),
                this.roleName,
                if (this.bannedUntil != null && this.bannedReason != null) {
                    User.BanData(
                        this.bannedUntil,
                        this.bannedReason,
                    )
                } else {
                    null
                },
                this.created,
                this.lastLogin,
                this.lastIp
            )
        }
    }
}
