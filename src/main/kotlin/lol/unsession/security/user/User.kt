package lol.unsession.security.user

import lol.unsession.security.Access

class User (
    val id: Int,
    val name: String,
    val email: String,
    val permissions: HashSet<Access>,
    val roleName: String,
    val isBanned: Boolean,
    val bannedReason: String,
    val bannedUntil: Int,
    val created: Int,
    val lastLogin: Int,
    val lastIp: String,
) {
    fun hasAccess(access: Access): Boolean {
        return this.permissions.contains(access)
    }

    fun hasAccess(access: Collection<Access>): Boolean {
        return this.permissions.containsAll(access)
    }

    fun addAccess(access: Access) {
        this.permissions.add(access)
    }

    fun removeAccess(access: Access) {
        this.permissions.remove(access)
    }

    fun checkPermission(access: Access, action: () -> Unit, onFailure: () -> Unit) {
        if (hasAccess(access)) {
            action()
        }
    }

    fun checkPermission(access: Collection<Access>, action: () -> Unit, onFailure: () -> Unit) {
        if (hasAccess(access)) {
            action()
        }
    }
}