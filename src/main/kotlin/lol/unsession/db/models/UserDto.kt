package lol.unsession.db.models

import kotlinx.serialization.Serializable
import lol.unsession.db.Repository
import lol.unsession.security.permissions.Access
import lol.unsession.security.user.User

@Serializable
data class UserDto(
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

    companion object {
        suspend fun UserDto.toUser(): User {
            val userCode = Repository.Codes.getCodeByUser(this.id) ?: ""
            val referrer = Repository.Users.getUser((Repository.Users.getUser(this.id)?.refererId) ?: -1)
            return User(
                this.id,
                this.name,
                User.UserLoginData(
                    this.name,
                    this.email,
                    this.password,
                    this.salt,
                    referrer?.refererCode,
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
                this.lastIp,
                userCode,
                referrer?.id,
            )
        }
    }
}
