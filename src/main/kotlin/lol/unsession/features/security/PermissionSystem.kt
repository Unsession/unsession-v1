package lol.unsession.features.security

import lol.unsession.features.security.Access.*

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
    Banned(
        AccountRole(
            "Banned",
            bannedP.toHashSet(),
            "Banned user has no access to anything.",
        )
    ),
    User(
        AccountRole(
            "User",
            userP.toHashSet(),
            "User has general read permissions for 'Legit content'",
        )
    ),
    ExtendedUser(
        AccountRole(
            "ExtendedUser",
            eUserP.toHashSet(),
            "User has general read permissions for 'Legit content' + teachers adding",
        )
    ),
    VerifiedUser(
        AccountRole(
            "VerifiedUser",
            vUserP.toHashSet(),
            "",
        )
    ),
    PaidUser(
        AccountRole(
            "PaidUser",
            pUserP.toHashSet(),
            "PaidUser has access to everything except for adding new admins.",
        )
    ),
    Admin(
        AccountRole(
            "Admin",
            adminP.toHashSet(),
            "Admin has access to everything except for adding new admins.",
        )
    ),
    Superuser(
        AccountRole(
            "Superuser",
            Access.entries.toTypedArray().toHashSet(), // все возможные права даём суперу
            "Superuser has access to everything.",
        )
    ),
}
