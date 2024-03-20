@file:OptIn(ExperimentalSerializationApi::class)

package lol.unsession.features.blockchain.auth

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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import lol.unsession.application.logger
import lol.unsession.features.blockchain.auth.Message.Companion.hexToByteArray2
import lol.unsession.utils.getLogger
import org.ton.crypto.Ed25519
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.*

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
            address,
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
        fun String.hexToByteArray2(): ByteArray {
            return chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }

    }
}

@Serializable
data class ParsedMessage(
    val workchain: Int, // -239
    val address: String, // 0323213
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

    val workchainBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(parsedMessage.workchain).array()
    val domainLengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(parsedMessage.domain.lengthBytes).array()
    val timestampBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(parsedMessage.timestamp).array()

    val message = tonProofPrefix +
            workchainBytes +
            parsedMessage.address.substringAfter(":").hexToByteArray2() +
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
        return Ed25519.verify(parsedMessage.parse().signature, message, pubKey)
    } catch (e: Exception) {
        logger.error("Error: ${e.printStackTrace()}")
        return false
    }
}

fun Application.configureBlockchain() {
    val logger = getLogger("Blockchain")
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
    routing {
        route("/ton") {
            get("/sign") {
                logger.info("sign call succeed")
                call.respond(DataRequest("proof"))
            }

            post("/auth") {
                logger.info("auth call-id: ${call.callId}")
                val parsedMessage = call.receive<Message>()
                @Serializable
                data class PublicKeyResponse (val public_key: String)

                val pubKey = clientTonApi.get {
                    url("v2/accounts/${parsedMessage.address}/publickey")
                }.body<PublicKeyResponse>().public_key // then covert pubKey (hex) to ByteArray
                logger.debug("auth call public key: $pubKey")
                val decoded = pubKey.toByteArray()
                logger.info("auth call public key: $decoded")
                val result = convertAndVerify(parsedMessage, decoded)
                @Serializable
                data class DataResponse(val token: String)
                logger.info("auth call response/result: $result")
                call.respond(DataResponse(result.toString()))
            }
        }
    }
}
