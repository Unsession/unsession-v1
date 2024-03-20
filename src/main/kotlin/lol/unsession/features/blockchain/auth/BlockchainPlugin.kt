@file:OptIn(ExperimentalSerializationApi::class)

package lol.unsession.features.blockchain.auth

import diglol.crypto.Ed25519
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import lol.unsession.application.logger
import lol.unsession.utils.getLogger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.*
import kotlin.system.exitProcess

val clientTonApi =
    HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            header("Content-Type", "application/json")
            url("https://tonapi.io/")
        }
    }

@OptIn(ExperimentalStdlibApi::class)
fun main() {

    var testCount = 0
    var allTests = 4

    val message = Message(
        address = "0:792340ce0c832fd4a2cae03704c98ec88acb43e356eadf2eabe76d6180de10d5",
        network = -239,
        timestamp = 1710910538,
        domain = Domain(21, "ton-connect.github.io"),
        signature = "RCLPr7fZSmduoT/iY8HjQrGKQhig4+AR2OZdWAA5FmNBEtSAG+7+dGiDbQ4HUM9BjcVwajMM5BM0+lCYuftwBQ==",
        payload = "01cdac5c08025a0d0000000065fa6d5db9e69dd577d7256b18d7f184c455ef0c",
        state_init = "te6cckECFgEAAwQAAgE0ARUBFP8A9KQT9LzyyAsCAgEgAxACAUgEBwLm0AHQ0wMhcbCSXwTgItdJwSCSXwTgAtMfIYIQcGx1Z70ighBkc3RyvbCSXwXgA/pAMCD6RAHIygfL/8nQ7UTQgQFA1yH0BDBcgQEI9ApvoTGzkl8H4AXTP8glghBwbHVnupI4MOMNA4IQZHN0crqSXwbjDQUGAHgB+gD0BDD4J28iMFAKoSG+8uBQghBwbHVngx6xcIAYUATLBSbPFlj6Ahn0AMtpF8sfUmDLPyDJgED7AAYAilAEgQEI9Fkw7UTQgQFA1yDIAc8W9ADJ7VQBcrCOI4IQZHN0coMesXCAGFAFywVQA88WI/oCE8tqyx/LP8mAQPsAkl8D4gIBIAgPAgEgCQ4CAVgKCwA9sp37UTQgQFA1yH0BDACyMoHy//J0AGBAQj0Cm+hMYAIBIAwNABmtznaiaEAga5Drhf/AABmvHfaiaEAQa5DrhY/AABG4yX7UTQ1wsfgAWb0kK29qJoQICga5D6AhhHDUCAhHpJN9KZEM5pA+n/mDeBKAG3gQFImHFZ8xhAT48oMI1xgg0x/TH9MfAvgju/Jk7UTQ0x/TH9P/9ATRUUO68qFRUbryogX5AVQQZPkQ8qP4ACSkyMsfUkDLH1Iwy/9SEPQAye1U+A8B0wchwACfbFGTINdKltMH1AL7AOgw4CHAAeMAIcAC4wABwAORMOMNA6TIyx8Syx/L/xESExQAbtIH+gDU1CL5AAXIygcVy//J0Hd0gBjIywXLAiLPFlAF+gIUy2sSzMzJc/sAyEAUgQEI9FHypwIAcIEBCNcY+gDTP8hUIEeBAQj0UfKnghBub3RlcHSAGMjLBcsCUAbPFlAE+gIUy2oSyx/LP8lz+wACAGyBAQjXGPoA0z8wUiSBAQj0WfKnghBkc3RycHSAGMjLBcsCUAXPFlAD+gITy2rLHxLLP8lz+wAACvQAye1UAFEAAAAAKamjF1h8bDJsGf10RENAGvUIaj22C5K8pX0N8alNAlvLJt6PQIMYJCU="
    )
    val expectedMessageBR = intArrayOf(226, 152, 114, 111, 74, 133, 82, 116, 7, 228, 165, 42, 7, 173, 30, 19, 204, 251, 60, 27, 111, 148, 145, 233, 116, 229, 51, 48, 88, 249, 67, 140).map { it.toByte() }.toByteArray()
    val expectedSignatureBR = intArrayOf(68, 34, 207, 175, 183, 217, 74, 103, 110, 161, 63, 226, 99, 193, 227, 66, 177, 138, 66, 24, 160, 227, 224, 17, 216, 230, 93, 88, 0, 57, 22, 99, 65, 18, 212, 128, 27, 238, 254, 116, 104, 131, 109, 14, 7, 80, 207, 65, 141, 197, 112, 106, 51, 12, 228, 19, 52, 250, 80, 152, 185, 251, 112, 5).map { it.toByte() }.toByteArray()
    val expectedPublicKeyBR = intArrayOf(88, 124, 108, 50, 108, 25, 253, 116, 68, 67, 64, 26, 245, 8, 106, 61, 182, 11, 146, 188, 165, 125, 13, 241, 169, 77, 2, 91, 203, 38, 222, 143).map { it.toByte() }.toByteArray()
    runBlocking {
        println("--- begin tests ---")
        val pMessage = message.parse()

        println("(pk test)")

        val pubKey = clientTonApi.get {
            url("v2/accounts/0:792340ce0c832fd4a2cae03704c98ec88acb43e356eadf2eabe76d6180de10d5/publickey")
        }.body<PublicKeyResponse>().public_key
        val decoded = pubKey.hexToByteArray()
        if (expectedPublicKeyBR.contentEquals(decoded)) {
            println("test0 passed")
            testCount++
        } else {
            println("test0 failed")
        }

        println("(createMessage test)")
        val result = createMessage(pMessage) // failed
        if (expectedMessageBR.contentEquals(result)) {
            println("test1 passed")
            testCount++
        } else {
            println("test1 failed")
        }

        println("(convertAndVerify test)")
        val result2 = convertAndVerify(message, decoded)
        if (result2) {
            println("test2 passed")
            testCount++
        } else {
            println("test2 failed")
        }

        println("(signature test)")
        if (pMessage.signature.contentEquals(expectedSignatureBR)) {
            println("test2.1 passed")
            testCount++
        } else {
            println("test2.1 failed")
        }
    }

    println("tests finished")
    println("tests passed: $testCount/$allTests")

    println("--- end tests ---")
    exitProcess(0)
}

@Serializable
data class Message(
    val address: String,
    val domain: Domain,
    val network: Int,
    val payload: String,
    val signature: String,
    val state_init: String,
    val timestamp: Long
) {
    fun parse(): ParsedMessage {
        println()
        return ParsedMessage(
            network,
            hexStringToByteArray(address), // fixme: incorrect!
            timestamp,
            domain,
            Base64.getDecoder().decode(signature),
            payload,
            state_init
        )
    }

    companion object {
        fun hexStringToByteArray(s: String): ByteArray {
            return hex(s.substringAfter(':'))
        }
    }
}

@Serializable
data class ParsedMessage(
    val workchain: Int, // -239
    val address: ByteArray, // 0323213
    val timestamp: Long, // unix
    val domain: Domain, // Domain class
    val signature: ByteArray, // base64
    val payload: String, // proof
    val stateInit: String // stateinit
)

@Serializable
data class Domain(val lengthBytes: Int, val value: String)

fun createMessage(parsedMessage: ParsedMessage): ByteArray {
    val tonProofPrefix = "ton-proof-item-v2/".toByteArray()
    val tonConnectPrefix = "ton-connect".toByteArray()

    val workchainBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0).array()
    val timestampBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(parsedMessage.timestamp).array()
    val domainLengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(parsedMessage.domain.lengthBytes).array()

    val message = tonProofPrefix +
            workchainBytes +
            parsedMessage.address +
            domainLengthBytes +
            parsedMessage.domain.value.toByteArray() +
            timestampBytes +
            parsedMessage.payload.toByteArray()

    val messageHash = MessageDigest.getInstance("SHA-256").digest(message)
    val fullMessage = byteArrayOf(0xff.toByte(), 0xff.toByte()) + tonConnectPrefix + messageHash
    return MessageDigest.getInstance("SHA-256").digest(fullMessage)
}

@Serializable
data class DataRequest(val payload: String)

fun convertAndVerify(parsedMessage: Message, pubKey: ByteArray): Boolean {
    val message = createMessage(parsedMessage.parse())
    logger.debug("s: ${parsedMessage.parse().signature.size} (${parsedMessage.parse().signature}), m: ${message.size}, p: ${pubKey.size}")
    try {
        return runBlocking { Ed25519.verify(parsedMessage.parse().signature, pubKey, message) }
//        return Ed25519.verify(parsedMessage.parse().signature, message, pubKey)
    } catch (e: Exception) {
        logger.error("Error: ${e.printStackTrace()}")
        return false
    }
}

fun Application.configureBlockchain() {
    val logger = getLogger("Blockchain")
    routing {
        route("/ton") {
            get("/sign") {
                logger.info("sign call succeed")
                call.respond(DataRequest("proof"))
            }

            post("/auth") {
                logger.info("auth call-id: ${call.callId}")
                val parsedMessage = call.receive<Message>()
                val pubKey = clientTonApi.get {
                    url("v2/accounts/${parsedMessage.address}/publickey")
                }.body<PublicKeyResponse>().public_key // then covert pubKey (hex) to ByteArray
                logger.debug("auth call public key: $pubKey")
                val decoded = pubKey.chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray()
                logger.info("auth call public key: $decoded")
                val result = convertAndVerify(parsedMessage, decoded)
                logger.info("auth call response/result: $result")
                call.respond(DataResponse(result.toString()))
            }
        }
    }
}
@Serializable
data class DataResponse(val token: String)
@Serializable
data class PublicKeyResponse (val public_key: String)