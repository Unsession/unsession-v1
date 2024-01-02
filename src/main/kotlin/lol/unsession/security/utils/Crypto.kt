package lol.unsession.security.utils

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Crypto {
    fun generateRandomSalt(): ByteArray {
        val random = SecureRandom.getInstance("SHA1PRNG")
        val salt = ByteArray(16)
        random.nextBytes(salt)

        return salt
    }

    private const val ALGORITHM = "PBKDF2WithHmacSHA512"
    private const val ITERATIONS = 240_001
    private const val KEY_LENGTH = 512
    private val SECRET = System.getenv("secret")

    @OptIn(ExperimentalStdlibApi::class)
    fun generateHash(password: String, salt: String): String {
        val combinedSalt = "$salt$SECRET".toByteArray()

        val factory: SecretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM)
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), combinedSalt, ITERATIONS, KEY_LENGTH)
        val key: SecretKey = factory.generateSecret(spec)
        val hash: ByteArray = key.encoded

        return hash.toHexString()
    }

    fun checkPassword(inputPassword: String, storedHash: String, salt: String): Boolean {
        val generatedHash = generateHash(inputPassword, salt)
        return generatedHash == storedHash
    }
}
