package lol.unsession.db.models

import lol.unsession.security.Access
import lol.unsession.security.user.User

data class UserDto (
    val id: Int,
    val name: String,
    val email: String,
    val password: String,
    val salt: String,
    val permissions: BooleanArray,
    val roleName: String,
    val isBanned: Boolean,
    val bannedReason: String,
    val bannedUntil: Int,
    val created: Int,
    val lastLogin: Int,
    val lastIp: String,
) {
    fun UserDto.toUser(): User {
        return User(
            this.id,
            this.name,
            this.email,
            Access.deserialize(permissions).toHashSet(),
            this.roleName,
            this.isBanned,
            this.bannedReason,
            this.bannedUntil,
            this.created,
            this.lastLogin,
            this.lastIp,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserDto

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}