package lol.unsession.security

import lol.unsession.security.Access.*

data class AccountRole(
    val roleName: String,
    val access: HashSet<Access>,
    val description: String,
) {
    fun hasAccess(access: Access): Boolean {
        return this.access.contains(access)
    }

    fun addAccess(access: Access) {
        this.access.add(access)
    }
}

val userP = setOf(
    TE,
    TE_R
)
val vUserP = setOf(
    TE_RE,
    HW,
    HW_A,
    T,
    T_R
) + userP
val pUserP = setOf(
    T_S,
    T_AN
) + vUserP
val adminP = setOf(
    U,
    U_A,
    U_B,
    U_RC
) + pUserP

enum class Roles(roleData: AccountRole) {
    Superuser(
        AccountRole(
            "Superuser",
            Access.entries.toTypedArray().toHashSet(), // все возможные права даём суперу
            "Superuser has access to everything.",
        )
    ),
    Admin(
        AccountRole(
            "Admin",
            adminP.toHashSet(),
            "Admin has access to everything except for adding new admins.",
        )
    ),
    PaidUser(
        AccountRole(
            "PaidUser",
            pUserP.toHashSet(),
            "PaidUser has access to everything except for adding new admins.",
        )
    ),
    VerifiedUser(
        AccountRole(
            "VerifiedUser",
            vUserP.toHashSet(),
            "",
        )
    ),
    User(
        AccountRole(
            "User",
            userP.toHashSet(),
            "User has general read permissions for 'Legit content'",
        )
    );
}
