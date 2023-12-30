package lol.unsession.db

import lol.unsession.db.models.UserDto
import lol.unsession.security.user.User

interface UsersDao {
    fun getUserById(id: Int): User?
    fun deleteUser(id: Int): Boolean
    fun updateUser(id: Int, updatedUser: User): User?
}