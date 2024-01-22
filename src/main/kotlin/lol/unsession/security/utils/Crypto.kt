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
    private val SECRET = "Ejrr]&HrrCr^DLy:xOfx}o5}%3_;x=U\$/J<H<l,4NTIRImVYBTXqB\\BQ(xlJdznP_GnZ3N_7*_FJXERo[nK4<5WByGtJtD}&_PJh}frdL%{N:usAbg5B<9*]]g;s,Ug;payment.key=TC-INVOICE_ee257eb3dc175e6ee5f7a58f89a09954651d7ae67381847f4d0c6c47cb47db1188"//System.getenv("secret")

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
