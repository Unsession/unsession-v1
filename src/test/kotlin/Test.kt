import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import lol.unsession.security.utils.Crypto

fun main() {
    testCrypto()
}

fun testCrypto() {
    testA1()
    testA2()
}

@OptIn(ExperimentalStdlibApi::class)
fun testA1() {
    val pwd = "846542478965asdasf o[j$#@$2"
    val salt = Crypto.generateRandomSalt().toHexString()
    val hash = Crypto.generateHash(pwd, salt)
    Crypto.checkPassword(inputPassword = pwd, hash, salt).shouldBeTrue()
}

@OptIn(ExperimentalStdlibApi::class)
fun testA2() {
    val pwd = "846542478965asdasf o[j$#@$2"
    val salt = Crypto.generateRandomSalt().toHexString()
    val hash = Crypto.generateHash(pwd, Crypto.generateRandomSalt().toHexString())
    Crypto.checkPassword(inputPassword = pwd, hash, salt).shouldBeFalse()
}
