package lol.unsession.security.code

import java.security.SecureRandom

object CodeUtils {
    private val random = SecureRandom().apply {
        setSeed(System.currentTimeMillis()+1488)
    }
    fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map {
            chars[random.nextInt(chars.length-1)]
        }.joinToString("")
    }
}
