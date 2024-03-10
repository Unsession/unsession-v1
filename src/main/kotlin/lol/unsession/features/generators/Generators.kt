package lol.unsession.features.generators

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lol.unsession.models.UserDto
import lol.unsession.models.UserDto.Companion.toUser

val user = UserDto(
    id = 1,
    name = "test",
    email = "",
    password = "",
    salt = "",
    permissions = listOf("SS"),
    roleName = "SUPERUSER",
    bannedReason = null,
    bannedUntil = null,
    created = 0,
    lastLogin = 0,
    lastIp = "",
)

fun main() {
    while (true) {
        val line = readlnOrNull() ?: break
        when (line) {
            "Exit" -> break
            "UserDto" -> {
                val jsonString = Json.encodeToString(user)
                println(jsonString)
            }
            "User" -> {
                val jsonString = Json.encodeToString(user.toUser())
                println(jsonString)
            }
        }
    }
}
