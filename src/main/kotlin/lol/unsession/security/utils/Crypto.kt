package lol.unsession.security.utils

import lol.unsession.getConfig
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

    private val ALGORITHM = getConfig().hashAlgorithm
    private val ITERATIONS = getConfig().hashIterations
    private val KEY_LENGTH = getConfig().hashKeyLength
    private val SECRET = getConfig().secret

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
