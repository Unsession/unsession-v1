package lol.unsession.security.permissions

import lol.unsession.security.permissions.Access.*

data class AccountRole(
    val name: String,
    val permissions: HashSet<Access>,
    val description: String,
)

val bannedP = setOf<Access>()
val userP = setOf(
    Teachers,
    TeachersReviewing
) + bannedP
val eUserP = setOf(
    TeachersAdding
) + userP
val vUserP = setOf(
    TeachersReviewing,
    Homeworks,
    HomeworksAdding,
    Tests,
    TestsRating
) + userP
val pUserP = setOf(
    TestAnswers
) + vUserP
val adminP = setOf(
    Users,
    UsersAdding,
    UsersBlocking,
    UsersRolesChanging
) + pUserP

enum class Roles(val roleData: AccountRole) {
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
    ExtendedUser(
        AccountRole(
            "ExtendedUser",
            eUserP.toHashSet(),
            "User has general read permissions for 'Legit content' + teachers adding",
        )
    ),
    User(
        AccountRole(
            "User",
            userP.toHashSet(),
            "User has general read permissions for 'Legit content'",
        )
    ),
    Banned(
        AccountRole(
            "Banned",
            bannedP.toHashSet(),
            "Banned user has no access to anything.",
        )
    ),
}
