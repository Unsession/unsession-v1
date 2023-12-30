package lol.unsession.security

import lol.unsession.db.models.UserDto
import lol.unsession.security.user.User

interface Content {
    val id: Int
    val name: String?
    val description: String?

    val permissions: HashSet<Access>

    fun hasAccess(user: User): Boolean {
        return user.hasAccess(permissions)
    }
}